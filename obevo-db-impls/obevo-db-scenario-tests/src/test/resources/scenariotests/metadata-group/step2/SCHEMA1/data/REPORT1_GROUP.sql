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

DELETE FROM TABLE_A WHERE A_ID IN (10, 11, 12, 13)
GO
DELETE FROM TABLE_B WHERE B_ID IN (1)
GO

INSERT INTO TABLE_B (B_ID, C_ID, B_VAL) VALUES (1, 999, 'b1')
GO

INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (10, 1, 'a10')
GO
INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (11, 1, 'a11')
GO
INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (12, 1, 'a12')
GO
INSERT INTO TABLE_A (A_ID, B_ID, A_VAL) VALUES (13, 1, 'a13')
GO

