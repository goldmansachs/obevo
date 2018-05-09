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

# Full Onboarding Guide for Existing Systems

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

This page outlines how to fully onboard an existing system to Obevo

At a high-level, the steps are:

1.  Reverse-engineer your schema from production to set as your source code
2.  Initialize your production and (optionally) UAT environments with Obevo
3.  Do the recommended final steps mentioned in the [Full Onboarding Guide for New Systems](new-onboarding-guide.html)

Note that this implies the production schema is your gold-standard for
your code-base (as this is the exact schema already defined in
production). Thus, you should discard any existing SQLs that you have
been maintaining as they likely would not meet <u>*both*</u> of the following criteria:

-   Be in the file-per-object format that Obevo desires
-   Be in sync with the actual schema in production

One exception here is for static data tables (i.e. delete/insert
statements for code tables / reference data tables / whatever your team
calls them). Such code can easily be reusable.

Estimated time effort:

-   For steps 1-6, a couple hours for a single instance and single
    schema of your production environment, which entail:
    -   getting your schema generated from prod
    -   re-deploying it to a blank dev db
    -   and marking your production schema as "deployed" in the Deploy
        Log
-   At best, your full schema will be deployed to your dev DB;
    otherwise, most of your objects should be deployable
-   The rest of the steps (step \>= 7) are getting your UAT in line,
    knowledge transition to your teams, cleanups as necessary. This time
    would vary depending on your team's situation


## 1) Setup your environment on your local workstation

Follow these [instructions](setup.html). Do this from your regular
desktop environment for ease of use (i.e. no need to run from a
controlled environment at this point).


## 2) Create a blank schema to test your deployment against

It is heavily suggested that that you dedicate a schema just for running
the DB scripts. This will have multiple uses:

-   As a sandbox for you defining your Obevo project
-   For comparing your deployed db against different environments to
    verify your DB scripts (or the state of your other environments)
-   For your continuous builds going forward (i.e. verifying your db
    scripts are fine as developers change it going forward)


## 3) Reverse-Engineering the DDLs from your DB

The next step is to reverse-engineer your DB DDLs from your reference
environment (likely your production environment) to serve as the
starting point for your source code

We have a couple methodologies available for you to use, depending on
your DBMS:

**<u>[[Option 1 (preferred): vendor-provided tools]{.underline}](reverse-engineer-dbmstools.html)</u>** (available for DB2, Oracle, PostgreSQL, SQL Server, Sybase ASE)

-   This option uses the tools provided by the vendors
-   It is relatively new compared to option 2, but it is the way we are
    investing in going forward.
-   Please reach out to the product team via Github if you have any
    questions in using this.

**<u>[[Option 2 (legacy): Aqua Data Studio]{.underline}](reverse-engineer-aquadatastudio.html)</u>**

-   Aqua Data Studio is a tool previously used by the Obevo team in
    their regular DB-based work.
-   However, it is proprietary and not 100% accurate for some edge
    cases; hence, we invested in the vendor-provided tool approach.
-   That said, we already had the instructions for Aqua written, so we
    will continue to provide it for now.

Note that none of the methodologies would include reverse-engineering
the grants, as we recommend teams to use the [Permission Management](permission-management.html) functionality instead


## 4) Review and modify the system-config.xml file

The schema reverse-engineering step will generate a system-config.xml
file for you in your output directory based on the parameters you
provided. Review it and modify it accordingly

To start with:

-   The source of your reverse-engineering (as specified by your
    arguments in the previous step) will be named as the "prod"
    environment in your config, with a separate placeholder environment
    for your dev
