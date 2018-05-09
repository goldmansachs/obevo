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

# Onboarding Guide

Once you've familiarized yourself with Obevo via the documentation or
kata, how do you then apply this to your actual system (i.e. to apply to
your dev/qa/prod environments)?

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

## Choosing a Strategy for your System

Keep in mind the end goal of onboarding your schema, which is to be able
to:

1.  Deploy incremental changes to your uat/production environments
2.  Deploy a full schema to a blank environment for testing
3.  Maintain all DB objects in your source code and match all the shards
    in your Production environment. By shards, we mean if your
    production environment has multiple instances, e.g. a host per
    region

Depending on the characteristics of your system, you have different
routes to take to this end state.

See the table below for what you should take for your system.


|--------|-----------|--------|
|Strategy|Available Features|Applicability to your system|
|--------|-----------|--------|
|[New Systems](new-onboarding-guide.html)|All|1) If you have not deployed any DB objects to your Production environment, or you can wipe away your production database and start clean|
|[Existing Systems - Single/Matching Shards](existing-onboarding-guide.html)|All|1) If you have DB objects already in your Production environment that should not be touched when onboarding to Obevo<br/>2) All your production DB shards match in schema, or are easily reconcilable (* - see below for more)|
|[Existing Systems - Diverging Shards](existing-diverging-onboarding-guide.html)|All|1) If you have DB objects already in your Production environment that should not be touched when onboarding to Obevo<br/>2) You have multiple production DB shards that do not match easily, but you would like to maintain them as one code-base and possibly unify them over time. (* - see below for more)|
|[Legacy Systems (not recommended - only exceptional cases)](legacy-onboarding-guide.html)|\#1 - only to deploy to uat/prod environments|1) If you have DB objects already in your Production environment that should not be touched when onboarding to Obevo<br/>2) You have multiple production DB shards that do not match easily<br/>3) If you have a legacy system about to be retired for which you don't want/need the investment for the testing features, but need to get by with production deployments until the system is retired.|


## (*) Guidance on the Matching vs. Diverging Shards condition for Existing Systems

Clarifying the point on matching shards - for DB object maintenance,
ideally the following condition should hold:

-   ***Each DB object should have the same definition in all production
    DB shards***

Use cases where this is achieved:

1.  If you have 1 production shard (trivial)
2.  If you have multiple production shards that have always been
    maintained in sync, e.g. if you had proper and design over your
    database migrations.

However, some systems may not be in this ideal state for various
reasons, e.g. a long-lived system that was built and evolved before DB
SDLC practices came into vogue, with various functional system
enhancements causing the schemas to diverge.

If this matches your case, you have a few questions to ask:

1.  Is the same application code modifying all instances? Do you truly
    care about having a single canonical copy of DDLs across your
    production instances?
    -   If not, then simply maintain separate for each (i.e. a
        Single-shard Existing System Onboarding for each production
        shard.
2.  If so, then you should into the [Diverging Shards Onboarding Guide](existing-diverging-onboarding-guide.html), which gives you
    guidance on how to get your diverging shard schemas into source
    control, then to evolve/fix the divergent objects over time into a
    unified model (if you choose to merge them).
3.  Last-resort - if your system is being retired soon and you want to
    save the merge work at a cost of features, then use the [Legacy Onboarding Strategy](legacy-onboarding-guide.html)


## FAQ: Can I onboard only a subset of my existing DB objects initially, and then incrementally onboard more later?

Short Answer: No

Long Answer: No, because it will:

-   Save significant amounts of time long term (and possibly even short
    term) compared to incremental onboarding
-   Significantly reduce short-term and long-term risk over your
    database changes

It is our explicit design choice and recommendation (as opposed to a
technical limitation).

We recognize the difficulty of managing an existing production systems
and the aversion to change. Some reasons we have heard on wanting to
onboard objects incrementally:

-   1) Too many objects in production, a lot of effort to onboard them all
-   2) Many objects that we don't know about or that haven't been touched
    in a while; why bother with onboarding them
-   3) Multiple teams deploy to that schema; we want to onboard our changes
    without impacting them

We can address these points:


### 1) Handling large schemas

We have onboarded many ***LARGE*** schemas (i.e. reverse-engineer from
production and get a working copy deployed to a test schema

-   800 tables, 5000 SPs, 200 views in Sybase ASE
-   700 tables, 1000 SPs, 80 functions, 200 views in DB2
-   200 tables, 100 SPs, 250 user types (!), 1 DEFAULT in Sybase ASE
-   ... and so on
-   ... with a lot of complexity (interdependencies among SPs, from
    functions to tables back to functions, ...)

Onboarding each schemas has taken *<u>2 days in the worst-case</u>*.

We've invested a lot in our onboarding tooling to make it easier. In
fact, much of the time in this onboarding is really to clean up
"invalid objects" , i.e. objects that refer to no-longer-existing
objects. If your schema is clean, onboarding is no issue. And we've
enhanced our onboarding workflow to make it easier to see and resolve
issues.

Hence, regardless of the difficulty that you expect when onboarding, you
can be assured that we have seen many other difficult use cases in the
past and that our tooling will be up to the challenge. And if you have a
use case that our tool hasn't seen yet, you will have some eager folks
ready to improve the tool to solve your issue! (as we have done with
many others in the past)


### 2) Cost of not onboarding upfront

As mentioned in \#1, we make it easy to onboard your objects. But is
there any harm in not deploying all objects regardless? Yes, we can name
two

A) You have not changed these objects recently in the past...that
doesn't mean they won't change in the future

B) You many really only care about a single table or SP or view. But you
may find that it depends on another view or SP, which means you have to
redo the onboarding steps for those objects. And *those* may depend on
yet another view or SP or table, which means you have to redo the
onboarding steps...you get the idea.

It could end up much simpler and less costly to then do the full
onboarding.


### 3) Better for all owners of a schema to onboard than a single owner at a time

In some older systems, multiple teams may be performing deployments
against a single schema or want to maintain their objects separately. It
is understandable that one team may want to onboard without affecting
the other teams. However, this can lead to some complexities:

A) As mentioned in \#2B, you may not know upfront if your subset of
objects is truly independent, or if there is some dependency on another
group's objects, or vice versa.

B) Having visibility on all changes going into a schema would reduce
risks for all teams. Would teams not worry about any migrations from
other teams that would potentially affect their own? Onboarding all the
objects in that schema would at least make it easier to keep tabs on
what other teams are doing.

That said - teams still would have options on keeping their DDLs
separate if they choose (even if the deployment is ultimately done from
a single source). See the FAQ for more information


### Summary - go with onboarding a full schema

No tooling is perfect, especially for cleaning up tough situations like
long-lived schemas. But given our experience with this in the past and
our investment into the onboarding tooling, we strongly feel the full
schema onboarding is the way to go. Feel free to give us feedback if you
have any difficulties onboarding your use case.
