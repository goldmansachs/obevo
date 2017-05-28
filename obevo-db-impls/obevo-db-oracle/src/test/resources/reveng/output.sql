
  CREATE UNIQUE INDEX "DBDEPLOY01"."SYS_C005373" ON "DBDEPLOY01"."TABLE_B" ("B_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" 
~

  CREATE TABLE "DBDEPLOY01"."TABLE_B" 
   (	"B_ID" NUMBER(*,0) NOT NULL ENABLE, 
	 PRIMARY KEY ("B_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" 
~

  CREATE UNIQUE INDEX "DBDEPLOY01"."SYS_C005371" ON "DBDEPLOY01"."TABLE_A" ("A_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" 
~

   CREATE SEQUENCE  "DBDEPLOY01"."MYSEQ1"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOPARTITION 
~

  CREATE TABLE "DBDEPLOY01"."TABLE_A" 
   (	"A_ID" NUMBER(*,0) NOT NULL ENABLE, 
	"B_ID" NUMBER(*,0) NOT NULL ENABLE, 
	"STRING_FIELD" VARCHAR2(30), 
	"TIMESTAMP_FIELD" TIMESTAMP (6), 
	"C_ID" NUMBER(*,0), 
	"EXTRA1" NUMBER(*,0), 
	"EXTRA2" NUMBER(*,0), 
	"EXTRA3" NUMBER(*,0), 
	"EXTRA4" NUMBER(*,0), 
	 PRIMARY KEY ("A_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE, 
	 FOREIGN KEY ("B_ID")
	  REFERENCES "DBDEPLOY01"."TABLE_B" ("B_ID") ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" 
~

   CREATE SEQUENCE  "DBDEPLOY01"."MYSEQ2"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOPARTITION 
~

  CREATE UNIQUE INDEX "DBDEPLOY01"."ARTDEFPK" ON "DBDEPLOY01"."ARTIFACTDEPLOYMENT" ("ARTIFACTPATH", "OBJECTNAME") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" 
~

  CREATE TABLE "DBDEPLOY01"."ARTIFACTDEPLOYMENT" 
   (	"ARTFTYPE" VARCHAR2(31) NOT NULL ENABLE, 
	"ARTIFACTPATH" VARCHAR2(255) NOT NULL ENABLE, 
	"OBJECTNAME" VARCHAR2(255) NOT NULL ENABLE, 
	"ACTIVE" NUMBER(*,0), 
	"CHANGETYPE" VARCHAR2(255), 
	"CONTENTHASH" VARCHAR2(255), 
	"DBSCHEMA" VARCHAR2(255), 
	"DEPLOY_USER_ID" VARCHAR2(32), 
	"TIME_INSERTED" TIMESTAMP (6), 
	"TIME_UPDATED" TIMESTAMP (6), 
	"ROLLBACKCONTENT" VARCHAR2(2048), 
	"INSERTDEPLOYID" NUMBER(19,0), 
	"UPDATEDEPLOYID" NUMBER(19,0), 
	 CONSTRAINT "ARTDEFPK" PRIMARY KEY ("ARTIFACTPATH", "OBJECTNAME")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" 
~

  CREATE TABLE "DBDEPLOY01"."ARTIFACTEXECUTIONATTR" 
   (	"DEPLOYEXECUTIONID" NUMBER(19,0) NOT NULL ENABLE, 
	"ATTRNAME" VARCHAR2(128) NOT NULL ENABLE, 
	"ATTRVALUE" VARCHAR2(128) NOT NULL ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" 
~

  CREATE UNIQUE INDEX "DBDEPLOY01"."DEPL_EXEC_PK" ON "DBDEPLOY01"."ARTIFACTEXECUTION" ("ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" 
~

  CREATE TABLE "DBDEPLOY01"."ARTIFACTEXECUTION" 
   (	"ID" NUMBER(19,0) NOT NULL ENABLE, 
	"STATUS" CHAR(1) NOT NULL ENABLE, 
	"DEPLOYTIME" TIMESTAMP (6) NOT NULL ENABLE, 
	"EXECUTORID" VARCHAR2(128) NOT NULL ENABLE, 
	"TOOLVERSION" VARCHAR2(32) NOT NULL ENABLE, 
	"INIT_COMMAND" NUMBER(*,0) NOT NULL ENABLE, 
	"ROLLBACK_COMMAND" NUMBER(*,0) NOT NULL ENABLE, 
	"REQUESTERID" VARCHAR2(128), 
	"REASON" VARCHAR2(128), 
	"PRODUCTVERSION" VARCHAR2(255), 
	"DBSCHEMA" VARCHAR2(255), 
	 CONSTRAINT "DEPL_EXEC_PK" PRIMARY KEY ("ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" 
~

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "DBDEPLOY01"."VIEW1" ("A_ID", "B_ID", "STRING_FIELD", "TIMESTAMP_FIELD", "C_ID", "EXTRA1", "EXTRA2", "EXTRA3", "EXTRA4") AS 
  SELECT "A_ID","B_ID","STRING_FIELD","TIMESTAMP_FIELD","C_ID","EXTRA1","EXTRA2","EXTRA3","EXTRA4" FROM VIEW2
~

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "DBDEPLOY01"."VIEW2" ("A_ID", "B_ID", "STRING_FIELD", "TIMESTAMP_FIELD", "C_ID", "EXTRA1", "EXTRA2", "EXTRA3", "EXTRA4") AS 
  SELECT "A_ID","B_ID","STRING_FIELD","TIMESTAMP_FIELD","C_ID","EXTRA1","EXTRA2","EXTRA3","EXTRA4" FROM TABLE_A WHERE A_ID = 4
~

  CREATE OR REPLACE EDITIONABLE FUNCTION "DBDEPLOY01"."SP3" (OUT mycount INT) RETURNS INT AS $$
BEGIN
    SELECT count(*) into mycount FROM TABLE_A;
END;
$$ LANGUAGE plpgsql
~

  CREATE OR REPLACE EDITIONABLE FUNCTION "DBDEPLOY01"."FUNC_WITH_OVERLOAD" (IN input INT, IN invalstr VARCHAR(32), OUT mycount INT) RETURNS INT AS $$
BEGIN
    SELECT count(*) into mycount FROM TABLE_A;
END
~