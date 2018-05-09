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

# In-memory DBs for unit testing

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

Obevo can create an in-memory db from your existing SQLs, thus
guaranteeing that the same DDLs you use to deploy to prod are the same
ones for your testing

Some translation happens to ensure that your prod SQLs work against an
in-mem environment (notably to simplify some SQLs, as we don't need to
support as much rich logic in an in-mem environment)

-   Stored Procedures, Functions, and Triggers are not deployed (very
    hard to unit-test these, or the SQL varies too much between the live
    dbs and the in-memory dbs)
-   See the other translations done in the class InMemoryTranslator.
    These include:
    -   Removing "lock data rows" and such from Sybase
    -   Removing mentions of "clustered" for indices

## Setting up the in-memory database

You have two options:


### Option 1 - use the inMemoryDbType variable in your system-config.xml

In your system-config.xml, you define the type for your system via this attribute:
```dbSystemConfig type="SYBASE_ASE"```.

At the dbEnvironment level, you can then specify the inMemoryDbType to
facilitate the conversion from the system type to the in-memory
environment type, per the example below.

You can then use the standard DbEnvironmentFactory API to access the
environment and build the DbDeployerAppContext.

**HOWEVER**, if you are re-creating the DB across tests, please reuse
the DbDeployerAppContext instance as to save time in reloading your db
files from disk.

```
<dbEnvironment name="BOSI_A_1"
            cleanBuildAllowed="true"
            jdbcUrl="jdbc:hsqldb:mem:unit1inmem" inMemoryDbType="HSQL"
            defaultUserId="sa"
            defaultPassword=""
            >
```


### Option 2 - Use the UnitTestDbBuilder to convert the environment in memory

There is a wrapper API also available to build the in-memory database by
using a reference env from your environments and optionally overriding
some values.

This option **_will cache the reads from file disk_** , so no need for
you to reuse the DbDeployerAppContext from the wrapper API

You can just include this in maven to get access to the utility:

```
<properties>
    <obevo.version>${project.version}</obevo.version>
</properties>
...
<dependencies>
    <dependency>
        <groupId>com.goldmansachs.obevo</groupId>
        <artifactId>obevo-db-unittest</artifactId>
        <version>${obevo.version}</version>
    </dependency>
    <dependency>
        <groupId>com.goldmansachs.obevo</groupId>
        <artifactId>obevo-db-hsql</artifactId>  <!-- or obevo-db-h2 -->
        <version>${obevo.version}</version>
    </dependency>
</dependencies>
```

And here is some sample code to build the DB:

```
DbDeployerAppContext context = UnitTestDbBuilder.newBuilder()
    .setSourcePath("platforms/db2/step1")
    .setReferenceEnvName("unittestrefh2")  // mention the environment name that you want to model off
    .setEnvName("myUnitTestDb")  // optionally - rename the environment if you are changing it from the reference
    .setDbPlatform(new H2DbPlatform())  // you can override the platform in code, or do it in the XML as mentioned in option 1
    .setDbServer("mydb2testH2")  // setting this value is a shortcut to generate the JDBC url for this environment for you if not already specified
    .buildContext();

// Once you have the reference to the DbDeployerAppContext, the code remains the same.
context.setupEnvInfra();
context.cleanEnvironment();
context.deploy();

System.out.println("This is my JDBC url for reference - " + context.getEnvironment().getJdbcUrl());
```


## Manually defining a translation SQL

The translation logic works for most cases, but at times it will not.

To define your own SQL, leverage the include/exclude platforms
functionality to define the SQL for your regular environments separately
from the in-memory environments, e.g.

```
//// CHANGE name="myChange" excludePlatforms="HSQL,H2"
My DB2-compatible SQL

//// CHANGE name="myChange" includePlatforms="HSQL,H2"
My in-memory-compatible SQL
```
