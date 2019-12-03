#!/bin/bash
#
# Copyright 2017 Goldman Sachs.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

CONTAINER_NAME=obevo-postgresql-instance

RUNNING_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")


if [[ ! -z "$RUNNING_CONTAINER_ID" ]]
then
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $RUNNING_CONTAINER_ID
else
    echo "Container is not running"
    exit 1
fi
