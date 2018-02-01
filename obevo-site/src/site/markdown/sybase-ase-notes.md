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
# Sybase ASE / SQL Server Notes

DB Type acronym for configurations: SYBASE_ASE

<!-- MACRO{toc|fromDepth=0|toDepth=1} -->

## Sybase stored procedures for quotes and temp tables

For teams that have a lot of Sybase stored procedures, you may hit the following two use cases:

### Supporting Quoted Identifiers

__The Problem__

In ANSI SQL, you use single-quotes to represent strings, not double-quotes. Double-quotes would tie
to identifiers in the DB (e.g. column names, tables names, in case they have spaces).

For Sybase, this is also true if you connect via JDBC, but for whatever reason, it is not true if you
connect via the isql command line - see [Sybase's doc for more info](http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc38151.1510/html/iqrefbb/CACIIHCI.htm).

Hence, if you deployed stored procedures via isql, and then try to re-deploy via isql or
Obevo in its default setting, you will get an error, as the SP will have double-quotes

__The Solution__

If you specify the metadata annotation //// METADATA DISABLE_QUOTED_IDENTIFIERS, then
Obevo will turn off the quotedidentifier option so that the double-quotes will be treated
as strings. It will then turn it back on for subsequent deployments.

Note that the Obevo reverse-engineering step mentioned earlier will automatically add this
annotation if it sees a double-quote in the text. (Though there is a chance that it is actually
intended as an identifier, the likely case is that it was intended as a string. You should try to recall if the
SP was originally deployed via isqlto help your decision)if you do the reverse-engineering steps above,

### Temp Tables as Input to Stored Procedures
In Costar, we've had some cases where a stored procedure required a temp table to be populated for
running the stored procedure. However, to create the SP, we need the temp table created in the first place

To do this, you can do this in the sp sql file itself, but just remember to add the temp table
beforehand and then to drop it, e.g.

```
create table #mytemp (a int, b int)
go
create proc PopulateSelectTradeTemps() as
...
select * from #mytemp
...
end
go
drop #mytemp
```

(not that we'd want to do something like this in the age of Java and Reladomo/Hibernate, but this is here in case you need it.)


## Interesting/unhelpful error messages when dealing w/ Sybase - see the FAQ

Compared to the other DBMS types supported by Obevo, Sybase has a lot of interesting error and error messages (or lack of helpful error messages).

Please see the [FAQ pages](faq.html) for more information.

## Duplicate index names allowed in Sybase but not in in-memory DBs

Sybase allows for indices not to have unique names given they refer to different tables. This is not supported by the in-memory databases.

To work around this, you can use a marker tag in system-config.xml as shown below. This will force
all indices in a schema to be prefixed with a table name in test mode.

```
<dbSystemConfig type="SYBASE_ASE">
    <schemas>
        <schema name="schema_name">
            <duplicateIndexNames>true</duplicateIndexNames>
            ...
```
