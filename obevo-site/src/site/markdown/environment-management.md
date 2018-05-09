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

# Environment Management

Ideally, the same code will get deployed to all your environments.
However, you may have use cases where you need to only deploy some
changes to certain environments. This page describes how to handle that
with Obevo.

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->


## Tokenization

You can define tokens per environment to tokenize your DB code

```
<!-- In your system-config.xml file -->
<environments>
    <dbEnvironment name="qa" cleanBuildAllowed="false" dbDataSourceName="NYQA01">
        <tokens>
            <token key="TABLESPACE1" value="QA_TSP_001" />
            <token key="MY_OTHER_TOKEN" value="qaToken" />
        </tokens>
    </dbEnvironment>
    <dbEnvironment name="prod" cleanBuildAllowed="false" dbDataSourceName="NYPROD01">
        <tokens>
            <token key="TABLESPACE1" value="PROD_TSP_001" />
            <token key="MY_OTHER_TOKEN" value="prodToken" />
        </tokens>
    </dbEnvironment>
</environments>
```

Then refer to those in your source files

```
//// CHANGE name="mychange"
create table MYTABLE (
    FIELD1 int
) in ${TABLESPACE1}
```

Note that you can tokenize a //// CHANGE file even after it has been
deployed, so long as applying the token results in the same content after

You can also define tokens in your permissions section in your
system-config.xml (see [Permission Management](permission-management.html)) if you need to tokenize that.


## The //// METADATA annotation

The //// METADATA annotation line is used to declare special attributes
within your object files. We will describe the functionalities that can
be enabled with this shortly. But first, let's review the format:

This line needs to be the first line of the db object file, and it can
go in any of the file types. It can define attributes and toggles, e.g.

```
//// METADATA attr1="val1" attr2="val2" toggle1 toggle
```

-   Attributes have a name and a value
-   Toggles represent boolean attributes (the presence of a toggle indicates that the value is true; otherwise, false)


## Environment-specific deployments

You need need certain objects/scripts to only run against certain
environments. An example is static metadata tables that differ across
environments (e.g. some values in qa, some in prod, etc.)

As mentioned in the [DB Project Structure](db-project-structure.html)
section, your file name can allow this, e.g. /data/MYCODES.qa.sql
/data/MYCODES.uat.sql /data/MYCODES.prod.sql

In addition, you need the "includeEnvs" or "excludeEnvs" attribute
under //// METADATA or //// CHANGE

This means that it would include/exclude the environments you list out
for that attribute. You cannot use both (it would not make sense); just
one or the other

The list can be comma separated, and you can use the wildcard %

Examples:

-   //// METADATA includeEnvs=prod1,prod2
-   //// METADATA excludeEnvs=dev
-   //// CHANGE name="abc" includeEnvs=qa,uat%


## Platform-specific deployments

\[since version 5.x\] Similar to environment-specific deployments,
changes can be limited based on the platform type. This is utilitized
particularly for the in-memory translation use case (i.e. to have
certain SQLs executed for the regular platform and others for in-memory)

Use the includePlatforms/excludePlatforms attributes for this, using the
names given for the platforms as keys, e.g.:

-   //// METADATA includePlatforms=DB2
-   //// CHANGE name="abc" excludePlatforms=HSQL


## Common metadata in packages (package-info.txt)

If you want all files in a particular folder to have the same metadata,
you can add a package-info.txt file to that folder and add content as
needed.

For example:

\#\#\# /migration/package-info.txt

```
//// METADATA excludeEnvs="db1*"
```

A common usecase for this is if you want certain files to only be
deployed to certain environments (i.e. you name your QA environments in
a particular way, starting w/ qa, and you define static data for qa in
such a folder)

Note that this is currently *NOT* RECURSIVE. So if you define this in
one folder, it will not trickle to its subchildren
