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



Download driver here: https://docs.aws.amazon.com/redshift/latest/mgmt/configure-jdbc-connection.html#download-jdbc-driver
https://docs.aws.amazon.com/redshift/latest/mgmt/configure-jdbc-connection.html

https://s3.amazonaws.com/redshift-downloads/drivers/Amazon+Redshift+JDBC+Driver+License+Agreement.pdf

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
