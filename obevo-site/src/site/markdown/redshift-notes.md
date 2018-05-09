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
# Amazon Redshift Usage Notes

DB Type acronym for configurations: REDSHIFT

## JDBC Driver and Distribution Setup

Amazon recommends using the Redshift JDBC Driver for connecting to the database.

Due to the license for this driver (see [here](https://s3.amazonaws.com/redshift-downloads/drivers/Amazon+Redshift+JDBC+Driver+License+Agreement.pdf)
and the [note at the end here](https://docs.aws.amazon.com/redshift/latest/mgmt/configure-jdbc-connection-with-maven.html)),
Obevo cannot include this driver in its distributions.

Instead:

1. Please [obtain the jar from Amazon](https://docs.aws.amazon.com/redshift/latest/mgmt/configure-jdbc-connection.html#download-jdbc-driver)
2. Add it to your copy of the Obevo distribution using the extension points available (see the [Setup Page](setup.html) for more information).


## PostgreSQL Compatibility

The Redshift platform is based off PostgreSQL, but with a few differences per Amazon ([link](https://docs.aws.amazon.com/redshift/latest/dg/c_redshift-and-postgres-sql.html)).

Though Amazon recommends using the Redshift JDBC Driver (thus requiring the JDBC URL start with jdbc:redshift://),
it also has some [compatibility with PostgreSQL](https://docs.aws.amazon.com/redshift/latest/dg/c_redshift-postgres-jdbc.html),
notably with PostgreSQL JDBC Driver versions 8.x, but not 9.x.

Hence, it is possible to run Obevo against a Redshift instance as if it were PostgreSQL, e.g.

* Specifying the type as POSTGRESQL in configs
* Using the JDBC url with the prefix jdbc:postgresql://
* Using the PostgreSQL JDBC Driver compatible with 8.x (but not 9.x)

## Sequences not supported

Unlike PostgreSQL, Redshift does not support the sequence object type.


## Use "late binding views" to allow views to be recreated without affecting dependent objects

Let's take the use case of a view dependending on a table, and the table dropping a column. By the simplest syntax for
creating a view, the table column drop will fail as Redshift will restrict such actions if a view depends on a table,
even if the table does not actually refer to the column in the table. The command to drop the column could succeed if
the CASCADE keyword was added, but that would force the view to be dropped as well, not a desirable result.

To work through this, add the syntax "WITH NO SCHEMA BINDING" when creating the views, per the example below:

```
CREATE VIEW my_view AS SELECT col1 FROM dbdeploy01.my_table with no schema binding;
```

This allows late binding of the views, such that views can still work if they did not refer to the dropped column.

See the [drop syntax](https://docs.aws.amazon.com/redshift/latest/dg/r_DROP_VIEW.html) and [create syntax](https://docs.aws.amazon.com/redshift/latest/dg/r_CREATE_VIEW.html)
docs for more information.

To see this use case in action, try out the SQL snippet below in an interactive SQL session connecting to Redshift using
the DB query tool of your choice.

```
-- set the schema
SET search_path TO dbdeploy01;

-- drop the objects to prep the test
drop view my_view;
drop view my_view_star;
drop view my_view_col2;
drop table my_table;

-- create and populate the table
CREATE TABLE my_table ( col1  INT2 NOT NULL, col2 INT2 NOT NULL );
insert into my_table values (1, 2);

-- create the view w/ explicit col1 references
CREATE VIEW my_view AS SELECT col1 FROM dbdeploy01.my_table with no schema binding;
-- create the view w/ select *
CREATE VIEW my_view_star AS SELECT * FROM dbdeploy01.my_table with no schema binding;
-- create the view referring col2 as well
CREATE VIEW my_view_col2 AS SELECT col1, col2 FROM dbdeploy01.my_table with no schema binding;

-- note that the view creation needs explicit mention of the schema, as indicated in Redshift docs

-- all selects should work fine
SELECT * FROM my_view;
SELECT * FROM my_view_star;  -- shows col1 and col2
SELECT * FROM my_view_col2;

-- now drop the column
ALTER TABLE my_table DROP COLUMN col2;

-- this select works fine, only shows col1
SELECT * FROM my_view;
-- this select works fine, only shows col1
SELECT * FROM my_view_star;
-- this will fail
SELECT * FROM my_view_col2;
```
