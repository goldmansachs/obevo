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

# DB Project Best Practices

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

### Logical schema name should match production or otherwise be environment-agnostic

The logical schema name should be representative of your schema across
environments. When onboarding an existing schema, this typically means
to match it to production (though not always)

In observing some teams onboarding, we see a habit of naming the logical
schema in the \<schemas\> section in the system-config.xml file as the
name of the dev schema that is being deployed to. This is not correct -
it is preferred to match it to prod, and then to leverage the
dbSchemaPrefix/Suffix or schemaOverrides to map it to the dev schema.

We will improve the reverse-engineering support in the future to also
generate a template system-config.xml file for you as to avoid confusion
on this in the future.


### Table //// CHANGEs should be as fine-grained as reasonable to ease recoverability

See the "Transactionality Considerations" section in the [Error Handling page](error-handling.html) for details.


### Set up a dedicated test schema to run a deploy against in your continuous builds

If you commit code, you should have automated tests for it; that applies
not just to your application code, but to your database code.

At minimum, you should be able to validate the correctness of the SQL
you deployed. The best way to do this is to simply deploy your schema to
an environment! This requires an actual environment to test against,
esp. for DBMS platforms whose specific SQL dialects and storage settings
cannot be tested against an in-memory double.


### Prefer not including references to the same schema in your DDLs

e.g. If your objects are for schema MYSCHEMA1, then do not qualify your object references with the schema, e.g.

```
CREATE TABLE MYSCHEMA1.TAB1 (  -- not preferred

CREATE TABLE TAB1 (  -- preferred
```

This is so that you have flexibility in deploying your objects to differently named schemas (whether suffixed
differently like MYSCHEMA1_QA or to reuse objects in other projects).

The reverse-engineering steps should already take care of removing the object qualifiers from the same schema. But if
somehow the qualifiers are not removed, then please do so manually.


### Tokenize references to other schemas in your objects

Take a view defined in MYSCHEMA1 that refers to another schema OTHERSCHEMA1, like so:

```
CREATE VIEW MYVIEW1 AS SELECT * FROM OTHERSCHEMA1.OTHER_TABLE
```

Per the point above, we'd like to have flexibility in the schema to deploy to.

You can define tokens yourself (see the [tokenization doc](environment-management.html)), or you can use the built-in
tokens for the physical schema name. For example, assuming you've defined the logical schema name as THATSCHEMA in your
system-config.xml that maps to the OTHERSCHEMA1 physical schema, use the token as below
(\<logicalSchema\>_schemaSuffixed). Note that this token includes the dot separators (i.e. to handle DBMS types that
sometimes require multiple dots as separators).

```
CREATE VIEW MYVIEW1 AS SELECT * FROM ${THATSCHEMA_schemaSuffixed}OTHER_TABLE
```


### Consider leveraging the in-memory DB translation feature for unit testing your DB access code

While unit test databases cannot test the exact SQL syntax that you define, they can prove useful for testing your
application code that needs to access the DB, e.g. your ORM layer or your data-access code. Hopefully your code is
agnostic to the underlying DBMS technology, esp. if you are using an ORM or ANSI SQL.

To help create an in-memory DB from your DDLs without having to resort to a separate copy just for the in-memory DB,
try out the [in-memory DB conversion feature](in-memory-db-testing.html).

* e.g. if you use Hibernate's schema generation from the POJOs, it would be a better idea to move to Obevo's instead
