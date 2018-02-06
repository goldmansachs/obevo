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


# Releasing Obevo

## Release Steps

Run the maven release plugin to prepare the build. This will invoke the tests.

```mvn release:clean release:prepare```

Once succeeded, perform the release to upload the artifacts to maven central. Watch out for the GPG keychain password prompt.

```mvn release:perform```


## Implementation Details

(We document this here only for knowledge purposes)

Follow the instructions on the [Sonatype OSSRH Guide](http://central.sonatype.org/pages/ossrh-guide.html).

Include the drilldown page that [deploys via Apache Maven](http://central.sonatype.org/pages/apache-maven.html).
* We do not follow the steps on "Nexus Staging Maven Plugin for Deployment and Release"
* Instead, we use the maven-release-plugin: see the "Performing a Release Deployment with the Maven Release Plugin" section

Note the "maven-release-plugin" declaration in the parent pom here; see the references to the _release_ profile and the
release profile definition itself.



# Dealing with branches in Github

## When committing

Add the main branch as the upstream to your fork
```git remote add upstream https://github.com/goldmansachs/obevo.git```

We request that pull requests are squashed into one commit before the pull request. Use these commands to do that
```
git rebase -i HEAD~3
git push -f
```

Finally, do a pull reqeust in Github.


## When pulling changes from upstream

```
git pull --rebase upstream master
git push -f
```
