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
# Permission Management

This page will describe the special functionalities for managing grants in your database.

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

## Permission Schemes in system-config.xml
You can specify general permissions to use for all your objects using the &lt;permissions&gt; element in
system-config.xml. See this example:

```
<dbSystemConfig>
    <schemas>
        ...
    </schemas>
    <environments>
        ...
    </environments>
    <permissions>
        <permission scheme="TABLE">  <!-- scheme name corresponding to object type "table" -->
            <grant groups="RO_GROUP1,RO_GROUP2" privileges="SELECT" />
            <grant groups="RW_GROUP" privileges="UPDATE,SELECT" />
            <grant users="SUPERUSER" privileges="INSERT,SELECT,UPDATE,DELETE" />
        </permission>
        <permission scheme="TABLE_RO">  <!-- custom scheme name that objects can opt into -->
            <grant groups="RO_GROUP3,RO_GROUP4" privileges="SELECT" />
        </permission>
        <permission scheme="VIEW">  <!-- scheme name corresponding to object type "view" -->
            <grant users="SUPERUSER" groups="RO_GROUP1,RO_GROUP2,RW_GROUP" privileges="SELECT" />
        </permission>
        <permission scheme="SP">  <!-- scheme name corresponding to object type "sp" -->
            <grant groups="RO_GROUP1,RO_GROUP2,RW_GROUP" privileges="EXECUTE" />
        </permission>
    </permissions>
</dbSystemConfig>
```

Element Descriptions:
* &lt;permission&gt; defines a particular scheme
 * Each DB object will have a particular scheme
 * By default, the scheme name will be the object type, e.g. TABLE, SP, VIEW, SEQUENCE, FUNCTION
 * But we can override using a metadata annotation, e.g. //// METADATA permissionScheme="TABLE_RO"
* Then specify a &lt;grant&gt; entry for each kind of user/group and privilege combo that you want to add.

So the above will translate to the following grants:
* Note: the _exact grant statements may differ per DBMS type_. e.g. some platforms do not require the GROUP or USER keyword to be specified

```
// for TableA on its default permission scheme TABLE
GRANT SELECT ON TableA TO GROUP RO_GROUP1
GRANT SELECT ON TableA TO GROUP RO_GROUP2
GRANT UPDATE, SELECT ON TableA TO GROUP RW_GROUP
GRANT INSERT, SELECT, UPDATE, DELETE ON TableA TO USER SUPERUSER

// for TableB that declares //// METADATA permissionScheme="TABLE_RO" in its file
GRANT SELECT ON TableA TO GROUP RO_GROUP3
GRANT SELECT ON TableA TO GROUP RO_GROUP4

// for ViewA on its default permission scheme VIEW
GRANT SELECT ON ViewA TO USER SUPERUSER
GRANT SELECT ON ViewA TO GROUP RO_GROUP1
GRANT SELECT ON ViewA TO GROUP RO_GROUP2
GRANT SELECT ON ViewA TO GROUP RW_GROUP
```

Note that:
* The values here can be tokenized (i.e. if you specify the tokens in system-config.xml, they can be replaced here as well

## Grant Execution Behavior

As of today, these grants are only applied when an object is created, i.e.

* When a table is initially created. This is detected either by the tool finding "CREATE TABLE" in the SQL or by defining a **_//// CHANGE applyGrants=true_** annotation on the change in question
* When a rerunnable object is created/recreated

Note that if you change the &lt;permissions&gt; element itself, the tool will not automatically apply
that grant on existing objects; it would only apply on newly created or modified objects going forward.

If the grant fails (e.g. due to the permission entries being misconfigured), then the deploy will continue to success,
but a warning will be logged. The grant would have to be executed separately (e.g. using a /migration script).

In the future (see [Github Issue 3](https://github.com/goldmansachs/obevo/issues/3)), we will support having &lt;permissions&gt;
changes applied on existing objects. This would also be the way to fix any failed grants.
