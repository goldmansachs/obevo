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

#DB_VERSION=$1
DB_VERSION=latest

# NOTE - this relies on Docker 2.1.0.5 (2.2.x not supported as of 2020-02-29) due to the error mentioned below:
# https://stackoverflow.com/questions/58194704/memsql-docker-image-could-not-reach-local-agent-at-127-0-0-19000

CONTAINER_IMAGE="memsql/quickstart:$DB_VERSION"
CONTAINER_NAME=obevo-memsql-instance

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

echo "MemSQL container creation started"


docker run -d -p 3306:3306 -p 9000:9000 --name $CONTAINER_NAME $CONTAINER_IMAGE

echo "MemSQL container created"
