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

CONTAINER_NAME=obevo-pgadmin-instance

RUNNING_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [[ ! -z "$RUNNING_CONTAINER_ID" ]]
then
    echo "Shutting down old container"
    docker stop $RUNNING_CONTAINER_ID
    docker rm $RUNNING_CONTAINER_ID
fi

PGADMIN_PORT=8080
PGADMIN_EMAIL="katadeployer@obevo-kata.com"
PGADMIN_PASSWORD="katadeploypass"
docker run --name $CONTAINER_NAME -p 8080:80 -e "PGADMIN_DEFAULT_EMAIL=$PGADMIN_EMAIL" -e "PGADMIN_DEFAULT_PASSWORD=$PGADMIN_PASSWORD" -d dpage/pgadmin4

echo ""
echo "pgadmin4 setup successful"
echo ""
echo "Please visit http://localhost:8080 w/ username = $PGADMIN_EMAIL and password as $PGADMIN_PASSWORD to access the page"

