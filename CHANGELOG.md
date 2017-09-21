# Change Log

## 6.3.0

### Bug Fixes

#73: Allowing the encoding to be specified when reading files.


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

### Technical Fixes

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

### Technical Fixes

Assorted SDLC fixes  around the Maven Central upload, Travis CI, etc.


## 6.0.0
Initial open-source release

### Functionality Improvements
Reverse Engineering: more improvements around the db2look reverse engineering for DB2. Work will continue on this in future releases


### Technical Changes
Obevo now builds on Java 7


### Bug Fixes
Rollback functionality: fixed an issue where a no-op deployment could not be rolled back t
Rollback fix to populate no-op deployments into table to facilitate rollback

Oracle: Fix in audit data table persistence

Maven Mojo: cleanBuildAllowed attribute was not being respected in the call to the "clean" goal in the Maven mojo (i.e. cleans were executing even if the attribute was false)

Deploy logic: mixup in connection usage when multiple environments were being deployed to in the same JVM
