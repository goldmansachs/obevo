# Change Log

## 8.1.0

### Functionality Improvements

#161: Hash mismatch error message should show the version name of the object

Initial MemSQL support

### Technical Improvements

### Bug Fixes

#246: For Oracle Reverse Engineering crashes when schema Reverse engineering an Oracle database crashes on index partitions

#263: Correct and clarify the graph cycle error message

#264: NPE caused when package-info.txt file w/ METADATA section exists in staticdata (data) folder

Correcting error messages on graph cycles for complex databases

## 8.0.0
### Functionality Improvements
#111: Preventing concurrent deploys against a given schema

#47: MySQL Support added - only for table/view objects with reverse-engineering. Still needs public schema permissions, more object types supported, embedding generateSimpleParameterMetadata param in the connection within our code.

#247: MongoDB - adding reverse-engineering, decoupling from deprecated eval API

### Technical Improvements
Upgrading to JDK 8 bytecode

Upgrading to JGraphT 1.3.0 (first required JDK 8 dependency)

### Bug Fixes
Correcting error messages on graph cycles for complex databases


## 7.2.0

### Functionality Improvements
#239: MongoDB productionization: collection now treated as an incremental change type, reverse-engineering support, clean functionality built

#231 #233: Correct reverse-engineering of table indices to their correct tables

#232 #233: Support reverse-engineering of triggers

#231 #235: Removing redundant unique indexes for Oracle Primary Key reverse-engineering

#236: Support Character Set Encoding for Oracle Reverse Engineering

