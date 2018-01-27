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

//// METADATA excludeEnvs="unittest*,test%schema"
-- excluding these from schema-based envs as these object types are not supported
-- boolean already exists in the in-mem db environments
CREATE TYPE Boolean FROM tinyint NOT NULL
GO
sp_bindrule booleanRule, Boolean
GO
