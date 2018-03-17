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

2) Install the [DB2 Express Docker Image](https://hub.docker.com/r/ibmcom/db2express-c/)

3) Create and start the DB2 Instance by starting the Docker container

```
# Start the container and store the container ID for reference
CONTAINER_ID=$(docker run -d -i -t -p 50000:50000 -e DB2INST1_PASSWORD=db2inst1-pwd -e LICENSE=accept ibmcom/db2express-c:latest db2start)

# Create the database (may take a few seconds)
docker exec -it $CONTAINER_ID bash -c "su - db2inst1 -c 'db2 create db dbdeploy'"

# Log into the database to subsequently run more actions
docker exec -it $CONTAINER_ID bash -c "su - db2inst1"
```

4) Create the schemas from within the DB2 bash shell

```
# Log into the database to subsequently run more actions
db2 connect to dbdeploy
db2 create schema dbdeploy01
db2 create schema dbdeploy02
db2 create schema dbdeploy03
```

5) Exit the bash shell, and copy the DB2 drivers jars out from the docker container and install into your Maven repository

```
DB2_VERSION=10.5.0.5
DB2_GROUP=com.ibm.db2
DB2_ARTIFACTS=db2jcc db2jcc4 db2jcc_license_cu
DB2_JAVA_BINARY_HOME=/home/db2inst1/sqllib/java

for ARTIFACT in $DB2_ARTIFACTS; do
    echo "Working on artifact $ARTIFACT"
    docker cp $CONTAINER_ID:$DB2_JAVA_BINARY_HOME/$ARTIFACT.jar $TMPDIR/$ARTIFACT.jar
    mvn install:install-file -DgroupId=$DB2_GROUP -DartifactId=$ARTIFACT -Dversion=$DB2_VERSION -Dfile=$TMPDIR/$ARTIFACT.jar -Dpackaging=jar -DgeneratePom=true
    rm -f $TMPDIR/$ARTIFACT.jar
done
```

6) In your IDE, enable the "amazon-build" profile so that you can activate integration tests against the DB2 server
