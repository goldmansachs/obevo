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

CONTAINER_NAME=obevo-postgresql-instance

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

docker run --name $CONTAINER_NAME -e POSTGRES_PASSWORD=mysecretpassword -d -p 5432:5432 postgres