-   Use the placeholder dev environments for your deploys during testing
-   Rename the \<schema\> entry to become the logical schema name that you want
-   Use the [kata system-config.xml file](https://github.com/goldmansachs/obevo-kata/tree/master/src/main/database/system-config.xml)
    as a further sample to model from if needed


## 5) (optional) Import your static data as CSV or inserts

### How To Generate the CSV Files

```
## required args:
    %OBEVO_HOME%\bin\deploy.bat DBREVENG -mode data -inputDir %s -outputDir %s -dbType %s -dbHost %s -dbPort %d -dbSchema %s -username %s -password %s

## optional args:
# 1) you can add this for DB2
    -dbServer &lt;serverName&gt; # for DB2 databases that require a server parameter
# 2) one of the following is required, but not both
    -tables table1,table2,table3,... # comma-separated list of tables to reverse-engineer
  or
    -inputDir dir # dir containing a file static-data-tables.txt that has the list of tables, one per line (you can use this in case you have many tables that are hard to pass in by command line)

# 3) If you want to automatically detect certain columns as &quot;updateTime columns&quot; and generate the CSV files as such, use this arg. The &quot;updateTime column&quot; concept is explained below
    -updateTimeColumns col1,col2
```

Clarification for certain params:

-   -outputDir will have the CSV files written there
-   -dbType should be SYBASE*IQ, SYBASE*ASE, DB2
-   -updateTimeColumns:
    -   Often times when we have static data tables, we'd denote a
        field like updateTime for audit purposes, i.e. to indicate when
        the row was added or inserted
    -   But if we define the static data in your source code, then you
        can't truly add the updateTime column at the same time, as that
        will get deployed later
    -   Hence, we allow users to define certain columns as "updateTime
        columns", such that the tool will automatically set this value
        to the current time whenever the row is inserted or updated
    -   To do this, you would add the following metadata tag at the
        start of your CSV file: (the value is singular: only 1 column
        can be denoted as such)

//// METADATA updateTimeColumn="col1" ...

-   In terms of the reverse-engineering:
    -   The arg defined in the reverse engineering is to specify which
        columns you expect to be used as such, as teams may have certain
        conventions, e.g. timeUpdated, updTmstmp, ...
    -   The reveng arg is plural in case you happen to have multiple
        such columns


### Generating the data as Insert statements

If you can't or don't want to maintain your static data as CSVs, and
want to keep it as inserts, you can use Aqua to do this (i.e. Tools -\>
Export Data), or define these files manually, or whatever other method
that you have


## 6) Execute a deployment to your blank schema successfully

Use the deployment command as mentioned earlier to deploy your
newly-created files into the test schema you setup in step 2 earlier
**including adding the -onboardMode flag to help w/ the onboarding**.
The point of the deployment is so we can verify that the scripts are
valid.

Please perform a successful deployment before proceeding to the next step

```
%OBEVO_HOME%\bin\deploy.bat DEPLOY -sourcePath src/main/database -env test -onboardingMode
```

In Obevo, we strove to make the reverse-engineering as smooth as
possible, so hopefully it will be successful on the first try. But for
various reasons, it may not. Hence, you should iteratively deploy,
review the logs, fix problems, and redeploy until all issues are fixed.


### Explaining the -onboardingMode flag

As the deployment described here will be an iterative process to find
and then resolve issues, it becomes important to easily identify
exceptions. The onboarding mode helps via the following:

1) Any files that cause exceptions will get moved into either the
/exceptions or /dependentOnExceptions folders, and will have a separate
".exceptions" file created containing the exceptions you have.

-   /exceptions gets created for normal cases.
-   /dependentOnExceptions is created if we detect that the exception
    was caused due to an earlier exception on another object (i.e.
    something already in /exceptions

2) Upon correction of SQL files and subsequent rerun, files will get
moved back to the home folder


### Onboarding workflow

Your workflow then specifically becomes the following:

-   1) Run a deployment using -onboardingMode with your reverse-engineered
    DDLs
-   2) Fix the issues you see in the objects stored in /exceptions, whether
    by fixing SQLs (see notes below), fixing the DB environment itself, or
    deleting the file and choosing not to maintain it.
