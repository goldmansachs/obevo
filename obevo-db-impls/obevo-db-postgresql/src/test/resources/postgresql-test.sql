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

grant select, insert, update, delete on TABLE_B_WITH_FK to GROUP DACT_RO
GO

create table TABLE_GENERATED_ID (
	GEN_ID    SERIAL,
	FIELD1  INT
)
GO
grant select, insert, update on TABLE_GENERATED_ID to GROUP DACT_RO
GO



CREATE VIEW VIEW1 AS SELECT * FROM METADATA_TEST_TABLE
-- my comment
GO
GRANT select on VIEW1 to GROUP DACT_RO
GO


-- Postgresql does not allow dropping objects that have dependencies, so the invalid view test is not applicable


CREATE SEQUENCE REGULAR_SEQUENCE
GO


CREATE OR REPLACE FUNCTION FUNC1 () RETURNS INT AS $$
BEGIN
    -- ensure that func comment remains
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
GO

CREATE FUNCTION FUNC_WITH_OVERLOAD () RETURNS INT AS $$
BEGIN
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
GO

CREATE FUNCTION FUNC_WITH_OVERLOAD (var1 integer) RETURNS INT AS $$
BEGIN
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
GO

CREATE FUNCTION FUNC_WITH_OVERLOAD (var1 integer, IN INVALSTR VARCHAR(32)) RETURNS INT AS $$
BEGIN
    RETURN 1;
END;
$$ LANGUAGE plpgsql;
GO


