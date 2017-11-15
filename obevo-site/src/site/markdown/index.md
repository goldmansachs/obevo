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
# Obevo - Evolve Your Database Objects

Deploying tables for a new application?

Or looking to improve the database deployment of a years-old system with hundreds (or thousands) of tables, views, stored procedures, and other objects?

Obevo has your database deployment use case covered.

[Quickstart Examples](https://github.com/goldmansachs/obevo-kata)


## Why Use Obevo?

### Organized maintenance of all your DB object files to handle all use cases

By allowing your DB files to be maintained per DB object (as you would with classes in application code), db file maintenance
becomes much easier compared to DB Deployment tools that require a new file or change definition per migration:

* Changes for a particular table can be reviewed in one place
* Stateless objects like stored procedures and views can be edited in place without specifying any new incremental change files
* All of this is possible without having to define the complete order of your file deployments; Obevo figures it out for you (a la a Java compiler compiling classes)

[Click here](design-walkthrough.html) for more information on how Obevo works and how its algorithm compares to what most other DB Deployment tools do

![1 file per object structure](images/db-kata-file-setup.jpg)


### In-memory and integration testing

How do you test your DDLs before deploying to production?

Obevo provides utilities and build plugins to clean and rebuild your databases so that you can integrate that step into
your integration testing lifecycle.

Obevo can take that a step further by converting your DB table and view code into an [in-memory database compatible format](in-memory-db-testing.html)
that you can use in your tests. The conversion is done at runtime, so you do not have to maintain separate DDLs
just for in-memory testing


### Easy onboarding of existing systems

Hesitant about getting your existing database schema under SDLC control due to how objects your application has built up
over the years? Obevo has been vetted against many such cases from applications in large enterprises.


### Versatile to run

Obevo can be invoked via:

* [Java API](java-api.html)
* [Command Line API](command-line-api.html)
* [Maven](maven-api.html)
* [Gradle](gradle-api.html)

Obevo is designed to allow your DB code to be packaged and deployed alongside your application binaries.

Treat your DB code like you would treat your application code!


### DBMS-specific features

Obevo currently supports DB2, Sybase ASE, Sybase IQ, PostgreSQL, MS SQL Server, Oracle, HSQLDB, and H2

It has special handling for DBMS, such as reorgs for DB2 and handling transaction log checks for Sybase ASE

## Document Conventions
This documentation assumes we are running this from a Windows desktop. Adjust the command line arguments accordingly for Linux if you choose.
