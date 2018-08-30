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


# DB2 Developer Setup

To test Obevo against DB2, you can leverage [Docker](https://www.docker.com) to setup a DB2 Express instance on your
local machine.


1) Install [Docker](https://www.docker.com/community-edition)


2) Run the db2-setup.sh script at the root of the obevo-db-db2 module. This will do the following:

* Pulls the [DB2 Express Docker Image](https://hub.docker.com/r/ibmcom/db2express-c/)
* Starts the container instance
* Creates the DB and schemas
* Extracts the DB2 Driver jar from the container and installs it into your local Maven repository

Note: for Windows, try the Windows 10 Bash integration (though we haven't tested it yet). If that doesn't suffice, you
can follow along the db2-setup.sh script and replicate the steps in Windows.


3) In your IDE, enable the "amazon-build" profile so that you can activate integration tests against the DB2 server
