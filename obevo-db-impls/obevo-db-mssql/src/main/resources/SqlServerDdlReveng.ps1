<#
Copyright 2017 Goldman Sachs.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
#>

<#
Some code used from: https://blogs.technet.microsoft.com/heyscriptingguy/2010/11/04/use-powershell-to-script-sql-database-objects/
#>
function global:SqlServerDdlReveng([string]$SavePath, [string]$server, [string]$dbname, [string]$username, [string]$password) {

    [System.Reflection.Assembly]::LoadWithPartialName("Microsoft.SqlServer.SMO") | out-null

    $serverConnection = New-Object ('Microsoft.SqlServer.Management.Common.ServerConnection') -argumentlist $server, $username, $password

    $SMOserver = New-Object ('Microsoft.SqlServer.Management.Smo.Server') -argumentlist $serverConnection

    if ($SMOserver.ServerType-eq $null) {
       Write-Error "Could not login to sever '$ServerName'"
       return
    } 
    
    $db = $SMOserver.databases[$dbname]


    $Objects = $db.Tables
    $Objects += $db.Views
    $Objects += $db.StoredProcedures
    $Objects += $db.UserDefinedFunctions
    $Objects += $db.Defaults
    $Objects += $db.ExtendedStoredProcedures
    $Objects += $db.Rules
    $Objects += $db.SymmetricKeys
    $Objects += $db.Synonyms
    $Objects += $db.Triggers
    $Objects += $db.UserDefinedDataTypes
    $Objects += $db.UserDefinedAggregates
    $Objects += $db.UserDefinedTableTypes
    $Objects += $db.UserDefinedTypes

    new-item -type directory -path "$SavePath"

    foreach ($ScriptThis in $Objects | where {!($_.IsSystemObject)}) {
        #Need to Add Some mkDirs for the different $Fldr=$ScriptThis.GetType().Name

        $scriptr = new-object ('Microsoft.SqlServer.Management.Smo.Scripter') ($SMOserver)

        # This is of type Microsoft.SqlServer.Management.Smo.ScriptingOptions
        $scriptr.Options.AllowSystemObjects = $False
        $scriptr.Options.AppendToFile = $False
        $scriptr.Options.AnsiFile = $False
        $scriptr.Options.Bindings = $True
        $scriptr.Options.ClusteredIndexes = $True
        $scriptr.Options.ConvertUserDefinedDataTypesToBaseType = $False

        $scriptr.Options.DriAll = $True

        $scriptr.Options.ScriptDrops = $False

        $scriptr.Options.IncludeHeaders = $True

        $scriptr.Options.ToFileOnly = $True

        $scriptr.Options.Indexes = $True
        $scriptr.Options.Triggers = $True

        $scriptr.Options.Permissions = $False

        $scriptr.Options.WithDependencies = $False
        $scriptr.Options.SchemaQualify = $True
        <#
        SchemaQualifyForeignKeysReferences
        #>

        $TypeFolder=$ScriptThis.GetType().Name

        if ((Test-Path -Path "$SavePath\$TypeFolder") -eq "true")
        {"Scripting Out $TypeFolder $ScriptThis"}
        else {new-item -type directory -name "$TypeFolder"-path "$SavePath"}

        $ScriptFile = $ScriptThis -replace "\[|\]"

        $enc = [system.Text.Encoding]::UTF8
        $scriptr.Options.FileName = "$SavePath\$TypeFolder\$ScriptFile.sql"
        $scriptr.Options.Encoding = $enc



        #This is where each object actually gets scripted one at a time.

        $scriptr.Script($ScriptThis)
    }
}
