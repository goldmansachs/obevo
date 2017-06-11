# Change Log

## 6.1.0

### Functionality Improvements

Reverse-engineering added for Oracle, Postgres, SQL Server


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
