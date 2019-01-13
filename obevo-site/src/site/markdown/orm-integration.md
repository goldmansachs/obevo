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

# ORM Integration

A standard practice nowadays in db programming is to use ORM tools, like Hibernate or Reladomo, to interact with your
database in your systems. These tools provide a way to map from your Java objects to your DB objects. If you are on an
existing system and schema, you would have to rely on the mapping abilities of the framework to map to your schema.

These ORMs also come with a feature to generate a DB schema from your object model; i.e. to "forward-engineer" what the
schema should be. If you were starting a system from scratch, then this is great - you can define your objects in one
place, and then generate your schema from there, so that you can keep your DB and Java objects in sync, as opposed to
needing to make changes in two places...

...However, the ORM tools will generate the full version of your table; it will not generate the alters to get you from 
whatever is in your Java objects to the actual DB (or at least, it is not easily to leverage such features in practice, 
or you need to have a finer control over the table change anyway).

But still, this is an overall positive - you will get the initial version of your table ddl, and the baselined version. 
Obevo has a plugin for Hibernate and Reladomo to generate the table DDLs from a Hibernate configuration and to output it to the
format that Obevo expects (and leaving room for the third piece to create subsequent alters, which we'll go through in 
the next section). (More ORMs can be supported in the future; please raise a Github issue for this)

<!-- MACRO{toc|fromDepth=1|toDepth=1} -->


## Overall ORM Integration Workflow

Each ORM reverse-engineering API generally works by taking in your model input and writing the reverse-engineered DDLs
to your chosen output directory.

The output directory will have the following:

