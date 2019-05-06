
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

  CREATE TABLE "DBDEPLOY01"."ARTIFACTEXECUTIONATTR" 
   (	"DEPLOYEXECUTIONID" NUMBER(19,0) NOT NULL ENABLE, 
	"ATTRNAME" VARCHAR2(128) NOT NULL ENABLE, 
	"ATTRVALUE" VARCHAR2(128) NOT NULL ENABLE
   ) SEGMENT CREATION DEFERRED 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS"
~

  CREATE TABLE "DBDEPLOY01"."COMMENT_COL_TABLE" 
   (	"ID" NUMBER NOT NULL ENABLE, 
	"VAL2" NUMBER
   ) SEGMENT CREATION DEFERRED 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS"
~

   COMMENT ON COLUMN "DBDEPLOY01"."COMMENT_COL_TABLE"."ID" IS 'comment col table id'
~

   COMMENT ON COLUMN "DBDEPLOY01"."COMMENT_COL_TABLE"."VAL2" IS 'comment col table val2'
~

  CREATE TABLE "DBDEPLOY01"."COMMENT_TABLE" 
   (	"ID" NUMBER NOT NULL ENABLE
   ) SEGMENT CREATION DEFERRED 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS"
~

   COMMENT ON COLUMN "DBDEPLOY01"."COMMENT_TABLE"."ID" IS 'comment2'
~

   COMMENT ON TABLE "DBDEPLOY01"."COMMENT_TABLE"  IS 'comment1'
~

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "DBDEPLOY01"."COMMENT_VIEW" ("A_ID", "STRING_FIELD") AS 
  SELECT A_ID, STRING_FIELD FROM dbdeploy01.TABLE_A WHERE A_ID = 3
~

   COMMENT ON COLUMN "DBDEPLOY01"."COMMENT_VIEW"."A_ID" IS 'comment VIEW COL A_ID 2'
~

CREATE OR REPLACE FORCE EDITIONABLE EDITIONING VIEW "DBDEPLOY01"."VIEW_SYMBOL#" ("A_ID", "STRING_FIELD") AS
SELECT A_ID, STRING_FIELD FROM dbdeploy01.TABLE_A WHERE A_ID = 3
GO
~
  CREATE OR REPLACE EDITIONABLE FUNCTION "DBDEPLOY01"."FUNC_WITH_OVERLOAD" (IN input INT, IN invalstr VARCHAR(32), OUT mycount INT) RETURNS INT AS $$
BEGIN
    SELECT count(*) into mycount FROM TABLE_A;
END;
$$ LANGUAGE plpgsql;
~

   CREATE SEQUENCE  "DBDEPLOY01"."MYSEQ1"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL
~

   CREATE SEQUENCE  "DBDEPLOY01"."MYSEQ2"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL
~
   OBEVO EXCEPTION MY_ERROR_OBJECT of type TABLE
