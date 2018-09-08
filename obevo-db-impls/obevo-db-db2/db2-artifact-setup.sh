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

# These props are also defined in local-db2-creds.yaml. Copy this from there
DB2_GROUP=com.ibm.db2

echo "Setting up DB2 Jars in your Maven environment"
DB2_ARTIFACTS="db2jcc db2jcc4 db2jcc_license_cu"
DB2_JAVA_BINARY_HOME=/home/$INSTANCE_USERID/sqllib/java
TMPDIR=/tmp
for ARTIFACT in $DB2_ARTIFACTS; do
    echo "Working on artifact $ARTIFACT"
    docker cp $CONTAINER_ID:$DB2_JAVA_BINARY_HOME/$ARTIFACT.jar $TMPDIR/$ARTIFACT.jar
    mvn install:install-file -DgroupId=$DB2_GROUP -DartifactId=$ARTIFACT -Dversion=$DB2_VERSION -Dfile=$TMPDIR/$ARTIFACT.jar -Dpackaging=jar -DgeneratePom=true
    rm -f $TMPDIR/$ARTIFACT.jar
done

echo "Done with setup"