-   (leave the "dependentOnExceptions" alone for now)
-   3) Rerun the deployment w/ the -onboardingMode command. You should
    expect the /exceptions objects you just fixed to be successfully
    deployed. This means the objects in /dependentOnExceptions now have a
    chance to succeed. If they do, they will get moved to the regular
    folder. If not, they will get moved to the /exceptions folder
-   4) Go back to step 2 and repeat steps 2-4 until there are no more
    exceptions during your run
-   5) Do a final full-deploy (i.e. cleanEnvironment, followed by deploy) to
    help ensure that the SQLs are actually valid and compatible w/ all
    environments


### Common problems encountered + resolution steps

-   Objects may refer to other non-existent objects (e.g. views or
    stored procedures referring to tables that no longer exist). This
    can particularly happen for long-lived systems
    -   A suggestion here is to do a grep on the log file of the
        deployment that you tried for the SQL error that indicates a
        reference to an object that doesn't exist (e.g. for Sybase, the
        message is "nested exception is
        com.sybase.jdbc3.jdbc.SybSQLException: ArchivingCandidate not
        found. Specify owner.objectname or use sp*help to check whether
        the object exists (sp*help may produce lots of output)". Hence,
        you can try grepping for "not found"
    -   Once you get that list, send it out to your team (possibly w/
        the object that referred to it) to see if these objects are
        actually needed. Likely, you should be able to delete these
        objects (definitely delete them from your source code, and drop
        them in production prior to onboarding)
-   Views/stored procedures/triggers that are referring to objects in
    other schemas. Depending on your situation, you may either:
    -   need to tokenize the schema name in case it is different on your
        dev env vs. production (e.g. account*master*dev vs.
        accountmaster)
    -   see that the dependent object exists in prod but not dev. In
        that case, look to get that schema cleaned up (potentially via
        Obevo? You can do that as a separate project)
-   Circular dependencies in stored procedures that the default
    algorithm could not detect (maybe due to comments in your files). In
    these cases, add the includeDependencies/excludeDependencies as
    needed (see earlier parts in this doc for reference)
-   (For Sybase ASE) The stored procedure may have been relying on a
    pre-existing temp table (i.e. the client app creates the temp table,
    populates the data, then calls the SP that uses the temp table).
    -   In this case, you must create the temp table before creating the
        SP. Though the temp table won't exist outside the connection,
        the SP will remember the name and structure, and thus expect it
        to be created when called
    -   To handle this case, just add the "create temp table"
        statement in your SP file prior to the "create procedure"
        statement, and then add a "drop temp table" statement after
        the actual SP sql (all within the same file)
        -   See the example below for reference


```
create table #MyTemp (
    FieldA varchar(32),
    FieldB int
)
GO
create proc SpWithTemp (@MaxCount int) as
begin
    select * from #MyTemp

    // Do whatever processing you need
end
GO
drop table #MyTemp /* cleanup */
GO
```

-   For the staticData loads, a couple common issues may arise:
    -   If the table does not have a primary key, then the CSV mode will
        not work, as mentioned earlier in the docs. Either add the PK to
        the table, or switch to the delete/insert mode
    -   If your data includes a backslash (i.e. ), then you need to
        convert it to escape it w/ another quote (i.e. \\\\ ). This is
        because of a bug w/ the underlying CSV reader implementation
        [OpenCSV](http://opencsv.sourceforge.net/) (which
        otherwise is truly an excellent library!)

If you run into any other kinds of problems that you cannot seem to
debug, feel free to reach out to the Obevo team


## 7) Marking your files as 'already deployed' in production

Once you've gotten your DDLs into a good shape (i.e. doing the reverse
engineering, deploying your code against the blank schema, then
comparing that deployed schema against prod via Aqua to verify
correctness), then essentially, you can assume that your prod db have
been deployed via obevo

But - the audit table does not exist yet in production. You will have to
seed it w/ the audit trail such that Obevo can deploy any new changes
going forward

To do this, run the INIT command

```
%OBEVO_HOME%\bin\deploy.bat INIT -sourcePath src/main/database
```

