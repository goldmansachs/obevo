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

# Rebaselining CHANGE entries in your table files

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

The key Obevo rule for handling incremental object types (e.g. tables
with //// CHANGE) is to never modify/delete an already deployed change.
There are exceptional cases where you may want to do this for the
purposes of code cleanup. This page describes how to do those cleanups.

### Use Case Details

For table files and changes - as Obevo requires that all new changes are
additions to the file, this file may end up being pretty long (see the
section below (to be written) on the justifications for this).

At some point, your team may want to "rebaseline" your table change
file, i.e. get rid of all the old individual changes in the file and
replace the content w/ the latest version of the table, so that any
changes going forward (and all people reading the db files) can look at
the cleaner latest view instead.


### High Level Steps To Execute

To do this for a particular table file, you must:

1.  Get the list of change names currently in your file
2.  Create a new //// CHANGE entry, with a new change name and the
    additional attribute "baselinedChanges=chng1,chng2,chng3" with
    your change names comma-separted
3.  Add your new baselined sql under this change and delete the rest


### Example

Note - you can also check out [the example]($%7Bsource.webroot%7D/obevo-db-impls/obevo-db-scenario-tests/src/test/resources/scenariotests/baseline-scenario)
in the source code - the snippets below are similar examples. The
different step folders represent the evolution of the table

Step 1 - let's say that you start w/ a table like so:

```
//// CHANGE name=chng1
CREATE TABLE TABLE_A (
    A_ID INT NOT NULL,
    B_ID INT NULL
)
GO

//// CHANGE FK name=fkChange
ALTER TABLE TABLE_A ADD FOREIGN KEY (B_ID) REFERENCES TABLE_B(B_ID)
GO

//// CHANGE name=index
CREATE INDEX TABLE_A_IND1 ON TABLE_A (A_ID, B_ID)

//// CHANGE name=chng2
ALTER TABLE TABLE_A ADD COLUMN C_ID INT NULL
GO

//// CHANGE name=chng3
ALTER TABLE TABLE_A ADD COLUMN D_ID INT NULL
GO
```

And let's say that you want to rebaseline this to look like the latest
version, e.g. with the columns C and D already added:

```
CREATE TABLE TABLE_A (
    A_ID INT NOT NULL,
    B_ID INT NULL,
    C_ID INT NULL,
    D_ID INT NULL
)
GO

ALTER TABLE TABLE_A ADD FOREIGN KEY (B_ID) REFERENCES TABLE_B(B_ID)
GO

CREATE INDEX TABLE_A_IND1 ON TABLE_A (A_ID, B_ID)
GO
```

Step 2 - collect the changes to rebaseline: Note that the names of the
changes are chng1, chng2, chng3, index - we want to combine these into
one. (the foreign key needs to be separate so that it can be deployed
separately due to the ordering algorithm.

The resulting file would look as follows:

```
//// CHANGE name=rebaselined baselinedChanges=chng1,chng2,chng3,index
CREATE TABLE TABLE_A (
    A_ID INT NOT NULL,
    B_ID INT NULL,
    C_ID INT NULL,
    D_ID INT NULL
)
GO

CREATE INDEX TABLE_A_IND1 ON TABLE_A (A_ID, B_ID)
GO

//// CHANGE FK name=fkChange
ALTER TABLE TABLE_A ADD FOREIGN KEY (B_ID) REFERENCES TABLE_B(B_ID)
GO
```

Step 3 - Perform a deployment that includes this file change. The
deployment will be a no-op for the physical table, but the audit table
will reflect the changes in your baseline

Step 4 - Once you've done a deploy to all your environments and thus
removed the original change entries in your audit table, you can safely
delete the baselinedChanges attribute, e.g.

```
//// CHANGE name=rebaselined baselinedChanges=chng1,chng2,chng3,index
CREATE TABLE TABLE_A (
    A_ID INT NOT NULL,
    B_ID INT NULL,
    C_ID INT NULL,
    D_ID INT NULL
)
GO

CREATE INDEX TABLE_A_IND1 ON TABLE_A (A_ID, B_ID)
GO

//// CHANGE FK name=fkChange
ALTER TABLE TABLE_A ADD FOREIGN KEY (B_ID) REFERENCES TABLE_B(B_ID)
GO
```


### Potential Improvements on the existing approach

Now that you have done this - great, now you have a much smaller
baselined file!

However, one issue we'd like to improve in this process: how did you
come up with that rebaselined sql?

-   Did you re-query it from the database, or figure it out yourself?
-   How do you truly know that your rebaselined SQL matches the CHANGEs
    that you used to deploy this in the first place?

There are two potential things to try for here:

1.  A nice way to generate the rebaselined SQL (or to derive it from
    another authoritative source...)
2.  A programmatic way to compare the original changes vs. the baseline

The next section goes through this


### How to update your Obevo db file if you have to do a manual SQL change without a release (not that you should try this)

don't do it often! there are alternatives to doing it manually

section to be written

