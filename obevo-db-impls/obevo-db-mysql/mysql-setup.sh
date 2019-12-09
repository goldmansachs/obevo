#!/bin/bash
set -e
set -x

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

DB_VERSION=$1

INSTANCE_PORT=3306
INSTANCE_DBNAME="dbdeploy"
INSTANCE_SCHEMAS="dbdeploy03 dbdeploy01 dbdeploy02"
INSTANCE_USERID="root"  # note - this user ID is hardcoded by the container
INSTANCE_PASSWORD="Deploybuilddb0!"

CONTAINER_IMAGE="mysql:$DB_VERSION"
CONTAINER_NAME=obevo-mysql-instance

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

echo "MySQL container creation started"

docker run -e "MYSQL_ROOT_PASSWORD=$INSTANCE_PASSWORD" \
   -p $INSTANCE_PORT:$INSTANCE_PORT --name $CONTAINER_NAME \
   -e MYSQL_DATABASE=$INSTANCE_DBNAME \
   -d $CONTAINER_IMAGE

echo "MySQL container created"
docker ps
sleep 30
docker ps
echo "MySQL container setup done"
