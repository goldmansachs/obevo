--
-- Copyright 2017 Goldman Sachs.
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--



-- Note - this file is here for eventually testing materialized views



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
