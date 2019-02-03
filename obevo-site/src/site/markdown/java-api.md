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

# Java API

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

## Module Overview

Before diving into the Java code, first let's describe the modules in
Obevo.

First, describe the high-level module groupings (the first-level of
directories in the Obevo codebase):

|Module Group|Description|
|------------|-----------|
|obevo-core|The core Obevo algorithm framework, as described in the [Design Walkthrough](design-walkthrough.html)|
|obevo-db-impls|DBMS-platform implementations of Obevo|
|obevo-utils|Java client utilities for DBMS-platforms that sit on top of the platform modules|
|obevo-dists|API endpoints for non-Java clients to access Obevo from their builds|

And now the modules themselves (the first-level of directories in the
Obevo codebase):

|Module                                                          |Module Group    |Description|
|----------------------------------------------------------------|----------------|--------------------------------------------------------------------------------------------------------------|
|obevo-core                                                      |core            |Core module for implementing the Obevo incremental-deployment algorithm|
|obevo-db                                                        |db-impls        |The base implementation of the core DA algorithm for DBMS platforms|
|obevo-db-\* &lt;implType&gt;                                    |db-impls        |The DBMS-specific implementations (full list [here](platform-listing.html)). These implement a few interfaces defined in obevo-db|
|obevo-dbmetadata-impl                                           |db-impls        |Wrapper code around the schemacrawler library (i.e. handling a couple tweaks around how to call the library)|
|obevo-internal-comparer                                         |db-impls        |Copy of the CATO library; hoping to retire this once the main CATO library takes in some changes|
|obevo-db-client-alldbs                                          |db-impls        |maven dependency grouping as a convenience to retrieve all DBMS implementations|
|obevo-db-scenario-tests                                         |db-impls        |Scenario tests (not for client usage)|
|obevo-db-unittest-util                                          |obevo-db-utils  |Utilities to facilitate creating unit test dbs and loading data via DB Unit|
|obevo-reladomo-util                                             |obevo-db-utils  |Utility to generate DDLs from Reladomo|
|obevo-dist                                                      |obevo-dists     |binary assembly distribution|
|obevo-maven-plugin                                              |obevo-dists     |Maven plugin|
|obevo-maven-cbk-archetype                                       |obevo-dists     |Maven archetype to create Obevo projects.|
|obevo-site                                                      |Documentation   |The documentation bundle|


## Modules relevant for Java API Users

As an API client, you'll only need to depend on the following:

-   The core module for the platform type you are interested in (e.g.
    obevo-db for DBMS platforms)
-   The specific platform module that you are working on, e.g. one of
    the obevo-db \*impl\* modules or obevo-db-client-alldbs to access
    all impls
-   Optionally, any of the Java utility modules, e.g. in the
    obevo-db-utils module group
-   You should not access the other modules via Java


## Java Package Overview

com.gs.obevo is the main package under which all is kept - this is in
the obevo-core module that has the code focusing on the change
calculation algorithm.

Under that, you will have the following core packages

| Sub-package                       | Description                       |
|-----------------------------------|-----------------------------------|
| api                               | The public API entrypoint for executing deployments; good place to start to explore the API. Typically has the following subpackages:<br/>- factory: Convenience factory classes to access the main Obevo classes<br/>- appdata: The domain model classes corresponding to the code and data that clients would provide<br/>- platform: The core classes/interfaces that will operate on the user data to carry out deployments.|
| impl                              | The implementations of the API. Clients should avoid accessing these classes as these are meant to be internal|
| apps                              | Obevo utilities built off the deploy api besides the basic deploy. Should only rely on the api/model packages, and not impl|

And you will also have the com.gs.obevo.db subpackage for the database
platform, which will have the same pattern of child packages, i.e. api,
impl, apps


## Key classes

|Class                                             |Description|
|--------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|com.gs.obevo.db.api.factory.DbEnvironmentFactory  |The main entrypoint; use this to access the systems that you've configured in your system configs and such.|
|com.gs.obevo.db.api.appdata.DbEnvironment         |The model class representing an environment to connect to. DbEnvironmentFactory would return this|
|com.gs.obevo.db.api.DbDeployerAppContext          |Context class to use to invoke deployments and that has references to the components that the Deployer uses and that you can access. e.g. methods for reading changes from source or the audit table|


## Retrieving dependencies from your artifact repository

Define the dependencies in your project prior to using them in your
project

Maven Example 1 - Conventional way

```
<dependency>
    <groupId>com.goldmansachs.obevo</groupId>
    <artifactId>obevo-db</artifactId>
    <version>${obevo.version}</version>
</dependency>
<dependency>
    <groupId>com.goldmansachs.obevo</groupId>
    <artifactId>obevo-db-hsql</artifactId>  <!-- replace db-hsql w/ whatever platform you are trying to deploy to -->
    <version>${obevo.version}</version>
</dependency>
```

Maven Example 2 - Use the Obevo BOM to manage your dependency versions
in one place (see the [Maven doc on BOMs](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
for more information)


```
<!-- First define the BOM in your dependencyManagement section -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.goldmansachs.obevo</groupId>
            <artifactId>obevo-bom</artifactId>
            <type>pom</type>
            <scope>import</scope>
            <version>${obevo.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Then define your dependencies in the modules that need them; no need to specify the version numbers here -->
<dependencies>
    <dependency>
        <groupId>com.goldmansachs.obevo</groupId>
        <artifactId>obevo-db</artifactId>
    </dependency>
    <dependency>
        <groupId>com.goldmansachs.obevo</groupId>
        <artifactId>obevo-db-hsql</artifactId>  <!-- replace db-hsql w/ whatever platform you are trying to deploy to -->
    </dependency>
</dependencies>
```


## Example API Usage - com.gs.obevo.api.factory.Obevo

com.gs.obevo.api.factory.Obevo is the main API entrypoint to read in
your project configurations from the config file. Explore the API and
the overloads for each method to see what other method options are
available.

```
// Read the environment from your file system
DbEnvironment env = Obevo.readEnvironment("./src/test/resources/platforms/hsql");

// Build the app context - you can pass in credentials via the API if needed
DeployerAppContext context = Obevo.buildContext(env, "sa", "password");

// Then invoke the deploy commands.
context.cleanEnvironment();
context.deploy();
```


## Example API Usage - Direct Creation of DbEnvironment

Or if you don't want to use the configuration file, you can create the
DbEnvironment object directly, and then create the DbDeployerAppContext
directly from there:

```
DbEnvironment dbEnv = new DbEnvironment();
dbEnv.setSourceDirs(Lists.immutable.with(FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/platforms/h2/step1")));
dbEnv.setName("test");
dbEnv.setPlatform(new H2DbPlatform());
dbEnv.setSchemas(Sets.immutable.with(new Schema("SCHEMA1"), new Schema("SCHEMA2")));
dbEnv.setDbServer("BLAH");

dbEnv.setSchemaNameOverrides(Maps.immutable.of("SCHEMA1", "bogusSchema"));
dbEnv.setNullToken("(null)");
dbEnv.setDataDelimiter('^');


DeployerAppContext context = Obevo.buildContext(dbEnv, new Credential("sa", ""));

context.setupEnvInfra();
context.cleanEnvironment();
context.deploy();
```


## Configuring Custom Logging for Obevo in Java

Obevo uses the [SLF4J API](https://www.slf4j.org), and
the main jars do not include any logging implementations as third-party
dependencies.

Hence, you can configure your logging as you wish.

Note: the only exception is the obevo-cli module, which is the
command-line API and will have a logging implementation included.
However, this dependency is not appropriate for Java library usage.
