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

//// METADATA order=1
// naming the file as Z to ensure that it is alphabetically listed after the other REPORT_GROUP files so that we can
// test out the ordering logic
DELETE FROM TABLE_C
GO
INSERT INTO TABLE_C (C_ID, C_VAL) VALUES (999, 'c999')
GO
