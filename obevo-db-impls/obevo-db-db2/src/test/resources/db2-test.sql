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

create table METADATA_TEST_TABLE (afield int, bfield int)
GO
grant select, insert, update on METADATA_TEST_TABLE to GROUP DACT_RO
GO

create table TABLE_A (
    A_ID INT NOT NULL,
    A2_ID INT,
    PRIMARY KEY (A_ID)
)
GO
grant select, insert, update, delete on TABLE_A to GROUP DACT_RO
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
ALTER TABLE TABLE_B_WITH_MULTICOL_FK ADD FOREIGN KEY FK_A_MULTICOL (OTHER_A1_ID, OTHER_A2_ID) REFERENCES TABLE_A_MULTICOL_PK(A1_ID, A2_ID)
GO

create table TABLE_B_WITH_FK (
    B_ID INT NOT NULL,
    OTHER_A_ID INT,
    PRIMARY KEY (B_ID)
)
GO
ALTER TABLE TABLE_B_WITH_FK ADD FOREIGN KEY FK_A (OTHER_A_ID) REFERENCES TABLE_A(A_ID)
GO

grant select, insert, update, delete on TABLE_B_WITH_FK to GROUP DACT_RO
GO

create table TABLE_GENERATED_ID (
	GEN_ID    BIGINT GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1, CACHE 20) NOT NULL,
	FIELD1  INT
)
GO
grant select, insert, update on TABLE_GENERATED_ID to GROUP DACT_RO
GO



CREATE VIEW VIEW1 AS SELECT * FROM METADATA_TEST_TABLE
-- my comment
WITH LOCAL CHECK OPTION
GO
GRANT select on VIEW1 to GROUP DACT_RO
GO


create table INVALID_TABLE (a INT)
GO
create view INVALID_VIEW AS SELECT * FROM INVALID_TABLE
GO
grant SELECT on VIEW1 to GROUP DACT_RO
GO
DROP TABLE INVALID_TABLE
GO


CREATE SEQUENCE REGULAR_SEQUENCE AS INTEGER START WITH 1 INCREMENT BY 5
GO


CREATE FUNCTION FUNC1 ()
RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA
    -- ensure that func comment remains
RETURN VALUES (1)
GO


CREATE FUNCTION FUNC_WITH_OVERLOAD ()
RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA
RETURN VALUES (1)
GO

CREATE FUNCTION FUNC_WITH_OVERLOAD (var1 integer)
RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA
RETURN VALUES (1)
GO

CREATE FUNCTION FUNC_WITH_OVERLOAD (var1 integer, IN INVALSTR VARCHAR(32))
RETURNS integer language SQL NOT deterministic NO EXTERNAL ACTION READS SQL DATA
RETURN VALUES (1)
GO


CREATE PROCEDURE SP1 ()
LANGUAGE SQL  DYNAMIC RESULT SETS 1
BEGIN ATOMIC
    -- ensure that SP comment remains
    DELETE FROM TABLE_A;
END
GO

CREATE PROCEDURE SP_WITH_OVERLOAD ()
LANGUAGE SQL  DYNAMIC RESULT SETS 1
BEGIN ATOMIC
    DELETE FROM TABLE_A;
END
GO

CREATE PROCEDURE SP_WITH_OVERLOAD (IN INVAL INT)
LANGUAGE SQL  DYNAMIC RESULT SETS 1
BEGIN ATOMIC
    DELETE FROM TABLE_A;
END
GO

CREATE PROCEDURE SP_WITH_OVERLOAD (IN INVAL INT, IN INVALSTR VARCHAR(32))
LANGUAGE SQL  DYNAMIC RESULT SETS 1
BEGIN ATOMIC
    DELETE FROM TABLE_A;
END
