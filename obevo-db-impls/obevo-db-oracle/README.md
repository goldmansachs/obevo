Oracle Setup steps:

https://aws.amazon.com/
My Account -> Security Credentials
Create user with RDS Full Permissions
Store the results in ~/.aws/credentials

[default]
aws_access_key_id=yourAccessKey
aws_secret_access_key=yourSecretAccessKey

https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html



https://chartio.com/resources/tutorials/how-to-create-a-user-and-grant-permissions-in-oracle/

SELECT OBJECT_TYPE, dbms_metadata.get_ddl(REPLACE(object_type,' ','_'), object_name, owner) || ';' AS object_ddl
--select object_type, object_name, owner
FROM DBA_OBJECTS
WHERE
      OWNER = 'DBDEPLOY03'
  AND OBJECT_TYPE NOT IN ('PACKAGE BODY')
  AND OBJECT_TYPE NOT IN('LOB','MATERIALIZED VIEW', 'TABLE PARTITION')

ORDER BY
    OWNER
  , OBJECT_TYPE
  , OBJECT_NAME


-- to fix params
EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE',false);
SELECT DBMS_METADATA.GET_DDL('TABLE',u.table_name)
     FROM USER_ALL_TABLES u
     WHERE u.nested='NO'
     AND (u.iot_type is null or u.iot_type='IOT');
EXECUTE DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'DEFAULT');
possible values: https://docs.oracle.com/database/121/ARPLS/d_metada.htm#BGBJBFGE

-- params to set
PARTITIONING
SEGMENT_ATTRIBUTES
STORAGE
