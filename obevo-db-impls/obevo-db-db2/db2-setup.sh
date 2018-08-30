#!/bin/bash

## Variable Setup
DB2_VERSION=$1

# These props are also defined in local-db2-creds.yaml. Copy this from there
INSTANCE_PORT=50000
INSTANCE_DBNAME="DBDEPLOY"
INSTANCE_SCHEMAS="DBDEPLOY01 DBDEPLOY02 DBDEPLOY03"
INSTANCE_USERID="db2inst1"  # note - this user ID is hardcoded by the container
INSTANCE_PASSWORD="db2inst1-pwd"
DB2_GROUP=com.ibm.db2
CONTAINER_NAME=obevo-db2-instance


## Now start the setup
docker pull ibmcom/db2express-c

OLD_CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
if [ ! -z "$OLD_CONTAINER_ID" ]
then
    echo "Shutting down old container"
    docker stop $OLD_CONTAINER_ID
    docker rm $OLD_CONTAINER_ID
fi

echo "Starting new container"
docker run --name $CONTAINER_NAME -d -i -t -p $INSTANCE_PORT:$INSTANCE_PORT -e DB2INST1_PASSWORD=$INSTANCE_PASSWORD -e LICENSE=accept ibmcom/db2express-c:latest db2start

CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")

echo "Creating the database (may take a few seconds)"
docker exec -it $CONTAINER_ID bash -c "su - $INSTANCE_USERID -c 'db2 create db $INSTANCE_DBNAME'"

for SCHEMA in $INSTANCE_SCHEMAS; do
    SCHEMAS_CREATE_COMMAND="$SCHEMAS_CREATE_COMMAND   db2 create schema $SCHEMA;"
done

echo "Logging into the database to create the schema"
docker exec -it $CONTAINER_ID bash -c "su - $INSTANCE_USERID -c 'db2 connect to $INSTANCE_DBNAME; $SCHEMAS_CREATE_COMMAND'"

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
