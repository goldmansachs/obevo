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

# Overview

<b><font color="red">NOTE:</font></b> We have published an [article on InfoQ](https://www.infoq.com/articles/Obevo-Introduction)
that gives an overview of Obevo.

Please see that article in place of this page.


## Preface

Before we start, we'd like to mention that one of the keys to this tool
is to make your db schema maintenance easy, whether for large or small
DBs, for new or existing.

Particuarly existing systems - if you haven't managed your DB schemas
in source code before, it usually would be painful to get that under
source control, and thus Obevo has functionality to make it as easy as
possible (and continually looks to improve)

Obevo was built to handle such cases and vetted by its usage and
onboarding into long-lived existing systems. See
[here](https://github.com/goldmansachs/obevo-kata/blob/master/internal/doc/kata2-reverse-engineering.md)
for a demo project to show this. In our opinion, the resulting code is
rather maintainable (considering the hundreds of db objects!) compared
to what may otherwise result if the system was maintained only using
incremental update scripts.


## Overview of how "most database deployment tools" work

Some Background - the key fact is that a database is maintained over
time with *incremental* changes

-   i.e. you cannot redefine a DB table by simply editing the existing
    definition
-   This is because the only way to perform such changes in SQL is by
    dropping and re-adding the table, and this is not possible as tables
    have data in it
-   Instead, you must define ALTER statements on top of the existing
    tables to make changes

Hence, most DB deployment tools operate in this manner:

-   Developers define the DB changes (i.e some create/alter command) in
    source control, with the changes identifiable by a certain
    convention (one example is to do one change per file, with the*name*
    of the change being the file name)
-   The tool is executed against a target database. The changes that get
    applied are stored in a table in the target database (keyed off the
    *name* of the change)
-   Upon subsequent calls to the deployment tool, the tool will use this
    audit trail to know which changes should get deployed (i.e. "set of
    changes defined in your code" minus "set of changes in the audit
    db" = "set of changes to deploy")
    -   This implies that once a change is deployed to a DB, that the
        change should no longer be modified in source control
    -   Many db tools will do a check to ensure that this does not
        happen (i.e. store a hash of the change in the DB upon
        deployment, and compare against that hash upon subsequent
        deployments)

Alternative considered - tool generates the diffs:

-   An approach that people may think of is to just maintain the latest
    version of the schema in your source, and have the tool figure out
    the differences
-   This would be ideal, but in practice can be tricky (even with vendor
    products) and would always need some human intervention. Hence, we
    cut out this part entirely


## Why Obevo

Viewing all DB migrations as incremental changes is simple to implement,
but does not lead to a maintainable code base in the long term

-   Imagine all the change files that will start piling up in your code
    base
-   And for rerunnable objects like stored procedures or views, it seems
    redundant to keep the old change files around. (yes, stored
    procedures are "evil", but many systems still use them and we need
    to continue to maintain them)
-   And having to manually define the order of your changes, e.g. for
    inter-dependent stored procedures. (Imagine if you had to do this
    for Java, i.e. specifying the compile order of your classes based on
    their dependencies and relationships)

Obevo looks to save some of the work around this by:

-   Encouraging a one-file-for-db-object convention as to make db object
    maintenance similar to your code (e.g. the one-file-per-class
    convention in Java)
-   Saving some work to generate alter commands where needed (e.g. for
    views and stored procedures)
-   Using an algorithm to automatically calculate the change deployment
    order (both for incremental changes and rerunnable changes)
-   Among other things

The user guide details the features that come with Obevo.


## How Obevo Calculates and Applies Changes

Read the 'how "most database deployment tools" work' section and
then see the [Design Walkthrough](design-walkthrough.html)
