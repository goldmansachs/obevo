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

//// METADATA templateParams="shardNum=1;shardNum=2;shardNum=3"
//// CHANGE name="init" parallelGroup="run1"
CREATE TABLE TABLE_SHARDED_${shardNum} (
  FIELD1 INT NULL
) IN ${defaultTablespace}
GO

//// CHANGE name="col2" parallelGroup="run2"
ALTER TABLE TABLE_SHARDED_${shardNum} ADD FIELD2 INT NULL
GO

//// CHANGE name="reorg" parallelGroup="run3"
call sysproc.admin_cmd ('reorg table ${DEPLOY_TRACKER_physicalName}.TABLE_SHARDED_${shardNum}')
GO