/*
Please report this as an Issue on the Obevo Github page so that we can improve the reverse-engineering logic.
For now, resolve this on your side.
java.sql.SQLException: ORA-31603: object "COL1_TAB" of type TABLE not found in schema "DBDEPLOY01"
ORA-06512: at "SYS.DBMS_METADATA", line 6478
ORA-06512: at "SYS.DBMS_SYS_ERROR", line 105
ORA-06512: at "SYS.DBMS_METADATA", line 6465
ORA-06512: at "SYS.DBMS_METADATA", line 9202
ORA-06512: at line 1

	at oracle.jdbc.driver.T4CTTIoer.processError(T4CTTIoer.java:450)
	at oracle.jdbc.driver.T4CTTIoer.processError(T4CTTIoer.java:399)
	at oracle.jdbc.driver.T4C8Oall.processError(T4C8Oall.java:1059)
	at oracle.jdbc.driver.T4CTTIfun.receive(T4CTTIfun.java:522)
	at oracle.jdbc.driver.T4CTTIfun.doRPC(T4CTTIfun.java:257)
	at oracle.jdbc.driver.T4C8Oall.doOALL(T4C8Oall.java:587)
	at oracle.jdbc.driver.T4CStatement.doOall8(T4CStatement.java:210)
	at oracle.jdbc.driver.T4CStatement.doOall8(T4CStatement.java:30)
	at oracle.jdbc.driver.T4CStatement.executeForRows(T4CStatement.java:931)
	at oracle.jdbc.driver.OracleStatement.executeMaybeDescribe(OracleStatement.java:957)
	at oracle.jdbc.driver.OracleStatement.doExecuteWithTimeout(OracleStatement.java:1111)
	at oracle.jdbc.driver.OracleStatement.executeQuery(OracleStatement.java:1309)
	at oracle.jdbc.driver.OracleStatementWrapper.executeQuery(OracleStatementWrapper.java:422)
	at org.apache.commons.dbcp.DelegatingStatement.executeQuery(DelegatingStatement.java:208)
	at org.apache.commons.dbcp.DelegatingStatement.executeQuery(DelegatingStatement.java:208)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryAndLeaveStatementOpenInternal(JdbcHelper.java:203)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryAndLeaveStatementOpen(JdbcHelper.java:193)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.query(JdbcHelper.java:172)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryForList(JdbcHelper.java:225)
	at com.gs.obevo.db.impl.platforms.oracle.OracleReveng.queryObjects(OracleReveng.kt:132)
	at com.gs.obevo.db.impl.platforms.oracle.OracleReveng.doRevengOrInstructions(OracleReveng.kt:84)
	at com.gs.obevo.db.apps.reveng.AbstractDdlReveng.reveng(AbstractDdlReveng.java:165)
	at com.gs.obevo.db.impl.platforms.oracle.OracleRevengIT.testReveng(OracleRevengIT.java:82)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:309)
	at org.junit.runners.Suite.runChild(Suite.java:127)
	at org.junit.runners.Suite.runChild(Suite.java:26)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:309)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:160)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)

	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryAndLeaveStatementOpenInternal(JdbcHelper.java:206)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryAndLeaveStatementOpen(JdbcHelper.java:193)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.query(JdbcHelper.java:172)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryForList(JdbcHelper.java:225)
	at com.gs.obevo.db.impl.platforms.oracle.OracleReveng.queryObjects(OracleReveng.kt:132)
	at com.gs.obevo.db.impl.platforms.oracle.OracleReveng.doRevengOrInstructions(OracleReveng.kt:84)
	at com.gs.obevo.db.apps.reveng.AbstractDdlReveng.reveng(AbstractDdlReveng.java:165)
	at com.gs.obevo.db.impl.platforms.oracle.OracleRevengIT.testReveng(OracleRevengIT.java:82)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:309)
	at org.junit.runners.Suite.runChild(Suite.java:127)
	at org.junit.runners.Suite.runChild(Suite.java:26)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:309)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:160)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)
	at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)
	at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)
Caused by: java.sql.SQLException: ORA-31603: object "COL1_TAB" of type TABLE not found in schema "DBDEPLOY01"
ORA-06512: at "SYS.DBMS_METADATA", line 6478
ORA-06512: at "SYS.DBMS_SYS_ERROR", line 105
ORA-06512: at "SYS.DBMS_METADATA", line 6465
ORA-06512: at "SYS.DBMS_METADATA", line 9202
ORA-06512: at line 1

	at oracle.jdbc.driver.T4CTTIoer.processError(T4CTTIoer.java:450)
	at oracle.jdbc.driver.T4CTTIoer.processError(T4CTTIoer.java:399)
	at oracle.jdbc.driver.T4C8Oall.processError(T4C8Oall.java:1059)
	at oracle.jdbc.driver.T4CTTIfun.receive(T4CTTIfun.java:522)
	at oracle.jdbc.driver.T4CTTIfun.doRPC(T4CTTIfun.java:257)
	at oracle.jdbc.driver.T4C8Oall.doOALL(T4C8Oall.java:587)
	at oracle.jdbc.driver.T4CStatement.doOall8(T4CStatement.java:210)
	at oracle.jdbc.driver.T4CStatement.doOall8(T4CStatement.java:30)
	at oracle.jdbc.driver.T4CStatement.executeForRows(T4CStatement.java:931)
	at oracle.jdbc.driver.OracleStatement.executeMaybeDescribe(OracleStatement.java:957)
	at oracle.jdbc.driver.OracleStatement.doExecuteWithTimeout(OracleStatement.java:1111)
	at oracle.jdbc.driver.OracleStatement.executeQuery(OracleStatement.java:1309)
	at oracle.jdbc.driver.OracleStatementWrapper.executeQuery(OracleStatementWrapper.java:422)
	at org.apache.commons.dbcp.DelegatingStatement.executeQuery(DelegatingStatement.java:208)
	at org.apache.commons.dbcp.DelegatingStatement.executeQuery(DelegatingStatement.java:208)
	at com.gs.obevo.db.impl.core.jdbc.JdbcHelper.queryAndLeaveStatementOpenInternal(JdbcHelper.java:203)
	... 37 more

*/
end
~
  CREATE TABLE "DBDEPLOY01"."NESTED_TABLE" 
   (	"ID" NUMBER, 
	"COL1" "DBDEPLOY01"."NESTED_TABLE_TYPE" 
   ) SEGMENT CREATION DEFERRED 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" 
 NESTED TABLE "COL1" STORE AS "COL1_TAB"
 (PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 LOGGING
  TABLESPACE "USERS" ) RETURN AS VALUE
