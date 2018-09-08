#!/bin/bash
#
# Copyright 2017 Goldman Sachs.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

## Variable Setup
DB2_VERSION=$1

# These props are also defined in docker-db2-creds.yaml. Copy this from there
INSTANCE_PORT=50000
INSTANCE_DBNAME="dbdeploy"
INSTANCE_SCHEMAS="dbdeploy01 dbdeploy02 dbdeploy03"
INSTANCE_USERID="db2inst1"  # note - this user ID is hardcoded by the container
INSTANCE_PASSWORD="db2inst1-pwd"
CONTAINER_NAME=obevo-db2-instance


## Now start the setup
docker pull ibmcom/db2express-c

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

echo "Starting new container"
docker run --name $CONTAINER_NAME -d -i -t -p $INSTANCE_PORT:$INSTANCE_PORT -e DB2INST1_PASSWORD=$INSTANCE_PASSWORD -e LICENSE=accept ibmcom/db2express-c:10.5.0.5-3.10.0 db2start

export CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")

echo "Creating the database (may take a few seconds)"
docker exec -it $CONTAINER_ID bash -c "su - $INSTANCE_USERID -c 'db2 create db $INSTANCE_DBNAME'"

for SCHEMA in $INSTANCE_SCHEMAS; do
    SCHEMAS_CREATE_COMMAND="$SCHEMAS_CREATE_COMMAND   db2 create schema $SCHEMA;"
done

echo "Logging into the database to create the schema"
docker exec -it $CONTAINER_ID bash -c "su - $INSTANCE_USERID -c 'db2 connect to $INSTANCE_DBNAME; $SCHEMAS_CREATE_COMMAND'"
