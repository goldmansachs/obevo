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
# Oracle Usage Notes

DB Type acronym for configurations: ORACLE


## Packages and Package Bodies

Obevo supports defining packages and package bodies within a single file and allowing the files to be edited in place.
For more information, see [DB Project Structure](db-project-structure.html) and the section "Objects with BODY components".


## Oracle Directories

Obevo supports the [DIRECTORY](https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_5007.htm) object type
as part of the environment setup step.

This will create the directory object if it doesn't already exist. If it already exists, it will leave it alone and not
modify it. (We may modify this behavior depending on user feedback; please let us know your thoughts)

To leverage this feature:

1) Define it in your system-config.xml file with the _serverDirectory_ element:

```
<dbSystemConfig type="ORACLE">
    <schemas>
        <schema name="SCHEMA1" />
    </schemas>

    <!-- You can define it for all environments by specifying this outside of the dbEnvironment element ... -->
    <serverDirectories>
        <serverDirectory name="dir1" directoryPath="path1" />
        <serverDirectory name="dir2" directoryPath="path2" />
    </serverDirectories>

    <environments>
        <dbEnvironment name="test" ...>
            <!-- ... or define it for a single environment (overriding the common definition) within the dbEnvironment element -->
            ...
            <serverDirectories>
                <serverDirectory name="dir1" directoryPath="path1_prime" />
                <serverDirectory name="dir2" directoryPath="path2_prime" />
            </serverDirectories>
            ...
        </dbEnvironment>
    </environments>
</dbSystemConfig>
```

2) Force the environment setup, via two options:

1. Use the -forceEnvSetup parameter at the command line.
2. Specify the property forceEnvSetup="true" in your dbEnvironment to have this be a default setting, and then execute a deploy.


## Statement Terminator

In Oracle, statements can be terminated using semicolon ''';''' and slash '''/'''.

However, within Obevo these cannot be used to separate multiple lines within a single //// CHANGE block or rerunnable file.
The reason is that the JDBC APIs are typically only allowed to execute a single SQL command within a JDBC statement (see
[link](https://stackoverflow.com/questions/18941539/is-the-semicolon-necessary-in-sql) for reference. Though other DBMS
platforms and drivers could be more lenient for this, Oracle is not.

Hence, we typically recommend using the default GO splitter to split multiple statements within a block if needed.

Note that you can still use a single semicolon or slash to end a statement, even if followed by a GO. (Most notably,
to end a PL/SQL begin/end block with a slash, since the block may contain multiple semicolons).

We have no plans to support this within JDBC itself, given the non-trivial nature of building our own parser of Oracle code.
Having said that, it may be an option in the future to have SQL executions done via SQL*Plus command line, though not in
the near future.



## TODO JDBC Driver Setup
new stuff
https://blogs.oracle.com/dev2dev/get-oracle-jdbc-drivers-from-the-oracle-maven-repo-netbeans-eclipse-intellij
