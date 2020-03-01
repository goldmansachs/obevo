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
# Developer Guide
<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

## Quick Code Walkthrough + Key Classes:

First, read through these:

1. [Design Walkthrough](design-walkthrough.html) to understand what the tool is trying to do and how
2. [Java API](java-api.html) to see the module structure and understand the tool from a client perspective

Pay special attention to the "Developer Guide note" mentions in the Design Walkthrough to see how the design maps to the code.

## Getting started with coding
Try out any of the unit tests and ITs in the DBMS-specific modules or the scenario test modules.

These will typically invoke deployments against actual DBs and vet that the results in the DB are correct.

You can use these to see the full code path executed and trace through as you need to.


## Setup a test database for development

Obevo defines a few common integation tests that DBMS implementations should use:

* AbstractDbMetadataManagerIT and subclasses: Tests the DB Metadata API and retrieval
* SameSchemaChangeAuditDaoTest and subclasses: Tests the execution and upgrades of the Change Audit tables
* \*Deploy\*IT: Tests end-to-end deployments

These tests require live DBs to execute against. In case you don't already have databases to execute against and would
like to develop, we have guidance on how to do so, whether via Amazon RDS or via installing express editions of the DBMS
software.

See the table below for how to setup these dates.

|Vendor|Test Locale|
|------|-----------|
|Sybase/SAP ASE|[ASE Express Setup](dev-setup-sybase-ase.html)|
|DB2|[Docker setup for DB2 Express](dev-setup-docker.html)|
|MS SQL Server|[Docker setup for SQL Server](dev-setup-docker.html) or [Amazon RDS](dev-setup-amazon.html)|
|Oracle|[Amazon RDS](dev-setup-amazon.html)|
|PostgreSQL|[Docker setup for PostgreSQL](dev-setup-docker.html) or [Amazon RDS](dev-setup-amazon.html)|
|Sybase/SAP IQ|No instructions available yet|
|H2|No setup needed (in-memory DB)|
|HSQLDB|No setup needed (in-memory DB)|

## Adding a new DBMS implementation

#### 1. Create a new module obevo-db-&lt;dbmsName&gt;

You can use an existing DB implementation module as reference.

See the [Platform Listing](platform-listing.html) for the full list of modules.

We recommend obevo-db-postgresql as a default choice if you need a suggestion.

Rename only the package and the poms for now, i.e. don't yet rename the classes; we will get to those later

Also add your project as a dependency to the obevo-db-client-all-dbs module.

Add entry to default.yaml in com.gs.obevo.db.confg in obevo-db

Rename platform name accordingly in <YourPlatform>DbPlatform

#### 2. Setup a test database

Option 1 (preferred): Docker
search dockerhub for examples; you have plenty here

1) add setup-docker-mongodb.sh file. ensure that your db can start

2) add build profile to the pom


Option 2: Amazon RDS
See CreateDbInstance for using an API to create your database
The only database currently using this is the Redshift platform (currently housed in postgres)
Your own method:




#### 3.  determine where your JDBC driver comes from and add it to the project
- preferably from Maven central. If so, then check the license:
  - if compatible w/ Apache license (e.g. Apache, MIT, BSD, EPL), then add it
  - if not compatible (e.g. GPL, LGPL), then make it optional as we don't want this included by default
- sometimes needs a special Maven repo. see the oracle example for that (TBD)
- sometimes is only within the docker container itself. see db2 example for that



#### 4. Create and setup a blank DB schema to run your tests against

Obevo will run tests against actual DBMSs that have schemas dedicated to Obevo.
Get a schema to facilitate your testing

As a convention, name your schemas as dbdeploy01, dbdeploy02, dbdeploy03.
* dbdeploy01 and dbdeploy02 are typically for testing your deployments
* dbdeploy03 is for the metadata API tests

#### 5. Setup your initial test files

First, update the docker-*-creds.yaml file accordingly to the DB you just setup

Next, under platforms.<yourplatform>.example1.step1, let's start simple with just the table (rename the other folders
for now)

#### 6. Implement the classes required to do a basic incremental deployment for simple objects like tables

First, remove / comment the optional classes:
* anything under change types
* Lock classes
* TranslationDialect classes

The main classes you will need to implement:
* <PlatformName>DbPlatform
* <PlatformName>AppContext
* <PlatformName>EnvironmentInfraSetup
   * add code for programmatically creating schemas/groups/users, etc
* <PlatformName>JdbcDataSourceFactory
   * add the semantics for generating jdbc urls
* <PlatformName>SqlExecutor
   * here you will add the commands to switch between schemas

Also implement a DbMetadataDialect for your new implementation in obevo-db-metadata-manager-impl

Start w/ tables for now; we will get to stored procedures/views/etc. later

#### 7. Get the integration test running for the simple example

Follow the examples, e.g. PostgreSqlDeployerIT
Most examples will have a "step1" and "step2" - the idea is to do the initial deployment and then
a subsequent update so that you can vet both cases.

#### 8. Add support for the other DB Object types

e.g. stored procedures, views, static data etc.

Also add the implementation for the DB Metadata Manager and the test

#### 9. Add the -cleanFirst command to your test to vet out the cleaning command

Note that some of the DBMS types require specific sqls to query out the db objects that schemacrawler cannot get to.
This would be declared in the *AppContext class if applicable.

#### 10. Add reverse-engineering support

1. Run the steps in the user guide for reverse-engineering from Aqua
2. Try the AquaRevengTest for your module to ensure that the code can get formatted into the right structure
3. Modify the code as needed to get it to work (most of the work is just in parsing the text for the object names, and
at this point, hopefully should not require much changes at this point for new DBMS's)
