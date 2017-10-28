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

DROP TABLE ${subschema}METADATA_TEST_TABLE
GO

DROP TABLE ${subschema}TABLE_B_WITH_FK
GO

DROP TABLE ${subschema}TABLE_A
GO

DROP TABLE ${subschema}TABLE_A_ROOT
GO

DROP TABLE ${subschema}TABLE_B_WITH_MULTICOL_FK
GO

DROP TABLE ${subschema}TABLE_A_MULTICOL_PK
GO

DROP TABLE ${subschema}TABLE_GENERATED_ID
GO

DROP TABLE ${subschema}TEST_TYPES
GO
DROP TABLE TEST_TYPES
GO

DROP VIEW ${subschema}VIEW1
GO


DROP TABLE ${subschema}INVALID_TABLE
GO

DROP VIEW ${subschema}INVALID_VIEW
GO



DROP PROCEDURE ${subschema}SP_WITH_OVERLOAD
GO
DROP FUNCTION ${subschema}FUNC_WITH_OVERLOAD
GO
DROP PROCEDURE ${subschema}SP1
GO
DROP FUNCTION ${subschema}FUNC1
GO

sp_droptype N'MyType'
GO
sp_droptype N'MyType2'
GO
DROP TYPE ${subschema}MyType
GO
DROP TYPE ${subschema}MyType2
GO
DROP TYPE MyType
GO
DROP TYPE MyType2
GO

DROP RULE ${subschema}booleanRule
GO

DROP RULE ${subschema}booleanRule2
GO

<#if !(subschema?has_content)>
DROP DEFAULT DateDefault
GO
</#if>