~

  CREATE OR REPLACE EDITIONABLE TYPE "DBDEPLOY01"."NESTED_TABLE_TYPE" AS TABLE OF VARCHAR2(30);
~

  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY01"."PKG_ADDING_BODY" 
AS
    FUNCTION ADDING_BODY return integer;
END;

CREATE OR REPLACE EDITIONABLE PACKAGE BODY "DBDEPLOY01"."PKG_ADDING_BODY" 
AS
    FUNCTION PKG_ADDING_BODY
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;
END;
~

  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY01"."PKG_FUNC_WITH_OVERLOAD" 
AS
    -- change to trigger a deployment
    FUNCTION INNER_FUNC_WITH_OVERLOAD return integer;
    FUNCTION INNER_FUNC_WITH_OVERLOAD(var1 IN integer) return integer;
    FUNCTION INNER_FUNC_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR) return integer;
END

CREATE OR REPLACE EDITIONABLE PACKAGE BODY "DBDEPLOY01"."PKG_FUNC_WITH_OVERLOAD" 
AS
    FUNCTION INNER_FUNC_WITH_OVERLOAD
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;

    FUNCTION INNER_FUNC_WITH_OVERLOAD (var1 IN integer)
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;

    FUNCTION INNER_FUNC_WITH_OVERLOAD (var1 IN integer, INVALSTR IN VARCHAR)
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;
END
~

  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY01"."PKG_REMOVING_BODY" 
AS
    FUNCTION REMOVING_BODY return integer;
END;
~

  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY01"."PKG_SP_WITH_OVERLOAD" 
AS
    PROCEDURE SP_WITH_OVERLOAD;
    PROCEDURE SP_WITH_OVERLOAD(INVAL IN integer);
    PROCEDURE SP_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR);
END;

CREATE OR REPLACE EDITIONABLE PACKAGE BODY "DBDEPLOY01"."PKG_SP_WITH_OVERLOAD" 
AS
    PROCEDURE SP_WITH_OVERLOAD IS
    BEGIN
        DELETE FROM TABLE_A;
    END;

    PROCEDURE SP_WITH_OVERLOAD (INVAL IN integer) IS
    BEGIN
        DELETE FROM TABLE_A;
    END;

    PROCEDURE SP_WITH_OVERLOAD (var1 IN integer, INVALSTR IN VARCHAR) IS
    BEGIN
        DELETE FROM TABLE_A;
    END;
    -- change to trigger a deployment
