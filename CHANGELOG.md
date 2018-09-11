# Change Log

## 7.0.0

### Functionality Improvements

### Technical Improvements
Moving more of the code over to Kotlin
#153: Refactor of dependency implementation
#193: Docker onboarding foor testing

### Bug Fixes


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
