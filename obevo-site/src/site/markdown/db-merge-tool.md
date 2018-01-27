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

# DB Merge Tool

We have created a tool to help teams merge database schemas. i.e.

1. Generate a merged-view of your reverse-engineered files from the [Full Existing Onboarding Guide](existing-onboarding-guide.html)
2. Generating and executing the SQLs that can be applied to get the schemas to match each other

# Use Case for the DB Merge Tool

You should consider this step if you have the following use case:

* You have a single app that you consider &quot;production&quot;, but of which you have many sharded instances, say like product line or location.
* For whatever reason (e.g. not applying schemas in a consistent manner to each instance), these schemas have diverged over time to the point where it is difficult to even migrate changes consistently across instances, or that you are very unsure on if a certain db object is the same across instances
* However, there is enough similarity where you want to at least manage these in the same codebase, where you can maintain some &quot;common db objects&quot; that are applied to each schema

High-level approach to resolve this:

1) Certainly, it would be nice if these schema definitions were consolidated before onboarding to Obevo.
* In practical terms, it may be very time-consuming to do so, and a lack of appetite to do so given the number of cleanups

2) At the very least, it could be desirable to create a merged codebase with the common vs. instance-specific objects highlighted, so that

1. when developers make changes, they are aware if it is for a common table or an instance-specific table
2. over time, the team can slowly look to consolidate the tables by applying the changes to the schema using Obevo

3) Once that merged view is created, teams can analyze the diffs and choose whether to leave them
maintained separately, or to try to clean up the db. Obevo provides help to generate the diff SQLs if you choose.

NOTE - all the steps below can work to merge any number of instances (i.e. an n-way merge, not just a 2-way merge).

Here the steps on how to do each part of the merge:

# Merge Step 1 - Create the Merge Diff View

Specific steps on how to do the merge:

1) Perform the reverse engineering steps for each instance/shard of your prod db (i.e. repeating steps
1-4 from the [Full Existing Onboarding Guide](full-existing-onboarding-guide.html)). Note
the DBs where your reverse-engineering output is generated

2) Execute the merge command, with the arguments and an example as below:

* outputDir - the folder where you want the merge output generated
* dbMergeConfigFile - the path to a file describing your merge settings (see the example below for how the file is configured)

```
%OBEVO_HOME%\bin\deploy.bat DBREVENGMERGE -outputDir h:\merged-output -dbMergeConfigFile h:\myDbMergeConfig.txt
```

h:\myDbMergeConfig.txt contents:

```
# Specify the DB Type you are working with
dbType=ORACLE
# First, list out all the instances
instances=instance1,instance2,instance3
# Then for each instance, define the inputDir where your reverse-engineered files from the previous step are
instance1.inputDir=h:\instance1dboutput
instance2.inputDir=h:\instance2dboutput
instance3.inputDir=h:\instance3dboutput
```

### Example
To walk through an example: let's say that for our example app, we have 3 instances: cd, eq, fx. The resulting directories are as follows:

![Merge Prev-View](images/merge-pre-view.png)

Let's assume all these folders are in H:\mydboutput

The command to run the reverse engineering would be as follows:

```
%OBEVO_HOME%\bin\deploy.bat DBREVENGMERGE -outputDir h:\merged-output -inputDirEntries h:\mergeConfig.txt
```

* Note that we expect the input directories to be the parent of the schema folders, not the parent of the db object folders

h:\mergeConfig.txt contents:

```
dbType=ORACLE
instances=cd,eq,fx
cd.inputDir=h:\mydboutput\cd
eq.inputDir=h:\mydboutput\eq
fx.inputDir=h:\mydboutput\fx
```

After running that command, the output is as follows. We expand only the &quot;view&quot; folder for this doc.

These fall into 3 flavors:

1. Objects that exist in all the instances that we had defined and are identical across those are stored directly under the &quot;view&quot; folder
2. The folders marked as &quot;only-...&quot; are cases where the objects contained within are identical for those instances (but missing in the others)
 * For example, the objects in only-cd-eq are in the cd and eq instances, but not the FX instances
3. The folders without &quot;only-&quot; are cases where the objects contained within are different across instances.

From here, the assumption is that when you define your environment names (in the next step when you create
system-config.xml), you should name the environment for that instance with that instance prefix. e.g.
cd-dev, cd-uat1, cd-prod1, eq-dev, eq-qa, eq-prod, ... Fyi, this is done via the package-info.txt file
stored in each folder (the merge step will generate this for you); will add more on this file in the &quot;Advanced
Use Cases&quot; section below

![Merge Post-View](images/merge-post-view.png)


