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

CONTAINER_NAME=obevo-oracle-instance

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

docker run --name $CONTAINER_NAME -d -it -p 1521:1521 -p 5500:5500 -e DB_SID=ORCLCDB -e DB_PDB=ORCLPDB1 -eUSE_SID_AS_SERVICE_listener=on -eUSE_SID_AS_SERVICE_ORCLPDB1=on store/oracle/database-enterprise:12.2.0.1-slim

## TODO: automate the subsequent schema creation process; need to figure out how to execute sqlplus from this script easily
## We want to create a DBA account in the PDB ORCLPDB1; the default superuser account doesn't work easily here, and we need the PDB context set for the user.
## For now, run manually:
#docker exec -it $CONTAINER_NAME bash
#/u01/app/oracle/product/12.2.0/dbhome_1/bin/sqlplus sys/Oradoc_db1 as sysdba
# run the commands in oracle-setup-manual.sql

# TODO Docker is having issues; go w/ Vagrant and VirtualBox setup
#https://github.com/oracle/vagrant-boxes/tree/master/OracleDatabase/12.2.0.1
