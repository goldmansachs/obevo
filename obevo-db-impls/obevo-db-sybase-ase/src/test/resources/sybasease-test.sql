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

<#if !(subschema?has_content)>
CREATE DEFAULT ${subschema}DateDefault AS '01Jan1972'
GO
CREATE RULE ${subschema}booleanRule
as @booleanValue in (1, 0)
go
CREATE RULE ${subschema}booleanRule2
as @booleanValue in (1, 0)
go
</#if>

<#if !(subschema?has_content)>
sp_addtype N'MyType', N'tinyint', N'not null'
GO
sp_addtype N'MyType2', N'tinyint', N'not null'
GO
</#if>

create table ${subschema}METADATA_TEST_TABLE (afield int, bfield int)
GO

create table ${subschema}TABLE_A (
    A_ID INT NOT NULL,
    A2_ID INT,
    PRIMARY KEY (A_ID)
)
GO

create table ${subschema}TEST_TYPES (
	idField INT NOT NULL,
	stringDateField DATE NULL,
	myBooleanCol INT NULL,
<#if !(subschema?has_content)>
	myTypeField ${subschema}MyType NULL,
	myTypeField2 ${subschema}MyType2 NULL,
</#if>
	CONSTRAINT PK PRIMARY KEY (idField)
)
GO

-- not applicable for user
<#if !(subschema?has_content)>
sp_bindefault 'DateDefault', 'TEST_TYPES.stringDateField'
GO
sp_bindrule booleanRule, 'TEST_TYPES.myBooleanCol'
GO
</#if>


create table ${subschema}TABLE_A_MULTICOL_PK (
    A1_ID INT NOT NULL,
    A2_ID INT NOT NULL,
    VAL3 INT,
    PRIMARY KEY (A1_ID, A2_ID)
)
GO
create table ${subschema}TABLE_B_WITH_MULTICOL_FK (
    B_ID INT NOT NULL,
    OTHER_A1_ID INT,
    OTHER_A2_ID INT,
    PRIMARY KEY (B_ID)
)
GO
ALTER TABLE ${subschema}TABLE_B_WITH_MULTICOL_FK ADD CONSTRAINT FK_A_MULTICOL FOREIGN KEY (OTHER_A1_ID, OTHER_A2_ID) REFERENCES ${subschema}TABLE_A_MULTICOL_PK(A1_ID, A2_ID)
GO

create table ${subschema}TABLE_B_WITH_FK (
    B_ID INT NOT NULL,
    OTHER_A_ID INT,
    PRIMARY KEY (B_ID)
)
GO
ALTER TABLE ${subschema}TABLE_B_WITH_FK ADD CONSTRAINT FK_A FOREIGN KEY (OTHER_A_ID) REFERENCES ${subschema}TABLE_A(A_ID)
GO

create table ${subschema}TABLE_GENERATED_ID (
	GEN_ID    BIGINT IDENTITY NOT NULL,
	FIELD1  INT
)
GO



CREATE VIEW ${subschema}VIEW1 AS SELECT * FROM ${subschema}METADATA_TEST_TABLE
-- my comment
GO


create table ${subschema}INVALID_TABLE (a INT)
GO
create view ${subschema}INVALID_VIEW AS SELECT * FROM ${subschema}INVALID_TABLE
GO
DROP TABLE ${subschema}INVALID_TABLE
GO


CREATE FUNCTION ${subschema}FUNC1
RETURNS INT
AS
    -- ensure that func comment remains
RETURN 10
GO


-- NOTE - no function overloads supported in ASE
CREATE FUNCTION ${subschema}FUNC_WITH_OVERLOAD (@var1 INT, @INVALSTR VARCHAR(32))
RETURNS INT AS
RETURN 10
GO


CREATE PROCEDURE ${subschema}SP1
AS
    -- ensure that SP comment remains
    DELETE FROM ${subschema}TABLE_A
    DELETE FROM ${subschema}TABLE_A
GO

CREATE PROCEDURE ${subschema}SP_WITH_OVERLOAD
AS
    DELETE FROM ${subschema}TABLE_A
GO

CREATE PROCEDURE ${subschema}SP_WITH_OVERLOAD;2 (@INVAL INT)
AS
    DELETE FROM ${subschema}TABLE_A
    DELETE FROM ${subschema}TABLE_A

GO

CREATE PROCEDURE ${subschema}SP_WITH_OVERLOAD;4 (@INVAL INT, @INVALSTR VARCHAR(32))
AS
    DELETE FROM ${subschema}TABLE_A
    DELETE FROM ${subschema}TABLE_A
    DELETE FROM ${subschema}TABLE_A
    DELETE FROM ${subschema}TABLE_A
GO
