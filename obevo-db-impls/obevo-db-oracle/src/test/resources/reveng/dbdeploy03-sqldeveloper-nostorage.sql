--------------------------------------------------------
--  DDL for Table METADATA_TEST_TABLE
--------------------------------------------------------

  CREATE TABLE "DBDEPLOY03"."METADATA_TEST_TABLE"
   (	"AFIELD" NUMBER(*,0),
	"BFIELD" NUMBER(*,0)
   ) ;
--------------------------------------------------------
--  DDL for Table TABLE_A
--------------------------------------------------------

  CREATE TABLE "DBDEPLOY03"."TABLE_A"
   (	"A_ID" NUMBER(*,0),
	"A2_ID" NUMBER(*,0)
   ) ;
--------------------------------------------------------
--  DDL for Table TABLE_A_MULTICOL_PK
--------------------------------------------------------

  CREATE TABLE "DBDEPLOY03"."TABLE_A_MULTICOL_PK"
   (	"A1_ID" NUMBER(*,0),
	"A2_ID" NUMBER(*,0),
	"VAL3" NUMBER(*,0)
   ) ;
--------------------------------------------------------
--  DDL for Table TABLE_B_WITH_FK
--------------------------------------------------------

  CREATE TABLE "DBDEPLOY03"."TABLE_B_WITH_FK"
   (	"B_ID" NUMBER(*,0),
	"OTHER_A_ID" NUMBER(*,0)
   ) ;
--------------------------------------------------------
--  DDL for Table TABLE_B_WITH_MULTICOL_FK
--------------------------------------------------------

  CREATE TABLE "DBDEPLOY03"."TABLE_B_WITH_MULTICOL_FK"
   (	"B_ID" NUMBER(*,0),
	"OTHER_A1_ID" NUMBER(*,0),
	"OTHER_A2_ID" NUMBER(*,0)
   ) ;
--------------------------------------------------------
--  DDL for Sequence REGULAR_SEQUENCE
--------------------------------------------------------

   CREATE SEQUENCE  "DBDEPLOY03"."REGULAR_SEQUENCE"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 5 START WITH 1 CACHE 20 NOORDER  NOCYCLE   ;
--------------------------------------------------------
--  DDL for Procedure SP1
--------------------------------------------------------
set define off;

  CREATE OR REPLACE EDITIONABLE PROCEDURE "DBDEPLOY03"."SP1" IS
BEGIN
    -- ensure that SP comment remains
    DELETE FROM TABLE_A;
END;

/
--------------------------------------------------------
--  DDL for Package PKG_FUNC_WITH_OVERLOAD
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY03"."PKG_FUNC_WITH_OVERLOAD"
AS
    FUNCTION FUNC_WITH_OVERLOAD return integer;
    FUNCTION FUNC_WITH_OVERLOAD(var1 IN integer) return integer;
    FUNCTION FUNC_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR) return integer;
END;


/
--------------------------------------------------------
--  DDL for Package PKG_SP_WITH_OVERLOAD
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY03"."PKG_SP_WITH_OVERLOAD"
AS
    PROCEDURE SP_WITH_OVERLOAD;
    PROCEDURE SP_WITH_OVERLOAD(INVAL IN integer);
    PROCEDURE SP_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR);
END;


/
--------------------------------------------------------
--  DDL for Package Body PKG_FUNC_WITH_OVERLOAD
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE PACKAGE BODY "DBDEPLOY03"."PKG_FUNC_WITH_OVERLOAD"
AS
    FUNCTION FUNC_WITH_OVERLOAD
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;

    FUNCTION FUNC_WITH_OVERLOAD (var1 IN integer)
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;

    FUNCTION FUNC_WITH_OVERLOAD (var1 IN integer, INVALSTR IN VARCHAR)
    RETURN integer IS
    BEGIN
        RETURN 1;
    END;
END;

/
--------------------------------------------------------
--  DDL for Package Body PKG_SP_WITH_OVERLOAD
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE PACKAGE BODY "DBDEPLOY03"."PKG_SP_WITH_OVERLOAD"
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
END;

/
--------------------------------------------------------
--  DDL for Function FUNC1
--------------------------------------------------------

  CREATE OR REPLACE EDITIONABLE FUNCTION "DBDEPLOY03"."FUNC1"
RETURN integer IS
BEGIN
    -- ensure that func comment remains
    RETURN 1;
END;

/
--------------------------------------------------------
--  Constraints for Table TABLE_A
--------------------------------------------------------

  ALTER TABLE "DBDEPLOY03"."TABLE_A" ADD PRIMARY KEY ("A_ID")
  USING INDEX  ENABLE;
  ALTER TABLE "DBDEPLOY03"."TABLE_A" MODIFY ("A_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table TABLE_B_WITH_MULTICOL_FK
--------------------------------------------------------

  ALTER TABLE "DBDEPLOY03"."TABLE_B_WITH_MULTICOL_FK" ADD PRIMARY KEY ("B_ID")
  USING INDEX  ENABLE;
  ALTER TABLE "DBDEPLOY03"."TABLE_B_WITH_MULTICOL_FK" MODIFY ("B_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table TABLE_B_WITH_FK
--------------------------------------------------------

  ALTER TABLE "DBDEPLOY03"."TABLE_B_WITH_FK" ADD PRIMARY KEY ("B_ID")
  USING INDEX  ENABLE;
  ALTER TABLE "DBDEPLOY03"."TABLE_B_WITH_FK" MODIFY ("B_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table TABLE_A_MULTICOL_PK
--------------------------------------------------------

  ALTER TABLE "DBDEPLOY03"."TABLE_A_MULTICOL_PK" ADD PRIMARY KEY ("A1_ID", "A2_ID")
  USING INDEX  ENABLE;
  ALTER TABLE "DBDEPLOY03"."TABLE_A_MULTICOL_PK" MODIFY ("A2_ID" NOT NULL ENABLE);
  ALTER TABLE "DBDEPLOY03"."TABLE_A_MULTICOL_PK" MODIFY ("A1_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Ref Constraints for Table TABLE_B_WITH_FK
--------------------------------------------------------

  ALTER TABLE "DBDEPLOY03"."TABLE_B_WITH_FK" ADD CONSTRAINT "FK_A" FOREIGN KEY ("OTHER_A_ID")
	  REFERENCES "DBDEPLOY03"."TABLE_A" ("A_ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table TABLE_B_WITH_MULTICOL_FK
--------------------------------------------------------

  ALTER TABLE "DBDEPLOY03"."TABLE_B_WITH_MULTICOL_FK" ADD CONSTRAINT "FK_A_MULTICOL" FOREIGN KEY ("OTHER_A1_ID", "OTHER_A2_ID")
	  REFERENCES "DBDEPLOY03"."TABLE_A_MULTICOL_PK" ("A1_ID", "A2_ID") ENABLE;
