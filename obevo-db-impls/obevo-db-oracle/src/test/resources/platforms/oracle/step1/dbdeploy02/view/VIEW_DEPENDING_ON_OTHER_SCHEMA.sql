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

//// METADATA includeDependencies="dbdeploy01.VIEW1" excludeEnvs="%"
/* not yet implementing this - need to add checks/grants for this in our code logic or in the DB itself */
CREATE OR REPLACE VIEW VIEW_DEPENDING_ON_OTHER_SCHEMA AS SELECT * FROM ${dbdeploy01_physicalName}.VIEW1
