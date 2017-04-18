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

-- Note - copied from SchemaCrawler DB2 library but with VALID flag added. Will contribute back to schemacrawler

SELECT
  NULLIF(1, 1)
    AS TABLE_CATALOG,
  STRIP(SYSCAT.VIEWS.VIEWSCHEMA)
    AS TABLE_SCHEMA,
  STRIP(SYSCAT.VIEWS.VIEWNAME)
    AS TABLE_NAME,
  SYSCAT.VIEWS.TEXT
    AS VIEW_DEFINITION,
  CASE WHEN STRIP(SYSCAT.VIEWS.VIEWCHECK) = 'N' THEN 'NONE' ELSE 'CASCADED' END
    AS CHECK_OPTION,
  CASE WHEN STRIP(SYSCAT.VIEWS.READONLY) = 'Y' THEN 'NO' ELSE 'YES' END
    AS IS_UPDATABLE,
    VALID AS VALID
FROM
  SYSCAT.VIEWS
ORDER BY
  SYSCAT.VIEWS.VIEWSCHEMA,
  SYSCAT.VIEWS.VIEWNAME,
  SYSCAT.VIEWS.SEQNO
WITH UR  
