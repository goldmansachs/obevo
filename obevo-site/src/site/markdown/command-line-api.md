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
# Command-line API

The command line format is generally as follows:

```
$OBEVO_PATH/bin/deploy.sh <COMMAND> -arg1 arg -arg2 arg etc
```

or if run via Docker:

```
docker run --rm -it --mount type=bind,source="$SOURCEPATH",target=/mysourcepath,readonly shantstepanian/obevo <COMMAND> -arg1 arg -arg2 arg etc

SOURCEPATH=/Users/shantstepanian/IdeaProjects/obevo/obevo-db-impls/obevo-db-h2/src/test/resources/platforms/h2/step1
docker run --rm -it --mount type=bind,source="$SOURCEPATH",target=/mysourcepath,readonly shantstepanian/obevo DEPLOY -sourcePath /mysourcepath
docker run --rm -it -v "$SOURCEPATH",/mysourcepath shantstepanian/obevo DEPLOY -sourcePath /mysourcepath
```

where &lt;COMMAND&gt; is one of the following:

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

### DEPLOY

This is the main command for Obevo that will deploy your source changes to your target environment. It will

1. read your source changes
2. read the changes already deployed in the audit table
3. calculate the difference
4. deploy the changes.

Listing of the commonly-used arguments (for full descriptions, run DEPLOY without any args):

-sourcePath &lt;arg&gt;

* The path to your DB source path (i.e. the directory where your system config file resides, or the direct path to the system config file itself
* Required

-action &lt;arg&gt;

* 2 possible arguments:
 * clean - wipes away all objects from the schemas defined in your config. Useful for dev and test environments
 * deploy - does a standard deploy
* You can specify as a comma-separated list if you want to do multiple commands
* Optional - defaults to deploy

-env &lt;arg&gt;

* The environment name/s defined in your config that you want to deploy. You can specify a comma-separated list or a wildcard via * or % (i.e. dev0*, %prod)
* Optional if the config at your source path only defines 1 environment, then that is chosen. Otherwise, this is required.

Credentials: -deployUserId &lt;arg&gt;, -password &lt;arg&gt;, -useKerberosAuth

* Either provide deployUserId + password, or deployUserId + useKerberos if you kinit beforehand and the platform supports it
* Optional: if not provided at command line or from config, you will be prompted

-noPrompt

* Defines if the deployment will proceed without any user interaction or prompting
* Optional - defaults to false (i.e. users are prompted by default)

-forceEnvSetup

* Defines whether to create infrastructure objects (e.g. schemas, users) prior to object deployment
* Optional - the default is based on the platform-chosen. (in-memory testing environments will create the infrastructure by default; others will not)


### PREVIEW

PREVIEW is a read-only mode command that will show the user the changes that expect to be deployed (i.e.
the first 3 steps mentioned in the DEPLOY command), but it will not actually deploy the changes.

This is a convenience for teams that just want to see the changes that will get deployed using a read-only db id

Same args as DEPLOY are used, except for -action


### INIT
The INIT command is used when onboarding an existing system to Obevo to mark your initial schema as "already deployed" in the system.

See the [Onboarding Guide](onboarding-guide.html) for details.

Essentially, this will execute a DEPLOY command, just without executing your source code to the DB - it will just update the deploy audit table.


### Reverse Engineering Commands
These commands are used when you need to onboard an existing system to Obevo.

They would extract the DB schema from your existing data and put it into the Obevo file format.

See the [Onboarding Guide](onboarding-guide.html) for details.


# Configuring Custom Java Arguments for the Obevo JVM

Using environment variables, you can configure the JVM arguments that are used for the Obevo process:

* OBEVO_CLASSPATH: any jars that you want to prepend to the regular Obevo classpath
* OBEVO_JAVA_OPTS: any system arguments or JVM args that you want to set for the process.

Windows Example:

```
SET OBEVO_CLASSPATH=C:\drivers\my-additional-driver.jar
C:\Obevo\bin\deploy DEPLOY -sourcePath ...
```


Linux Example:

```
export OBEVO_JAVA_OPTS="-Xmx512M -Dmysysprop=myvalue"
/opt/obevo/bin/deploy DEPLOY -sourcePath ...
```


# Configuring Custom Logging for the Obevo process

Command-line logging is configured for two log types:

* SQL Statements
* All other log statements

Both default to the INFO log level. They will be written to System.out and a log file under &lt;tempDir&gt;/obevo.

Obevo uses the [SLF4J API](https://www.slf4j.org), but the underlying implementation is [Logback](https://logback.qos.ch). The default configuration
file is in $OBEVO_HOME/conf.

The following system properties can modify this behavior (the previous section describes how to pass in system properties
to Obevo).

* obevo.log.level: The log level (trace/debug/info/warn/error) to use for all log statements (console and file) except for SQL logs

```
Example:
-Dobevo.log.level=debug
```

* obevo.sqllog.level: The log level (trace/debug/info/warn/error) to use for the SQL log statements (console and file)
* logback.configurationFile: The path to specify to another Logback configuration file, in case you want to fully customize the logging.
 * See the [Logback configuration page](https://logback.qos.ch/manual/configuration.html) for more details
