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

# Phased Deployments

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

This section describes the phased deployments feature, a.k.a. changesets
feature, that can be used if you need to orchestrate a release in
multiple steps. An example use case is for a long-running deployment
(e.g. creating indexes) that you would like to run separately after the
rest of your regular DDLs are run.

Let's take a regular deployment going from version 1 in the database to
version 2 in your source code. Say that the delta between them is 5
changes: c1, c2, ... c5. Normally, the deployment will execute all 5
changes in one shot, and this is the recommendation.

However, in certain cases, teams may need to execute their deployment in
phases. The prime example is for index creation; teams may want to
deploy their structural DDL changes for their main deployment so that
their application can move to a new release, but not deploy their index
changes as those would take much longer (hours potentially depending on
the table size) and the application could still run without them.
Instead, these index changes could be deployed separately (say if a team
prefers deploying table changes on weekdays and indices on weekends)

We stress that this is not the ideal case; preferably, all DDL changes
can be done in one shot, and even if some changes take longer, if you
can find downtime on a weekend you can do the full release then.
However, as practicality dictates that some teams will need to do a
phased approach as described above, we will provide this functionality
in Obevo.


## Declaring Changesets in your code

Add the changeset annotation to the CHANGE that you want to deploy
separately. e.g.

```
[mytable1.ddl]
//// CHANGE name=c1
abc

//// CHANGE name=c2
abc

//// CHANGE name=c3 changeset=set1
abc

//// CHANGE name=c4 changeset=set1
abc

//// CHANGE name=c5 changeset=set2
abc


[mytable2.ddl]
//// CHANGE name=d1
abc

//// CHANGE name=d2
abc

//// CHANGE name=d3 changeset=set2
abc

//// CHANGE name=d4 changeset=set3
abc
```

Given this example:

-   changeset "set1" will have mytable1.c3, mytable1,c4
-   changeset "set2" will have mytable1.c5, mytable2,d3
-   changeset "set3" will have mytable2.d4
-   The rest will be in the default changeset, i.e. mytable1.c1,
    mytable1,c2, mytable2.d1, mytable2.d2


## Deploying changesets in phases or all-together

Change calculation with these changeset attributes is calculated as
follows:

1.  The normal change calculation takes place as usual.
2.  The change differences found are then filtered based on the
    changeset parameters passed in, as described below
3.  Note that if a change is already deployed and we pass in a changeset
    argument that corresponds to that change, it will ***NOT*** redeploy
    that change. Per the previous point, the changeset argument only
    serves as a filter on top of the regular changeset calculation

Onto the argument types:

-   Without providing any additiona parameters, only the default
    changeset will be deployed, i.e. any changes that have a
    "changeset" attribute specified will get filtered out.
-   To deploy a specific changeset, use the "-changesets" parameter,
    with the changeset names passed in as a comma-separated list
-   To deploy all changesets, use the "-allChangesets" parameter (no
    extra value needed)


## Deploy Command Example

For a particular release, the deploy commands can go as follows:

1.  Do a regular deployment to get the standard changes deployed:
    deploy.sh DEPLOY -sourcePath /mydir
2.  Then deploy your other changesets on your own schedule: deploy.sh
    DEPLOY -sourcePath /mydir -changesets set1
3.  Finally, deploy using -allChangesets to ensure that no changes have
    been missed: deploy.sh DEPLOY -sourcePath /mydir -allChangesets


## -allChangesets enabled for testing

For regular deploys, the default is to not deploy changesets as this
optimized for the production use case.

However, for testing, we'd encourage deploying all changesets to ensure
that no changes get missed out. Hence, this is enabled by default for
unit test deployments and the maven "test" goal.
