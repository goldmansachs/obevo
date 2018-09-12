# Contributing to Obevo

This file contains information about reporting issues as well as contributing code. Make sure
you read our [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md) before you start participating.


# Development and Build Environment

Obevo's development environment relies on the following:

### 1) Java 8 for build, and Java 7 for runtime / usage

To obtain for:
* MacOS: use the Homebrew package manager
* Any OS: you can also try the Oracle links
** [Oracle JDK Download](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
** [OpenJDK Download](http://openjdk.java.net/install/)

Note:
* Language: Java 7 syntax and Kotlin (compiling Kotlin to Java 6 bytecode).
* The Obevo binary byte code is in Java 7, so Java 7 users can still leverage Obevo.
* We do still need Java 8 for build-time as the Kotlin libraries need Java 8 to run.
* To enforce Java 7 compatibility, we use:
** [Animal Sniffer plugin](https://www.mojohaus.org/animal-sniffer/) to ensure that we do not refer to any JDK 8 APIs
** Tests against the Java 7 binary in Travis CI

### 2) Maven 3.3.1 or higher

[Download](https://maven.apache.org/download.cgi)

### 3) IDE

The main developers use [IntelliJ](https://www.jetbrains.com/idea/download/). Hence, the IDE files for IntelliJ (*.iml, .idea) have been checked in - this particularly
helps with enforcing the code formatting standards.

If you use another IDE and would like to commit the IDE files, please raise an issue and discuss with the Obevo team.

### 4) (Optional) Docker

We use [Docker](https://www.docker.com/get-started) for integrating testing some of the DBMS environments. More
instructions are available in the developer guide.


# Developer Guide

Please visit the [Developer Guide](https://goldmansachs.github.io/obevo/developer-guide.html) for notes on
developing with Obevo.

The guide includes:

* Navigating the code, unit tests, and integration tests
* How to setup your own test databases for each supported DBMS type
* Guidance on adding new DBMS implementations


# Issues
Search the [issue tracker](https://github.com/goldmansachs/obevo/issues) for a relevant issue or create a new one.

For good candidates to try out, use the "good first issue", "help wanted - small", or "help wanted - medium" labels.

To contribute a test case follow the same process as contributing a feature.


# Making changes
Fork the repository in GitHub and make changes in your fork.

Please add a description of your changes to the [Change Log](CHANGELOG.md).

Before you submit your first pull request, please first submit a DCO, per the instructions in the last seciton on this page.

Finally, submit a pull request. In your pull requests:
* Make sure you [rebase your fork](https://github.com/edx/edx-platform/wiki/How-to-Rebase-a-Pull-Request) so that pull requests can be fast-forward merges.
* We generally prefer squashed commits, unless multi-commits add clarity or are required for mixed copyright commits.
* Your commit message for your code must contain a `covered by: <dco>` line. See above.
* Every file you modify should contain a single line with copyright information after the Apache header:
```
//Portions copyright <copyright holder>. Licensed under Apache 2.0 license
```
* New files must contain the standard Apache 2.0 header with appropriate copyright holder.
* If you're going to contribute code from other open source projects, commit that code first with `covered by: <license>`
where `<license>` is license of the code being committed. Ensure the file retains its original copyright notice and add an appropriate line to
NOTICE.txt in the same commit. You can then modify that code in subsequent commits with a reference to your DCO and copyright.


# Coding Style
Obevo follows a coding style that is similar to [Google's Style Guide for Java](https://google.github.io/styleguide/javaguide.html).
Many aspects of the style guide are enforced by CheckStyle and the IntelliJ code formatter, but not all, so please take care.

Simple notes on the style:
* Use 4 spaces for indentation. No tabs.
* Use the [https://en.wikipedia.org/wiki/Indent_style#Variant:_1TBS_.28OTBS.29](egyptian style braces coding) for braces.


# Appendix: Contribution Prerequisite: Submitting a DCO

If you have never contributed to Obevo, or your copyright ownership has changed, you must first create a pull request that has
a developer certificate of origin (DCO) in it. To create this file, follow these steps:

For code you write, determine who the copyright owner is. If you are employed in the US, it's likely that your
employer can exert copyright ownership over your work, even if the work was not done during regular working hours or
using the employer's equipment. Copyright law is highly variable from jurisdiction to jurisdiction. Consult your
employer or a lawyer if you are not sure.

If you've determined that the copyright holder for the code you write is yourself, 
please fill out the following (replace all `<>` terms); place it in a file under `dco/<your name>.dco`. 

```
1) I, <your name>, certify that all work committed with the commit message 
"covered by: <your name>.dco" is my original work and I own the copyright 
to this work. I agree to contribute this code under the Apache 2.0 license.

2) I understand and agree all contribution including all personal 
information I submit with it is maintained indefinitely and may be 
redistributed consistent with the open source license(s) involved. 

This certification is effective for all code contributed from <date submitted> to 9999-01-01.
```

If you've determined that the copyright holder for the code you write is some other entity (e.g. your employer), 
you must ensure that you are authorized by the copyright holder to be able to license this code under the 
Apache 2.0 license for the purpose of contribution to Obevo. Negotiating such authorization and administering 
the terms is entirely between you and the copyright holder. Please fill out the following (replace all
`<>` terms); place it in a file under `dco/<copyright holder name>-<your name>.dco`. 

```
1) I, <your name>, certify that all work committed with the commit message 
"covered by: <copyright holder name>-<your name>.dco" is copyright 
<copyright holder name> and that I am authorized by <copyright holder name> 
to contribute this code under the Apache 2.0 license.

2) I understand and agree all contribution including all personal 
information I submit with it is maintained indefinitely and may be 
redistributed consistent with the open source license(s) involved. 

This certification is effective for all code contributed from <date submitted> to 9999-01-01.
```

`<your name>` must reference your real name; we will not accept aliases, pseudonyms or anonymous contributions.
Issue a pull request with the appropriate DCO and a change to NOTICE.txt with
one line `This product contains code copyright <copyright holder name>, licensed under Apache 2.0 license`.
