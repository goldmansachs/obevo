# Building Obevo

Requirements:

### Java 7 or higher
* [Oracle JDK Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [OpenJDK Download](http://openjdk.java.net/install/)

Note that it is possible to develop with JDK8, but
* the code must conform to Java 7 language syntax
* libraries must be Java 7 compatible.


### Maven 3.2.5 or higher

[Download](https://maven.apache.org/download.cgi)


### IDE

The main developers use IntelliJ. Hence, the IDE files for IntelliJ (*.iml, .idea) have been checked in - this particularly
helps with enforcing the code formatting standards.

If you use another IDE and would like to commit the IDE files, please raise an issue and discuss with the Obevo team.


### Developer Guide

Please visit the [Developer Guide](https://goldmansachs.github.io/obevo/developer-guide.html) for notes on
developing with Obevo.

The guide includes:

* Navigating the code, unit tests, and integration tests
* How to setup your own test databases for each supported DBMS type
* Guidance on adding new DBMS implementations