END;
~

  CREATE OR REPLACE EDITIONABLE FUNCTION "DBDEPLOY01"."SP3" (OUT mycount INT) RETURNS INT AS $$
BEGIN
    SELECT count(*) into mycount FROM TABLE_A;
END;
$$ LANGUAGE plpgsql;
~

  CREATE OR REPLACE EDITIONABLE PROCEDURE "DBDEPLOY01"."SP_CHAR_TEST" AS
BEGIN
dbms_output.put_line('Hello world'); --目标表名
END character_test;
~

  CREATE OR REPLACE EDITIONABLE SYNONYM "DBDEPLOY01"."SYN1" FOR "DBDEPLOY01"."TABLE_A"
~

  CREATE OR REPLACE EDITIONABLE SYNONYM "DBDEPLOY01"."SYN_TO_ADD" FOR "DBDEPLOY01"."TABLE_A"
~

  CREATE OR REPLACE EDITIONABLE SYNONYM "DBDEPLOY01"."SYN_TO_UPDATE" FOR "DBDEPLOY01"."TABLE_B"
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

  CREATE TABLE "DBDEPLOY01"."TABLE_B" 
   (	"B_ID" NUMBER(*,0) NOT NULL ENABLE, 
	"VAL1" NUMBER(*,0),
	"VAL2" NUMBER(*,0),
	"VAL3" NUMBER(*,0),
	 PRIMARY KEY ("B_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS"
~

  CREATE OR REPLACE EDITIONABLE TRIGGER "DBDEPLOY01"."TRIGGER1"
AFTER LOGON
ON DBDEPLOY01.schema
--ON database
DECLARE
BEGIN
EXECUTE IMMEDIATE 'ALTER SESSION SET CURRENT_SCHEMA=DBDEPLOY01';
END
/
ALTER TRIGGER "DBDEPLOY01"."TRIGGER1" ENABLE
~

  CREATE INDEX "DBDEPLOY01"."TABLE_A_IND1" ON "DBDEPLOY01"."TABLE_A" ("B_ID", "STRING_FIELD")
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
  TABLESPACE "USERS"
~

  CREATE INDEX "DBDEPLOY01"."TABLE_B_IND1" ON "DBDEPLOY01"."TABLE_B" ("VAL1", "VAL2")
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
  TABLESPACE "USERS"
~

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "DBDEPLOY01"."VIEW1" ("A_ID", "B_ID", "STRING_FIELD", "TIMESTAMP_FIELD", "C_ID", "EXTRA1", "EXTRA2", "EXTRA3", "EXTRA4") AS
  SELECT "A_ID","B_ID","STRING_FIELD","TIMESTAMP_FIELD","C_ID","EXTRA1","EXTRA2","EXTRA3","EXTRA4" FROM VIEW2
~

  CREATE OR REPLACE FORCE EDITIONABLE VIEW "DBDEPLOY01"."VIEW2" ("A_ID", "B_ID", "STRING_FIELD", "TIMESTAMP_FIELD", "C_ID", "EXTRA1", "EXTRA2", "EXTRA3", "EXTRA4") AS 
  SELECT "A_ID","B_ID","STRING_FIELD","TIMESTAMP_FIELD","C_ID","EXTRA1","EXTRA2","EXTRA3","EXTRA4" FROM TABLE_A WHERE A_ID = 4
~

  CREATE UNIQUE INDEX "DBDEPLOY01"."ARTDEFPK" ON "DBDEPLOY01"."ARTIFACTDEPLOYMENT" ("ARTIFACTPATH", "OBJECTNAME")
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
  TABLESPACE "USERS"
~

  CREATE UNIQUE INDEX "DBDEPLOY01"."DEPL_EXEC_PK" ON "DBDEPLOY01"."ARTIFACTEXECUTION" ("ID")
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
  TABLESPACE "USERS"
~