1. /interim folder that has the raw output from the reverse-engineering (Hibernate only)
2. /final folder that has your reverse-engineered DDLs
3. /final/system-config.xml - example system-config.xml that you can use
4. /final/yourSchema/table/*.sql - your table DDLs
5. /final/yourSchema/table/baseline/*.sql - your baseline table DDLs (will explain this shortly)

Though you can execute this as a one-time exercise to generate DDLs for production deployment, you can also incorporate
this into your standard SDLC:

* Generate the reverse-engineered output as part of your standard build
* Run unit tests against the generated build (e.g. using the [in-memory testing](in-memory-db-testing.html) functionality.

### Incorporating Baseline Validation (optional)

There is one initial catch - the ORM generator will only safely generate the latest version of the DDL and not account
for any incremental deltas in your scripts (e.g. whether differences against production or accounting for //// CHANGE
entries in your existing scripts). Ideally, you'd like your ORM reverse-engineered output to tie to your production code
somehow.

To do this, we generate both a regular DDL file and a "baseline" DDL file.

* The regular DDL file is meant for your regular DB deployments. It will only be generated if the file didn't previously
exist. Once written, you the developer are responsible for modifying it (i.e. adding alter and //// CHANGE statements)
* The baseline DDL file is always overwritten with the latest DDL view from the ORM. The developer is not supposed to
modify this file.

With both those in place, you can now reconcile the two using the [Baseline Validation](baseline-validation.html) utility.

Note that the baseline generation and validation is optional. You can do the DDL generation as this without the validation
step and still gain benefits; the baseline functionality simply adds an extra validation that your ORM code matches your
production deployments.

Now onto the ORM-specific APIs for reverse-engineering ...

## Hibernate Integration

Hibernate has APIs to generate a DDL for a given Hibernate ORM POJO model. This DDL output can be reverse-engineered into
the Obevo format.

Obevo also has some helper utilities to invoke the Hibernate reverse-engineering calls. However, note that these APIs
have changed from Hibernate 3 to 4 to 5. We do have implementations for each version.

### High Level Steps to Execute

1. Only available via Java API (not our native Maven API or command-line interface)
2. Pass in your annotated POJO classes to the API (or Configuration class for Hibernate 3 and 4)
3. Use the dependency corresponding to your Hibernate version

### Usage and Example

1) Add the Hibernate dependency to your pom based on your version:

* obevo-hibernate3-util
* obevo-hibernate4-util
* obevo-hibernate5-util

```
<dependency>
    <groupId>com.goldmansachs.obevo</groupId>
    <artifactId>obevo-hibernate5-util</artifactId>
    <version>${obevo.version}</version>
</dependency>
```

(Note - each of these dependencies has a transitive dependency on obevo-hibernate-util, which has the API code you will
interact with)


2) Example is as follows

```
import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.gs.obevo.db.impl.platforms.db2.Db2DbPlatform;
import com.gs.obevo.hibernate.HibClassA;
import com.gs.obevo.hibernate.HibClassB;
import com.gs.obevo.hibernate.HibClassSchemaC;
import com.gs.obevo.hibernate.HibClassSchemaD;
import com.gs.obevo.hibernate.HibernateReveng;
import com.gs.obevo.hibernate.HibernateRevengArgs;
import com.gs.obevo.hibernate.HibernateRevengFactory;
import com.gs.obevo.hibernate.HibernateRevengTest;
import org.hibernate.dialect.DB2Dialect;

public void reverseEngineeringExample() {
    HibernateReveng<List<? extends Class<?>>> reveng = HibernateRevengFactory.getInstance().getHibernate5();

    HibernateRevengArgs args = new HibernateRevengArgs(
            "yourDbSchema"  // the schema that you want to generate. If your ORM model does not
                                    // define a schema, this value will be the default in the reverse-engineering output
            , new File("/home/youruser/yourOutputDir")  // the output directory to write to
            , new Db2DbPlatform()  // the Obevo platform that you want to reverse-engineer your model into
            , DB2Dialect.class  // the Hibernate dialect class corresponding to the DBMS platform you are reverse-engineering to
            , Arrays.asList(HibClassA.class, HibClassB.class, HibClassSchemaC.class, HibClassSchemaD.class)  // your ORM POJOs
    );

    // Optional parameters
    args.setGenerateBaseline(true);  // defaults to false: Whether to generate the baseline DDLs (per section above)
    args.setPostCreateTableSql(" lock datarows");   // defaults to blank "": This appends the given SQL to the end
                                                    // of your "create table" statements (i.e. for storage specifications)
    args.setGenerateForeignKeys(true);  // defaults to true: whether to include foreign keys in your reverse engineering
    args.withExplicitSchemaRequired(false);  // defaults to false: whether to exclude objects mapped to schemas that
                                             // are different from the "schema" argument above

    reveng.executeReveng(args);
}
```


3) You can wire in this call into your SDLC as you see fit. If you have suggestions on any native-integrations into
existing build tooling that can make this easier (e.g. with Maven, Gradle, etc.), please raise an issue on Github.



## Reladomo Integration

### Reverse-engineering files generated from Reladomo

1) Generate the DDLs using the Reladomo API. (See the Reladomo documentation for how to do this). This will result in
an output directory structure like this:

```
H:\yourInputDir\APP_INFO_DEPLOYMENT_SERVER.ddl
H:\yourInputDir\APP_INFO_DEPLOYMENT_SERVER.fk
H:\yourInputDir\APP_INFO_DEPLOYMENT_SERVER.idx
H:\yourInputDir\APPLICATION_USER_ROLE.ddl
H:\yourInputDir\APPLICATION_USER_ROLE.idx
```

2) Run the Obevo reverse-engineering command to get those files into the Obevo format:

Via Command Line:

```
%OBEVO_HOME%\bin\deploy.bat RELADOMOREVENG -dbType DB2 -inputDir h:\reveng-example -outputDir h:\reveng-example-output [-dontGenerateBaseline] [-dbSchema yourSchemaName]
```

Arguments

* dbType: (Required) The DBMS type that the DDLs were generated for; use one of the acronym values from [Platform Listing](platform-listing.html)
* inputDir: (Required) The directory that contains the Reladomo DDLs
* outputDir: (Required) The directory to write the Obevo DDLs. The format would look like below.

```
H:\yourOutputDir\yourSchema\table\APP_INFO_DEPLOYMENT_SERVER.sql
H:\yourOutputDir\yourSchema\table\APPLICATION_USER_ROLE.sql
H:\yourOutputDir\yourSchema\table\baseline\APP_INFO_DEPLOYMENT_SERVER.baseline.sql
H:\yourOutputDir\yourSchema\table\baseline\APPLICATION_USER_ROLE.baseline.sql
```

* dontGenerateBaseline: (Optional) Whether to generate the baseline files. See [the Baseline Validation Page](baseline-validation.html) for more information
* dbSchema: (Optional)

Via Java API:

* Add this to Maven

```
<dependency>
    <groupId>com.goldmansachs.obevo</groupId>
    <artifactId>obevo-reladomo-util</artifactId>
    <version>${obevo.version}</version>
</dependency>
```

* Run this code:

```
ReladomoSchemaConverter schemaConverter = new ReladomoSchemaConverter();
schemaConverter.convertReladomoDdlsToDaFormat(platform, new File("./src/test/resources/reveng/input"), outputFolder, "yourSchema", true);
```

### Generating DDLs directly from Reladomo API

In the future, we would like to support generating the Reladomo DDLs directly from an API (as opposed to having to
generate the DDLs first in the Reladomo format and then reverse-engineering to the Obevo format). This is not yet
available.