# Merge Step 2 - Try to clean up the db by creating/executing SQLs for the merged view"
<font color="red">PLEASE NOTE: This feature is an Alpha-quality version (i.e. early Incubation, not tried
out by other teams). No work imminently planned on this, but we can look more into this if there is demand.
Any brave souls who would like to try this out, please reach out to the [Obevo team](support.html) first to coordinate.
</font>

Given the merge view, you can look to consolidate where possible if you are ambitious. The steps are
different based on the objects that are different.

# For Stateless Objects (e.g. SP, view, static data)
You basically just need to code for these tables to try to merge them. After all, these just incorporate
logic, so if you wanted to combine, you'd need the appropriate logic to take care of it. (or, you can choose
to keep them separate)

For any cases where you have an &quot;only&quot; case, i.e. objects only deployed in some instances but not
all, you can simply deploy the missing object to the other schemas.

# For Tables
The tricky bit is for tables, as they are not stateless and require alters to get them in line.

What Obevo can assist you with is generating SQLs to create a &quot;merged view&quot; of the
tables. This merged view is based on some heuristics, e.g.

* If columns are missing in some instances but not others, add them
* If indexes/pks are missing in some instances but not others, add them
* If columns exist in instances but have differences among them, e.g. nullability or data type, you will be notified and need to resolve them

Obviously, you can choose not to use the proposals, or to leave them as is. The tooling simply provides you
some visibility into it.

Steps to do this:

1. Deploy the schemas that you've reverse-engineered to separate instances in your dev environment
2. Run the following command: (command looks mostly the same as above, except it is DBREVENG*TABLE*MERGE):

```
%OBEVO_HOME%\bin\deploy.bat DBREVENGTABLEMERGE -outputDir h:\merged-output -dbMergeConfigFile h:\myDbMergeConfig.txt
```

h:myDbMergeConfig.txt contents (this has more stuff too, now includes the db information):

```
dbType=ORACLE
instances=instance1,instance2,instance3
# Then for each instance, define the inputDir where your reverse-engineered files from the previous step are
instance1.inputDir=h:\instance1dboutput
instance1.driverClassName=com.sybase.jdbc3.jdbc.SybDriver
instance1.url=jdbc:sybase:Tds:myserver1.me.com:1234
instance1.username=myuser1
instance1.password=mypass1
instance1.physicalSchema=INST1
instance2.inputDir=h:\instance2dboutput
instance2.driverClassName=com.sybase.jdbc3.jdbc.SybDriver
instance2.url=jdbc:sybase:Tds:myserver2.me.com:1234
instance2.username=myuser2
instance2.password=mypass2
instance2.physicalSchema=INST2
instance3.inputDir=h:\instance3dboutput
instance3.driverClassName=com.sybase.jdbc3.jdbc.SybDriver
instance3.url=jdbc:sybase:Tds:myserver3.me.com:1234
instance3.username=myuser3
instance3.password=mypass3
instance3.physicalSchema=INST3
```

Essentially, you add the db connection params, including the schema, basically to where you deployed to.
The tool will do a diff among the tables and seeks to create a merged view of each table, i.e.

* If columns exist in B and not in A or C, generate alters to add them to A and C
* If columns exist in A and not in B or C, generate alters to add them to B and C
* If indices exist in B and C and not in A, generate a create-index to add them to A
* If a column is nullable in A but not nullable in B, make it nullable in B, as that is compatible across instances (note - this one is not yet implemented)
* If a primary key exists in A but the equivalent exists as a unique index in B, convert the unique index in B to a primary key

There are certain differences that are not resolvable automatically. The tool flags these for you, e.g.

* Data type diff (e.g. varchar vs. int)

The tool will just generate the SQLs and put them into a file format; it will not actually execute them.
Hence, you are free to modify the SQLs as you choose. Worst-case, you can just use this for your analysis.

To execute it, do it manually. Yes, this is not automated and unlike Obevo! This is because this
feature is still early (call it an &quot;alpha version&quot; and cleanup is tricky; we can add support for
this in Obevo eventually to execute these cleanups, but for now, let's do this manually so that
teams don't actually execute alters that they don't want to.

Assuming you choose to proceed w/ the cleanups: execute them in UAT and re-compare the schemas (e.g. using
Aqua Data Studio). If you are happy, go ahead and execute them in prod. Then, reverse-engineer again to
ensure you have the latest view of the ddls in your code, and thus you can go w/ 1 set of ddls for your
environment

(now obviously, it may not be possible to clean up all objects; feel free to use the package-info.txt files
and the //// METADATA includeEnvs=... or excludeEnvs=... as needed to segregate the files per environment)

