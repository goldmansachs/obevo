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

DELETE FROM INSERTFK_A
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (1,null,'anull')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (2,null,'anull')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (10,110,'a10')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (11,110,'a11')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (20,120,'a20')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (21,120,'a21')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (30,130,'a30')
GO
INSERT INTO INSERTFK_A (A_ID,B_ID,A_DATA) VALUES (31,130,'a31')
GO
