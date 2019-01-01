
ALTER SESSION SET container=ORCLPDB1;
CREATE USER deploydba IDENTIFIED BY MyPassword;
GRANT CONNECT, RESOURCE, DBA TO deploydba;
GRANT CREATE SESSION TO deploydba;
GRANT UNLIMITED TABLESPACE TO deploydba;


# now run a deploy to create the schemas


CREATE MATERIALIZED VIEW DBDEPLOY01.MV_TABLE_A
BUILD IMMEDIATE
REFRESH FORCE
ON DEMAND
AS
SELECT * FROM DBDEPLOY01.TABLE_A;

CREATE MATERIALIZED VIEW LOG ON DBDEPLOY01.TABLE_A
TABLESPACE users
WITH PRIMARY KEY
INCLUDING NEW VALUES;



ALTER SESSION SET container=ORCLPDB1;
grant create session to dbdeploy01;
grant create database link to dbdeploy01;

grant create session to dbdeploy02;
grant create database link to dbdeploy02;



# Now login as dbdeploy01 to create the DB link (in local testing, I can only do this via SQL Developer)

sqlplus dbdeploy01/schemaPassw0rd

CREATE DATABASE LINK dblink connect to dbdeploy02 identified by schemaPassw0rd using 'dblink';



# the rest are just scrap notes


CREATE MATERIALIZED VIEW DBDEPLOY01.MV_LINK4
BUILD IMMEDIATE
REFRESH FORCE
ON DEMAND
AS
SELECT * FROM DBDEPLOY01.TABLE_A@dblink5;

identified by abc using 'dbdeploy01.dblink';

CREATE DATABASE LINK dbdeploy01.dblink connect to dbdeploy02 identified by abc using 'dbdeploy01.dblink';
CREATE MATERIALIZED VIEW emp_mv_link
BUILD IMMEDIATE
REFRESH FORCE
ON DEMAND
AS
SELECT * FROM table_a@dbdeploy01.db;
