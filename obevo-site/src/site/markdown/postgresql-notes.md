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
# PostgreSQL Usage Notes

DB Type acronym for configurations: POSTGRESQL


## Packages and Package Bodies

Obevo supports defining packages and package bodies within a single file and allowing the files to be edited in place.
For more information, see [DB Project Structure](db-project-structure.html) and the section "Objects with BODY components".


## PostgreSQL Directories

Obevo supports the [EXTENSION](https://www.postgresql.org/docs/current/static/sql-createextension.html) object type
as part of the environment setup step.

This will create the extension object if it doesn't already exist. If it already exists, it will leave it alone and not
modify it.

Note - this only creates the extension via SQL by simply executing the SQL referenced in the [doc](https://www.postgresql.org/docs/current/static/sql-createextension.html); no other backend setup is done as part of this.

To leverage this feature:

1) Define it in your system-config.xml file with the _extensions_ element:

```
<dbSystemConfig type="POSTGRESQL">
    <schemas>
        <schema name="SCHEMA1" />
    </schemas>

    <!-- You can define it for all environments by specifying this outside of the dbEnvironment element ... -->
    <extensions>
        <extension name="uuid-ossp" />
        <extension name="some-other-ext" />
    </extensions>

    <environments>
        <dbEnvironment name="test" ...>
            <!-- ... or define it for a single environment (overriding the common definition) within the dbEnvironment element -->
            ...
            <extensions>
                <extension name="yet-another-ext" />
                <extension name="uuid-ossp" />
            </extensions>
            ...
        </dbEnvironment>
    </environments>
</dbSystemConfig>
```

2) Force the environment setup, via two options:

1. Use the -forceEnvSetup parameter at the command line.
2. Specify the property forceEnvSetup="true" in your dbEnvironment to have this be a default setting, and then execute a deploy.
