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
|DB2|[DB2 Express Setup](dev-setup-db2.html)|
|MS SQL Server|[Amazon RDS Setup](dev-setup-amazon.html)|
|Oracle|[Amazon RDS Setup](dev-setup-amazon.html)|
|PostgreSQL|[Amazon RDS Setup](dev-setup-amazon.html)|
|Sybase/SAP IQ|No instructions available yet|
|H2|No setup needed (in-memory DB)|
|HSQLDB|No setup needed (in-memory DB)|

## Adding a new DBMS implementation

#### 1. Create and setup a blank DB schema to run your tests against

Obevo will run tests against actual DBMSs that have schemas dedicated to Obevo.
Get a schema to facilitate your testing

#### 2. Create a new module obevo-db-&lt;dbmsName&gt;

You can use obevo-db-db2 or obevo-db-postgresql as examples

#### 3. Implement the classes required to do a basic incremental deployment for simple objects like tables

These classes revolve around setting up the JDBC parameters and such:

* AppContext, DbType, Dialect, DataSourceFactory, and SqlExecutor implementations (refer to the example for the exact names)

Also implement a DbMetadataDialect for your new implementation in obevo-schemacrawler
Start w/ tables for now; we will get to stored procedures/views/etc. later

#### 4. Implement an integration test in your module

Follow the examples, e.g. PostgreSqlDeployerIT
Most examples will have a "step1" and "step2" - the idea is to do the initial deployment and then
a subsequent update so that you can vet both cases.

#### 5. Add support for the other DB Object types

e.g. stored procedures, views, static data etc.

#### 6. Add the -cleanFirst command to your test to vet out the cleaning command

Note that some of the DBMS types require specific sqls to query out the db objects that schemacrawler cannot get to.
This would be declared in the *AppContext class if applicable.

#### 7. Add reverse-engineering support

1. Run the steps in the user guide for reverse-engineering from Aqua
2. Try the AquaRevengTest for your module to ensure that the code can get formatted into the right structure
3. Modify the code as needed to get it to work (most of the work is just in parsing the text for the object names, and
at this point, hopefully should not require much changes at this point for new DBMS's)
