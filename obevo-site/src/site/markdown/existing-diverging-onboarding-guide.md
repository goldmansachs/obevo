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

# Existing Diverging-Shard Onboarding Guide

If you are on this page, then you have the situation w/ diverging-shards
in production described in the [Onboarding Guide](onboarding-guide.html)

## Initial Onboarding Steps

The onboarding steps for such systems are similar to that for regular
existing systems with single matching shards as highlighted in
the[Existing System Onboarding Guide](existing-onboarding-guide.html),
but with a few changes that we highlight now.

We will abbreviate Existing System Onboarding Guide as ESOG for brevity
in the steps below:

1.  ESOG Step 1: Do setup as usual
2.  ESOG Step 2: Create a blank schema for each shard; it will make your
    development and testing effort easier
3.  ESOG Step 3-4: Perform the reverse-engineering steps for each shard
    separately
4.  New Step: Leverage the [DB Merge Tool - Step 1](db-merge-tool.html)
    to give a merged view of your DDL code. This serves two purposes:
    -   \#1: This format will be deployable to each of the instances you
        define while minimizing code duplication
    -   \#2: Gives a quick view on how much your schemas diverge (easy
        to tell from the file system)
5.  New Step: Consider how much you want to correct in production before
    completing the onboarding steps, i.e. if you want to execute SQLs
    manually to merge the schemas. You can leverage [DB Merge Tool - Step 2](db-merge-tool.html) to generate candidate SQLs for you; but
    this only works for tables; won't work for views, stored
    procedures, and so on
    -   If you choose not to merge before onboarding, then proceed w/
        the next steps. You can later on do the alters in Obevo itself
        to merge the schemas over time in a controlled fashion.
    -   If you choose to merge before onboarding, then apply the changes
        to production and rerun the reverse engineering steps from the
        top
6.  ESOG Step 5: Define your system-config.xml file and specify all the
    environments you had in Step 2. The environment names should match
    the names you gave in the Merge Tool Step 1, at least in prefix, so
    that the environment filters can work appropriately.
7.  ESOG Step 6: Execute the deployments for each shard
8.  ESOG Step 7: Seed the audit table for each shard
9.  ESOG Step 8: As you do the final "adoption completion", aim to do
    this in as unified a manner across your shards as possible. Many of
    the steps above were done separately for each shard, so this is the
    point at which you can start thinking in a unified manner


## Suggestions on maintaining/merging your objects over time

For existing tables, attempt to merge them over time and then
re-baseline:

-   Try various diff tools, whether in tooling like Aqua Data Studio or
    [DB Merge Tool Step 2](db-merge-tool.html) to generate the SQLs to
    do the merge, then code those SQLs into your DB code base
-   Whenever the tables have a merged definition in production, it is
    likely that your source code still has a file-per-shard as each
    shard may have required a different path to merge the table. See the
    [documentation on rebaselining](advanced-use-cases.html#Rebaselining_CHANGE_entries_in_your_table_files)
    for how to to clean up those files to delete the various copies and
    maintain only a single copy.

For new tables: do not repeat the mistakes of the past! :)

For views/stored procedures/other rerunnable objects: These are going to
be harder to merge, as these are single atomic units of code, as opposed
to a table that is built over time using multiple changes. For these
cases, you should try to merge them as you would code, e.g.:

1.  either logic changes and conditional statements to keep the same
    behavior
2.  bite the bullet and keep separate copies going forward (this may
    just be unavoidable)
