--
-- Copyright 2017 Goldman Sachs.
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

create table METADATA_TEST_TABLE (afield int, bfield int)
GO
grant select, insert, update on METADATA_TEST_TABLE to DACT_RO
GO

create table TABLE_A (
    A_ID INT NOT NULL,
    A2_ID INT,
    PRIMARY KEY (A_ID)
)
GO
grant select, insert, update, delete on TABLE_A to DACT_RO
GO


create table TABLE_A_MULTICOL_PK (
    A1_ID INT NOT NULL,
    A2_ID INT NOT NULL,
    VAL3 INT,
    PRIMARY KEY (A1_ID, A2_ID)
)
GO
create table TABLE_B_WITH_MULTICOL_FK (
    B_ID INT NOT NULL,
    OTHER_A1_ID INT,
    OTHER_A2_ID INT,
    PRIMARY KEY (B_ID)
)
GO
ALTER TABLE TABLE_B_WITH_MULTICOL_FK ADD CONSTRAINT FK_A_MULTICOL FOREIGN KEY (OTHER_A1_ID, OTHER_A2_ID) REFERENCES TABLE_A_MULTICOL_PK(A1_ID, A2_ID)
GO

create table TABLE_B_WITH_FK (
    B_ID INT NOT NULL,
    OTHER_A_ID INT,
    PRIMARY KEY (B_ID)
)
GO
ALTER TABLE TABLE_B_WITH_FK ADD CONSTRAINT FK_A FOREIGN KEY (OTHER_A_ID) REFERENCES TABLE_A(A_ID)
GO

grant select, insert, update, delete on TABLE_B_WITH_FK to DACT_RO
GO


--no sequences yet in Oracle 11
--create table TABLE_GENERATED_ID (
--	GEN_ID    NUMBER GENERATED ALWAYS AS IDENTITY,
--	FIELD1  INT
--)
-- G O
--grant select, insert, update on TABLE_GENERATED_ID to DACT_RO
-- G O

-- need to ignore Oracle views for now due to bad testing environment
--CREATE VIEW VIEW1 AS SELECT * FROM METADATA_TEST_TABLE
-- my comment
-- G O
--GRANT select on VIEW1 to DACT_RO
-- G O


create table INVALID_TABLE (a INT)
GO
--create view INVALID_VIEW AS SELECT * FROM INVALID_TABLE
-- G O
--grant SELECT on INVALID_VIEW to DACT_RO
-- G O
DROP TABLE INVALID_TABLE
GO


CREATE SEQUENCE REGULAR_SEQUENCE START WITH 1 INCREMENT BY 5
GO


CREATE OR REPLACE FUNCTION FUNC1
RETURN integer IS
BEGIN
    -- ensure that func comment remains
    RETURN 1;
END;
GO


CREATE OR REPLACE PACKAGE PKG_FUNC_WITH_OVERLOAD
AS
    FUNCTION FUNC_WITH_OVERLOAD return integer;
    FUNCTION FUNC_WITH_OVERLOAD(var1 IN integer) return integer;
    FUNCTION FUNC_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR) return integer;
END;

GO


CREATE OR REPLACE PACKAGE BODY PKG_FUNC_WITH_OVERLOAD
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
GO

CREATE OR REPLACE PROCEDURE SP1 IS
BEGIN
    -- ensure that SP comment remains
    DELETE FROM TABLE_A;
END;
GO


CREATE OR REPLACE PACKAGE PKG_SP_WITH_OVERLOAD
AS
    PROCEDURE SP_WITH_OVERLOAD;
    PROCEDURE SP_WITH_OVERLOAD(INVAL IN integer);
    PROCEDURE SP_WITH_OVERLOAD(var1 IN integer, INVALSTR IN VARCHAR);
END;

GO

CREATE OR REPLACE PACKAGE BODY PKG_SP_WITH_OVERLOAD
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
