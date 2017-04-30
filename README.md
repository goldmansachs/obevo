# Obevo
[![][travis img]][travis]
[![][maven img]][maven]
[![][release img]][release]
[![][license-apache img]][license-apache]

[![Build Status abc](https://travis-ci.org/goldmansachs/obevo.svg?branch=master)](https://travis-ci.org/goldmansachs/obevo)
[![](https://maven-badges.herokuapp.com/maven-central/com.goldmansachs.obevo/obevo-parent/badge.svg)](http://search.maven.org/#search|gav|1|g:"com.goldmansachs.obevo"%20AND%20a:"obevo-parent")


[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.goldmansachs.obevo/obevo-parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.goldmansachs.obevo/obevo-parent)

## What is it? 
Obevo is a database deployment tool that handles enterprise scale and complexity:

* Organized maintenance of all your DB-object files (tables, stored procedures, views, static data)
** 1 file per object - just as you would with Java files
** Edit stored procedures, views, and static data files in place (if you have them) - just as you would with Java files
** No need to specify the order in which to deploy your files - Obevo figures it out for you (a la a Java compiler compiling classes)
* Variety of DBMSs supported to DB2, Sybase ASE, Sybase IQ
** Is extensible to non-RDBMSs
* In-memory DB support - convert your scripts to an in-memory database for unit testing support
* Easy onboarding - get your production schema checked in and deployed to other environments in under 2 hours
* Runnable from Java, command-line, or maven
* Easy integration into your SDLC and deployment - package and deploy alongside your Java code and run a simple command like this:
* $OBEVO_HOME/bin/deploy.sh -dir /home/myuser/myproject/latest-version/db -env prod
* Extra features like cleaning and rebuilding existing schemas and detecting/firing reorgs

## Sample Project
To help getting started with Obevo, a simple project is available with maven and gradle build set-up.

Prerequisite: install maven or gradle.

```
git clone https://github.com/goldmansachs/obevo-kata.git
```

#### Maven
```
mvn clean install
```

#### Gradle
```
gradle clean build
```



## Documentation

Documentation is available [online] (https://goldmansachs.github.io/obevo/)


## Acquiring Obevo

* [Versions] (https://github.com/goldmansachs/obevo/releases)
* [Maven Central] (http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.goldmansachs.obevo%22)


[travis]:https://travis-ci.org/goldmansachs/obevo
[travis img]:https://travis-ci.org/goldmansachs/obevo.svg?branch=master

[license-apache]:LICENSE.txt
[license-apache img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[maven]:http://search.maven.org/#search|gav|1|g:"com.goldmansachs.obevo"%20AND%20a:"obevo-parent"
[maven img]:https://maven-badges.herokuapp.com/maven-central/com.goldmansachs.obevo/obevo/badge.svg

[release]:https://github.com/goldmansachs/obevo/releases
[release img]:https://img.shields.io/github/release/goldmansachs/obevo.svg
