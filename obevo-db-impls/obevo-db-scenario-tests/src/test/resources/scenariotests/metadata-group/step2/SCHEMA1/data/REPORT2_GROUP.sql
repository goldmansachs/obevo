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

DELETE FROM TABLE_A WHERE A_ID IN (20, 21, 22, 23)
GO
DELETE FROM TABLE_B WHERE B_ID IN (2)
GO

INSERT INTO TABLE_B (B_ID, C_ID, B_VAL) VALUES (2, 999, 'b1')
GO

INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (20, 2, 'a20')
GO
INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (21, 2, 'a21')
GO
INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (22, 2, 'a22')
GO
INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (23, 2, 'a23')
GO