Allowing valid special characters (e.g. #) to be in object names, as certain RDBMS platforms also allow them

### Technical Improvements
DB2 build updated to use the new Docker Hub coordinates from IBM

#252: Optimize Images 610.60kb -> 448.30kb (26.58%) and fix typo

### Bug Fixes
#229: Oracle Reverse Engineering can still generate output even if a single object DDL cannot be generated.


## 7.1.0

### Functionality Improvements
#182: Adding Hibernate reverse-engineering API. See [ORM Integration docs](https://goldmansachs.github.io/obevo/orm-integration.html) for more details.

#221 #223 #225: Oracle reverse-engineering improvements - unicode characters, nested tables, types, comments

#228: PostgreSQL improvements for kata - reverse-engineering, in-memory databases

### Technical Improvements
#228: Upgrading checkstyle version to avoid security prompts at Github site

### Bug Fixes
#228: Fixed drop behavior of PostgreSQL for multiple views that depended on each other


## 7.0.2

### Functionality Improvements
#106: CSV data loads will ignore indices that have columns not in the CSV file. This will allow users to use the CSV functionality on tables with identity columns but with separate unique indices.

### Technical Improvements

### Bug Fixes
#212: Fixing inability to handle DB2 reorg exceptions during static data queries. Previously, reorg detection only worked on update statements

#210 #213: Oracle - ignoring DATABASE LINKs during reverse-engineering, instead of erroring out. Separate ticket #186 is there for DATABASE LINK and materialized view support


## 7.0.0 and 7.0.1 (same release; had to redo it due to Maven SDLC issues)

### Functionality Improvements
#199: Adding support for PostgreSQL roles and extensions in the environment setup step

#202: Add option to export graph representation to a file

#196: Adding UUID support for CSV data loads for PostgreSQL

Initial MySQL support (still in Alpha)

### Technical Improvements
Moving more of the code over to Kotlin

#153: Refactor of dependency implementation

#193: Docker onboarding for testing

### Bug Fixes
#198: Static data loads for tables with self-referencing foreign keys are now able to work.


## 6.6.1

### Functionality Improvements
#187: DB Merge Tool Output Improvement - easier to see differences in files

### Technical Improvements

### Bug Fixes
#188: Correcting the metadata retrieval for ASE and PostgreSQL

#184: Documentation cleanups


## 6.6.0

### Functionality Improvements
#142: Support Oracle DIRECTORY objects at setup time.

### Technical Improvements
#173: Support YAML/JSON configurations and move away from type safe config towards commons-config v2

#175: Removing retrolambda, moving back to Java 7, and initial support for Kotlin

#150: Documentation updates

### Bug Fixes
#125: Clarify error messages when reading merge config file (missing input dirs, forward-slashes specified)

#165: Supporting Unicode in regular table/db object files and avoiding "TokenMgrError: Lexical error at line ..." issues

#169: Fixing missing quotes in deploy.sh/bat files in obevo-cli; otherwise, spaces in JAVA_HOME or OBEVO_HOME were not supported.

#166: Clearer error message if an invalid platform type is specified in config


## 6.5.2

### Bug Fixes
Assorted fixes on the file reader context in DeployerAppContext

### Technical Improvements
Added testing instructions for DB2


## 6.5.1

### Bug Fixes
#158: Fixing performance issues for DB2 and Oracle on DB metadata access


## 6.5.0

### Functionality Improvements
#127: MongoDB Support (beta)

#134: Amazon Redshift Support

### Bug Fixes
#81: Fixing performance issue: unnecessary call to get column data types from SchemaCrawler

#115: Reladomo DDL reverse engineering - removing "drop if exists" and splitting indices by leveraging common reverse-engineering utility

#122: Re-enabling the DB Merge Tool (was failing due to some exceptions before)

#129: Changes whose upstream dependencies failed should not be executed

#145: DB2 Invalid Object detection should always leverage INVALIDOBJECTS and SYSTABLES instead of just one of them.

### Technical Improvements
#128: Refactorings to more easily support non-SQL implementations


## 6.4.0

### Functionality Improvements
#84: Supporting subschema support in SQL Server and Sybase ASE (i.e. database.schema.object convention)

#53: Allowing log level to be configured in command-line API

#98: Clearer error messages on exceptions

### Bug Fixes
#82: Clearer error messages on cyclic dependencies in user DDL files

#94: Errors on granting permissions will no longer leave deployments in an inconsistent state

#55: Adding clearer log messages on rollback

### Notable Dependency Upgrades
Schemacrawler: to version 14.16.04-java7 (and 14.16.03-java7 for Sybase IQ)

HSQLDB: from 2.3.2 to 2.3.4; needed to avoid log warnings on synonym lookups


## 6.3.0

### Functionality Improvements
#42: Oracle package and package body support

#74: Oracle synonym support


### Bug Fixes
#73: Allowing the encoding to be specified when reading files.

#72: Fixing JDBC URL creation for MS SQL Server

#77: Adding includeDependencies attribute to TableChangeParser

Fixed the Sequence retrieval in DBMetadataManager to use the SchemaCrawler implementation; along the way, fixed Oracle metadata tests

#45: Ensure that an undeployed file w/ DROP_TABLE is not redeployed

#4: If users specify "create or replace" in SQL, then that should not force it to be dropped on rerunnable objects

#76: Clearer error message if schema was not found

#80: Defaulting lenient environment setups to true


## 6.2.2

### Bug Fixes
#49: Continuing fixes on DB2 Reorg

Reverse engineering fixes - ensuring that MS SQL, Oracle, and PostgreSQL reverse-engineering can be invoked via command line


## 6.2.1

### Bug Fixes
#49: Fixing DB2 Reorg detection - certain reorg error messages (incl. on batch update) were not getting parsed correctly

#65: Added missing entries to the BOM

#67: Documentation cleanups


## 6.2.0
Primary goal of this release was to support the reverse engineering lesson in the Obevo kata.

### Functionality Improvements
#51: Reverse-engineering added for HSQLDB

### Bug Fixes
#50: Obevo BOM should not also expose third-party dependencies

### Technical Improvements
#57: Uploading test sources to Maven central


## 6.1.0

### Functionality Improvements
Reverse-engineering added for Oracle, Postgres, SQL Server

#37: Allowing objects to be excluded during reverse engineering


### Bug Fixes

SchemaCrawler logginq moved to error level to avoid excessive noise from unsupported DBMS functions


## 6.0.1

### Bug Fixes

###### A number of fixes around the CSV static data file parsing:

* Tolerates white space in the header columns
* Tolerates white space around null tokens
* Backslash \ character can now be loaded easily; does not need to be escaped a la Java strings

This now leverages [Apache CSV](https://commons.apache.org/proper/commons-csv/) in place of [OpenCSV](http://opencsv.sourceforge.net/)

Obevo clients from past versions (<= 5.x) will need the following attribute in their system-config.xml file to enable this:
```
<dbSystemConfig csvVersion=”2” …
```

### Technical Improvements

Assorted SDLC fixes  around the Maven Central upload, Travis CI, etc.


## 6.0.0
Initial open-source release

### Functionality Improvements
Reverse Engineering: more improvements around the db2look reverse engineering for DB2. Work will continue on this in future releases


### Technical Improvements
Obevo now builds on Java 7


### Bug Fixes
Rollback functionality: fixed an issue where a no-op deployment could not be rolled back t

Rollback fix to populate no-op deployments into table to facilitate rollback

Oracle: Fix in audit data table persistence

Maven Mojo: cleanBuildAllowed attribute was not being respected in the call to the "clean" goal in the Maven mojo (i.e. cleans were executing even if the attribute was false)

Deploy logic: mixup in connection usage when multiple environments were being deployed to in the same JVM
