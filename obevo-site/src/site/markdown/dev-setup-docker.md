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


# Developer Setup for local Docker-enabled environments

Certain DBMS types (including DB2, SQL Server, and PostgreSQL) provide [Docker](https://www.docker.com) containers that you can use to deploy
and test locally.


Steps to use this:

1) Install [Docker](https://www.docker.com/community-edition)


2) Run the setup script located at the root of each module

|DBMS|Module|Script|Docker Image|
|----|------|------|----------------|
|DB2|obevo-db-db2|db2-setup.sh|[DB2 Express](https://hub.docker.com/r/ibmcom/db2express-c/)|
|MSSQL|obevo-db-mssql|mssql-setup.sh|[MSSQL Server on Linux](https://hub.docker.com/r/microsoft/mssql-server-linux/)|
|POSTGRESQL|obevo-db-postgresql|postgresql-setup.sh|[PostgreSQL image](https://hub.docker.com/_/postgres/)|

The setup script will do the following:

* Pulls the container image
* Starts the container instance
* Creates the DB and schemas
* (only applicable for DB2) Extracts the DB2 Driver jar from the container and installs it into your local Maven repository

Note: for Windows, try the Windows 10 Bash integration (though we haven't tested it yet). If that doesn't suffice, you
can follow along the db2-setup.sh script and replicate the steps in Windows.


3) In your IDE, enable the "integration-build" profile so that you can activate integration tests against your local Docker instance

(Note - DB2 does not enable the integration-build by default in the build as a couple integration tests like Db2PostDeployActionIT
are failing. Db2DeployerMainIT is fine, however. You can use that for testing.)
