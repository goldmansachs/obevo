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
# DB2 Notes

DB Type acronym for configurations: DB2

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

## DB2 Reorg Detection and Support

Obevo supports the handling of reorgs in 2 places:

* If any SQL is executed and returns the DB2 reorg-pending error (-668), Obevo will detect this and automatically fire off the reorg statement (you can disable the automatic firing of the reorg in system-config.xml; see section below)
* At the end of a deployment, Obevo will check if any tables are pending reorg using the query below, and if any are detected, it will show the results to you and fire the reorg (unless you've disabled the automatic firing of the reorg, in which case you'll just see the list of affected tables)

```
SELECT TABSCHEMA, TABNAME, NUM_REORG_REC_ALTERS, REORG_PENDING
FROM SYSIBMADM.ADMINTABINFO
WHERE TABSCHEMA IN ('schema1','schema2') AND REORG_PENDING = 'Y'
```

Both use cases need to be supported as it can be possible to modify a table that is pending reorg
without incurring the error (the error is only incurred if you try to use the data in it), and so you need a
check after the fact.


## DB2 Invalid Object Detection and Recompilation
Existing objects can become invalid if a dependent object is modified in certain ways; DB2 terms these as
invalid objects. Obevo can detect if [such objects exist after your deployment](http://www.ibm.com/support/knowledgecenter/SSEPGG_9.7.0/com.ibm.db2.luw.sql.ref.doc/doc/r0054588.html)
and [recompile them if possible](http://www.ibm.com/support/knowledgecenter/SSEPGG_9.7.0/com.ibm.db2.luw.sql.rtn.doc/doc/r0053626.html).

This behavioral is configurable - see section below.


## Configuring the DB2 post-deploy and reorg behaviors
The following parameters are available to configure:

|Property|Default Value|Description|
|---|---|---|
|reorgCheckEnabled|true|If true, will execute the query to find all tables that require reorg. This property does not determine whether the reorg is executed|
|autoReorgEnabled|true|If true, will execute a reorg automatically if detected during deployment or the post-deployment step. If reorgCheckEnabled==false, then no reorgs will run during the post-deploy step, regardless of the autoReorgEnabled value|
|invalidObjectCheckEnabled|true|If true, will execute the invalid-object check and recompilation in the post-deployment step.|


You have the choice to configure the behaviors above either at the dbSystemConfig or dbEnvironment levels, e.g.

```
<dbSystemConfig autoReorgEnabled="false" ...>   <!-- Set the default value here -->
    <!-- ... -->
    <environments>
        <dbEnvironment name="env1" ... />  <!-- inherits the default value from the top level -->
        <dbEnvironment name="env2" autoReorgEnabled="true" ... />  <!-- overrides the default value -->
```


## "Invalid operation: Lob is closed" Exception

If you receive this exception, you should add the progressiveStreaming=false value to your JDBC url, per below. More details [here](https://pdn.pega.com/support-articles/pega-718-installation-db2-fails-lob-closed-error)

```
jdbc:db2://localhost:50000/dbdeploy:progressiveStreaming=false;
```

We have only seen this in the DB2 Express environment so far. If you run into this in your own standard environment,
please raise an issue on Github.


## DB2 Error Message Handling

Error messages in DB2 will appear somewhat as follows:

```
Caused by: com.ibm.db2.jcc.b.SqlException: DB2 SQL error: SQLCODE: -750, SQLSTATE: 42986, SQLERRMC: null
```

If you see such an exception, first search via the SQLCODE for a hint of the problem.

For the example above, do a search like "db2 error code 750" to find the issue. This usually leads to the
[DB2 error code reference page](https://www.ibm.com/support/knowledgecenter/en/SSEPEK_10.0.0/codes/src/tpc/n750.html).

If the error message does not help you, then reach to your database administrators or contact the Obevo team.
