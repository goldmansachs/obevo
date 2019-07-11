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
set -e

## Variable Setup
DB2_VERSION=$1

# These props are also defined in docker-db2-creds.yaml. Copy this from there
INSTANCE_PORT=50000
INSTANCE_DBNAME="dbdeploy"
INSTANCE_USERID="db2inst1"  # note - this user ID is hardcoded by the container
INSTANCE_PASSWORD="db2inst1-pwd"
CONTAINER_NAME=obevo-db2-instance


## Now start the setup
OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

echo "Starting new container"

dir_resolve() {
    local dir=`dirname "$1"`
    pushd "$dir" &>/dev/null || return $? # On error, return error code
    echo "`pwd -P`" # output full, link-resolved path with filename
    popd &> /dev/null
}

PWDVARNAME=`echo "$INSTANCE_USERID" | tr a-z A-Z`
CURDIR=$(dir_resolve $(dirname $0))
docker run --name $CONTAINER_NAME -d -i -t -p $INSTANCE_PORT:$INSTANCE_PORT -e DB2INSTANCE=$INSTANCE_USERID -e ${PWDVARNAME}_PASSWORD=$INSTANCE_PASSWORD -e DBNAME=$INSTANCE_DBNAME -e LICENSE=accept -e IS_OSXFS=true --ipc=host --privileged=true -v $CURDIR/target/db2docker:/database ibmcom/db2:$DB2_VERSION

echo "Done with setup"

COUNTER=1
while [ $COUNTER -lt 10 ]
do
    echo "Try #$COUNTER to start docker"
    ((COUNTER++))

    CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
    if [ -z "$CONTAINER_ID" ]
    then
        echo "Container failed unexpectedly; exiting with error"
        exit 6
    fi

    docker exec -ti $CONTAINER_NAME bash -c "su - $INSTANCE_USERID -c 'db2 connect to $INSTANCE_DBNAME'" && RETVAL=$? || RETVAL=$?

    if [ $RETVAL -eq 0 ]; then
        echo "DB started"
        break
    fi

    echo "DB not yet started on count #$COUNTER, will wait for 30 seconds"
    sleep 30
done

export CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
