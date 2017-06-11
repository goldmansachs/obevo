<!--

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

-->
# Reverse-Engineering via DBMS-Provided Tools
Obevo will integrate w/ DBMS-provided tooling for reverse-engineering. We prefer this route over
using external tools as the DBMS vendors are more likely to keep up-to-date on their own DDL generation tooling.

Obevo will either A) call the vendor APIs directly or B) provide you the
command/s to run to generate some interim artifacts for Obevo to then complete processing.

The following tools are currently supported.

| DBMS | Tooling Leveraged by Obevo |
|------|-----------|
|Sybase ASE|[ddlgen](http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc30191.1570100/doc/html/san1367605037678.html)|
|DB2|[DB2LOOK](http://www.ibm.com/support/knowledgecenter/SSEPGG_11.1.0/com.ibm.db2.luw.admin.cmd.doc/doc/r0002051.html)|
|Postgres|[pg_dump](https://www.postgresql.org/docs/9.6/static/app-pgdump.html)|
|SQL Server|[Microsoft.SqlServer.Management.Smo.Scripter class in Powershell](https://msdn.microsoft.com/en-us/library/microsoft.sqlserver.management.smo.scripter.aspx)|
|Oracle|[DBMS_METADATA API via JDBC](https://docs.oracle.com/database/121/ARPLS/d_metada.htm#ARPLS026)|

The tooling generally works as follows:
1. Execute the Obevo command to generate the DBMS-specific commands to reverse-engineer the DDLs to a particular format
2. Execute those DBMS-specific commands to generate the DDL output file
3. Re-execute the Obevo command w/ the DDL output file as an additional argument

# Execution Steps
Step 1: execute the NEWREVENG command with your arguments

```
%OBEVO_HOME%\bin\deploy.bat NEWREVENG -dbType DB2 -dbSchema YourSchemaName -mode schema -outputPath h:\reveng-example-output -dbHost yourhost.me.com -dbPort 1234 -dbServer MYDB01

Detailing the arguments:
 -dbType: required, specify SYBASE_ASE or DB2
 -dbSchema: required, the schema/database you are querying from
 -mode: required, use the "schema" value
 -outputPath: required, specify where your reverse-engineered output should go
 Connection arguments - specify either -dbHost and -dbPort for Sybase or -dbHost and -dbPort and -dbServer for DB2
```

Step 2: execute the commands that are prompted for you in the instructions from the Step 1 output.

Step 3: execute your step 1 command again, but add the -inputPath &lt;yourFilePath&gt; argument to do the conversion to the obevo format

Regarding the output:
* This will generate the reverse-engineered output under &lt;outputDir&gt;/final

Once you have these files, do the final touches on them as you see fit (e.g. delete junk tables), and proceed to the next step
* <font color="Red">Note the warning in your output - if you see any directories with a name containing
    "-pleaseAnalyze" in the result of the DBREVENG script</font>, then the tool could not figure out what to do w/ those
    sql snippets. Either manually figure out where to put them and do so, or if you find too many
    such cases, reach out to the product team via Github, including a zip file of your reveng contents

Note that we explicitly don't include the grants here. This is because you can (and should) use the global permissioning functionality instead.

Once done, return to the [Existing Onboarding Guide](existing-onboarding-guide.html) to continue the onboarding process.
