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

CREATE DEFAULT DateDefault AS '01Jan1972'
GO
CREATE RULE booleanRule
as @booleanValue in (1, 0)
go
CREATE RULE booleanRule2
as @booleanValue in (1, 0)
go

sp_addtype N'MyType', N'tinyint', N'not null'
GO
sp_addtype N'MyType2', N'tinyint', N'not null'
GO

create table METADATA_TEST_TABLE (afield int, bfield int)
GO
grant select, insert, update on METADATA_TEST_TABLE to DACT_RO_DBDeploy
GO

create table TABLE_A (
    A_ID INT NOT NULL,
    A2_ID INT,
    PRIMARY KEY (A_ID)
)
GO
grant select, insert, update, delete on TABLE_A to DACT_RO_DBDeploy
GO

create table TEST_TYPES (
	idField INT NOT NULL,
	stringDateField DATE NULL,
	myBooleanCol INT NULL,
	myTypeField MyType NULL,
	myTypeField2 MyType2 NULL,
	CONSTRAINT PK PRIMARY KEY (idField)
)
GO
sp_bindefault 'DateDefault', 'TEST_TYPES.stringDateField'
GO
sp_bindrule booleanRule, 'TEST_TYPES.myBooleanCol'
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

grant select, insert, update, delete on TABLE_B_WITH_FK to DACT_RO_DBDeploy
GO

create table TABLE_GENERATED_ID (
	GEN_ID    BIGINT IDENTITY NOT NULL,
	FIELD1  INT
)
GO
grant select, insert, update on TABLE_GENERATED_ID to DACT_RO_DBDeploy
GO



CREATE VIEW VIEW1 AS SELECT * FROM METADATA_TEST_TABLE
-- my comment
GO
GRANT select on VIEW1 to DACT_RO_DBDeploy
GO


create table INVALID_TABLE (a INT)
GO
create view INVALID_VIEW AS SELECT * FROM INVALID_TABLE
GO
grant SELECT on VIEW1 to DACT_RO_DBDeploy
GO
DROP TABLE INVALID_TABLE
GO


CREATE FUNCTION FUNC1()
RETURNS INT
AS
BEGIN
    -- ensure that func comment remains
    RETURN 10
END
GO


-- NOTE - no function overloads supported in ASE
CREATE FUNCTION FUNC_WITH_OVERLOAD (@var1 INT, @INVALSTR VARCHAR(32))
RETURNS INT AS
BEGIN
RETURN 10
END
GO


CREATE PROCEDURE SP1
AS
    -- ensure that SP comment remains
    DELETE FROM TABLE_A
    DELETE FROM TABLE_A
GO

CREATE PROCEDURE SP_WITH_OVERLOAD
AS
    DELETE FROM TABLE_A
GO

CREATE PROCEDURE SP_WITH_OVERLOAD;2 (@INVAL INT)
AS
    DELETE FROM TABLE_A
    DELETE FROM TABLE_A

GO

CREATE PROCEDURE SP_WITH_OVERLOAD;4 (@INVAL INT, @INVALSTR VARCHAR(32))
AS
    DELETE FROM TABLE_A
    DELETE FROM TABLE_A
    DELETE FROM TABLE_A
    DELETE FROM TABLE_A
GO
