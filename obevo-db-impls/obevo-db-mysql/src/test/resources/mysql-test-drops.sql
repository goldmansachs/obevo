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

DROP VIEW VIEW1
GO


DROP TABLE METADATA_TEST_TABLE
GO

DROP TABLE TABLE_B_WITH_FK
GO

DROP TABLE TABLE_A
GO

DROP TABLE TABLE_A_ROOT
GO

DROP TABLE TABLE_B_WITH_MULTICOL_FK
GO

DROP TABLE TABLE_A_MULTICOL_PK
GO

DROP TABLE TABLE_GENERATED_ID
GO



DROP TABLE INVALID_TABLE
GO

DROP VIEW INVALID_VIEW
GO


DROP SEQUENCE REGULAR_SEQUENCE
GO


DROP PROCEDURE SP_WITH_OVERLOAD (INT, VARCHAR(32))
GO
DROP PROCEDURE SP_WITH_OVERLOAD (INT)
GO
DROP PROCEDURE SP_WITH_OVERLOAD ()
GO
DROP FUNCTION FUNC_WITH_OVERLOAD (INT, VARCHAR(32))
GO
DROP FUNCTION FUNC_WITH_OVERLOAD (INT)
GO
DROP FUNCTION FUNC_WITH_OVERLOAD ()
GO
DROP PROCEDURE SP1 ()
GO
DROP FUNCTION FUNC1 ()
GO
