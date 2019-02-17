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

#https://github.com/oracle/vagrant-boxes/tree/master/OracleDatabase/12.2.0.1

CURDIR=$(dirname $0)
VB_SETUP_HOME=~/IdeaProjects
VB_ORACLE_HOME=$VB_SETUP_HOME/vagrant-boxes/OracleDatabase/12.2.0.1

git clone https://github.com/oracle/vagrant-boxes.git

cd $VB_ORACLE_HOME

cp $CURDIR/oracle-init.sql $VB_SETUP_HOME/vagrant-boxes/OracleDatabase/12.2.0.1/userscripts

## TODO download from  here - http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html
# https://download.oracle.com/otn/linux/oracle12c/122010/linuxx64_12201_database.zip
vagrant up
