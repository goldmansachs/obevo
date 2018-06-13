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

DELETE FROM INSERTFK_B
GO
INSERT INTO INSERTFK_B (B_ID,B_DATA) VALUES (110,'b10')
GO
INSERT INTO INSERTFK_B (B_ID,B_DATA) VALUES (120,'b20')
GO
INSERT INTO INSERTFK_B (B_ID,B_DATA) VALUES (130,'b30')
GO
