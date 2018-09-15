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

INSTANCE_PORT=3306
INSTANCE_DBNAME="dbdeploy"
INSTANCE_SCHEMAS="dbdeploy03 dbdeploy01 dbdeploy02"
INSTANCE_USERID="root"  # note - this user ID is hardcoded by the container
INSTANCE_PASSWORD="Deploybuilddb0!"

CONTAINER_IMAGE="mysql:latest"
CONTAINER_NAME=obevo-mssql-instance

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

echo "Setting password $INSTANCE_PASSWORD"

docker pull $CONTAINER_IMAGE
docker run -e "MYSQL_ROOT_PASSWORD=$INSTANCE_PASSWORD" \
   -p $INSTANCE_PORT:$INSTANCE_PORT --name $CONTAINER_NAME \
   -d $CONTAINER_IMAGE

echo "Container created"

#for SCHEMA in $INSTANCE_SCHEMAS; do
#    docker run -it --link $CONTAINER_NAME --rm mysql sh -c 'exec mysql -h"localhost" -P"$INSTANCE_PORT" -uroot -p"$INSTANCE_PASSWORD"'
#    docker run -it --link $CONTAINER_NAME --rm mysql sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'

#    docker exec $CONTAINER_NAME /opt/mssql-tools/bin/sqlcmd \
#       -S localhost -U $INSTANCE_USERID -P "$INSTANCE_PASSWORD" \
#       -Q "CREATE DATABASE $SCHEMA"
#done