Do the same for the rest of your environments (e.g. qa, uat) if needed

In case you have some ddls in uat but not production, the suggestion is
to:

-   first create baseline DDLs based off prod
-   then run init-env for prod
-   then add your migration script in your source code for your other
    envs, e.g. uat
-   then run init-env for the other env, e.g. uat
-   repeat if you have yet another layer of changes (e.g. dev or qa)


8) Complete the Adoption
------------------------

The above steps are easy - doing the final "production" touches is the
harder part, IMO. The time and effort to do this would vary depending on
your team situation


### Apply this to your other environments


#### UATs

You likely will have a number of dev/qa/uat environments already up and
running. We'd need to onboard these to Obevo too, and it is possible
that these environments are not in sync with production (hence, they are
out of sync with the db scripts you just created)

You have 2 options to work around this:

1) Wipe away the UAT environment and recreate (whether via devsync or
via Obevo -cleanFirst invocation)

This is the easy option if you don't have any ongoing UATs or can
easily start over

2) Manually compare and apply any changes to the UAT environment, and
then apply the "INIT" command mentioned in step 7

In the case that you cannot just start afresh, you may need to do manual
reconciliations and comparisons to get your UAT in line. This is not
that great an option, but depending on your team's situation, it may be
needed

To compare the schemas, use any DB management tool of your choice.
[SchemaCrawler](http://sualeh.github.io/SchemaCrawler/),
which is used as a library in Obevo, also has comparison functionality.

To compare the static data, see the following subsection...


#### Comparing the Data across different databases

When defining your static data files, it may help to do comparisons
between different databases (e.g. prod vs. uat, your deployed code vs.
prod) in case you are starting from a very unmanaged state (i.e. no
static data files checked in, not sure of the states your various dbs
are in)

To do that - run the following command. It will let you do compare a
list of tables that you define against the dbs that you define (you can
define any number of 2-way compares). The tool will output the
differences in Excel files (a summary report defining the overall
differences, plus detailed reports for cases where content exists in
either side of a comparison, but there are differences)

```
j:\cc\obevo\bin\deploy.bat DBDATACOMPARE -configFile H:\mydir\costarRepoComparisonConfig.properties -outputDir H:\mycomparisons\
```

Config File Example:
[dbComparisonConfigExample.properties](files/dbComparisonConfigExample.properties)

Format:

-   tables.include -- tables you want to check (you must define this)
-   tables.exclude -- tables you don't want to check (you can leave
    blank if you choose -- this is more of a convenience in case you
    were incrementally working on the comparison and wanted )
-   comparisons -- define the 2-way comparisons that you want to do.
    E.g. the string â€œdev1,uat;uat,prod1;dev1,prod2â€? will do the
    following comparisons:
    -   dev1 vs. uat
    -   uat vs. prod1
    -   dev1 vs. prod2
    -   Then you have an entry for each of the data sources that you
        define, and you must define the
        schema/url/username/password/driverClass for each


### Cleanup old/unmaintained objects as you see fit

-   Note - we ultimately are planning on a utility GITHUB\#1 to cleanup
    any objects that are in the environment and not in the source code.
    Till then, you can try the following manual cleanup options.
-   Removing backup tables from production (e.g. Transaction*bkup,
    Transaction*20130101)
    -   The suggestion is that you do not include these tables into your
        code base - delete them before you do the INIT command (if you
        already did the INIT command, you can just run it again after
        your changes). Drop these manually from production (I'd do this
        manually as the tool currently does not support dropping
        tables - better safe than sorry from a tooling perspective!
        though we will eventually support this)
-   Removing unused views/sps
    -   In this case, I "would" keep these in your Obevo codebase, as
        after your init command, you can then delete these in a
        controlled manner. (I'm less worried about the risk of dropping
        stored procedures, as you can easily add them back)


### See rest of recommended steps...

Follow the rest of the guidance on "More Recommended Steps" in [Full
Onboarding Guide for New Systems](new-onboarding-guide.html)
