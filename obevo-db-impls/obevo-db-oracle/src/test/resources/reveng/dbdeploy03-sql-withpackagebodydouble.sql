"
  CREATE OR REPLACE EDITIONABLE FUNCTION "DBDEPLOY03"."FUNC1"
RETURN integer IS
BEGIN
    -- ensure that func comment remains
    RETURN 1;
END;
;"
"
  CREATE UNIQUE INDEX "DBDEPLOY03"."SYS_C005307" ON "DBDEPLOY03"."TABLE_A" ("A_ID")
  PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE UNIQUE INDEX "DBDEPLOY03"."SYS_C005310" ON "DBDEPLOY03"."TABLE_A_MULTICOL_PK" ("A1_ID", "A2_ID")
  PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE UNIQUE INDEX "DBDEPLOY03"."SYS_C005312" ON "DBDEPLOY03"."TABLE_B_WITH_MULTICOL_FK" ("B_ID")
  PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE UNIQUE INDEX "DBDEPLOY03"."SYS_C005315" ON "DBDEPLOY03"."TABLE_B_WITH_FK" ("B_ID")
  PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY03"."PKG_FUNC_WITH_OVERLOAD"
AS
    FUNCTION FUNC_WITH_OVERLOAD return integer;
    FUNCTION FUNC_WITH_OVERLOAD(var1 IN integer) return integer;
    FUNCTION FUNC_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR) return integer;
END;


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
;"
"
  CREATE OR REPLACE EDITIONABLE PACKAGE "DBDEPLOY03"."PKG_SP_WITH_OVERLOAD"
AS
    PROCEDURE SP_WITH_OVERLOAD;
    PROCEDURE SP_WITH_OVERLOAD(INVAL IN integer);
    PROCEDURE SP_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR);
END;


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
;"
"
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
;"
"
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
;"
"
  CREATE OR REPLACE EDITIONABLE PROCEDURE "DBDEPLOY03"."SP1" IS
BEGIN
    -- ensure that SP comment remains
    DELETE FROM TABLE_A;
END;
;"
"
   CREATE SEQUENCE  "DBDEPLOY03"."REGULAR_SEQUENCE"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 5 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOPARTITION ;"
"
  CREATE TABLE "DBDEPLOY03"."METADATA_TEST_TABLE"
   (	"AFIELD" NUMBER(*,0),
	"BFIELD" NUMBER(*,0)
   ) SEGMENT CREATION IMMEDIATE
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE TABLE "DBDEPLOY03"."TABLE_A"
   (	"A_ID" NUMBER(*,0) NOT NULL ENABLE,
	"A2_ID" NUMBER(*,0),
	 PRIMARY KEY ("A_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE TABLE "DBDEPLOY03"."TABLE_A_MULTICOL_PK"
   (	"A1_ID" NUMBER(*,0) NOT NULL ENABLE,
	"A2_ID" NUMBER(*,0) NOT NULL ENABLE,
	"VAL3" NUMBER(*,0),
	 PRIMARY KEY ("A1_ID", "A2_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE TABLE "DBDEPLOY03"."TABLE_B_WITH_FK"
   (	"B_ID" NUMBER(*,0) NOT NULL ENABLE,
	"OTHER_A_ID" NUMBER(*,0),
	 PRIMARY KEY ("B_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE,
	 CONSTRAINT "FK_A" FOREIGN KEY ("OTHER_A_ID")
	  REFERENCES "DBDEPLOY03"."TABLE_A" ("A_ID") ENABLE
   ) SEGMENT CREATION IMMEDIATE
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"
"
  CREATE TABLE "DBDEPLOY03"."TABLE_B_WITH_MULTICOL_FK"
   (	"B_ID" NUMBER(*,0) NOT NULL ENABLE,
	"OTHER_A1_ID" NUMBER(*,0),
	"OTHER_A2_ID" NUMBER(*,0),
	 PRIMARY KEY ("B_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE,
	 CONSTRAINT "FK_A_MULTICOL" FOREIGN KEY ("OTHER_A1_ID", "OTHER_A2_ID")
	  REFERENCES "DBDEPLOY03"."TABLE_A_MULTICOL_PK" ("A1_ID", "A2_ID") ENABLE
   ) SEGMENT CREATION IMMEDIATE
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;"