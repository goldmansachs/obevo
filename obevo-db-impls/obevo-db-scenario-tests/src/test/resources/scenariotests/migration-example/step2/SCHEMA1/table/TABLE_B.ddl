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

//// METADATA includeEnvs="testFailure"
//// CHANGE name=chng1
CREATE TABLE TABLE_B (
	B_ID    INT	NOT NULL,
	B_VAL   VARCHAR(32) NULL
)
GO

//// CHANGE name=chng2
ALTER TABLE TABLE_B ADD COLUMN B_VAL2 VARCHAR(32)
GO

//// CHANGE name=chng3 dependencies="migration_fail_example.migrate"
ALTER TABLE TABLE_B DROP COLUMN B_VAL
GO
