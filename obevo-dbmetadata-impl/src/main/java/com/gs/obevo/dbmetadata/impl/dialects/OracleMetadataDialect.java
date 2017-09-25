/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * For SchemaCrawler fix, we need to copy and fix a line of code from:
 * https://github.com/sualeh/SchemaCrawler/blob/master/schemacrawler-oracle/src/main/resources/oracle.information_schema/ROUTINES.sql
 *
 * The Eclipse Public License is available at:
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.gs.obevo.dbmetadata.impl.dialects;

import java.sql.Connection;

import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;

/**
 * Oracle DBMS metadata dialect.
 */
public class OracleMetadataDialect extends AbstractMetadataDialect {
    @Override
    public String getSchemaExpression(String schemaName) {
        return schemaName;
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, String schemaName) {
        DatabaseSpecificOverrideOptionsBuilder dbSpecificOptionsBuilder = super.getDbSpecificOptionsBuilder(conn, schemaName);

        // we needed to remove the "PROCEDURES.AUTHID" caluse from the query in SchemaCrawler - see https://github.com/sualeh/SchemaCrawler/blob/master/schemacrawler-oracle/src/main/resources/oracle.information_schema/ROUTINES.sql
        dbSpecificOptionsBuilder.getInformationSchemaViewsBuilder().withRoutinesSql("SELECT /*+ PARALLEL(AUTO) */\n" +
                "  NULL AS ROUTINE_CATALOG,\n" +
                "  PROCEDURES.OWNER AS ROUTINE_SCHEMA,\n" +
                "  PROCEDURES.OBJECT_NAME AS ROUTINE_NAME,\n" +
                "  PROCEDURES.OBJECT_NAME AS SPECIFIC_NAME,\n" +
                "  'SQL' AS ROUTINE_BODY,\n" +
                "  DBMS_METADATA.GET_DDL(OBJECT_TYPE, PROCEDURES.OBJECT_NAME, PROCEDURES.OWNER) \n" +
                "    AS ROUTINE_DEFINITION\n" +
                "FROM\n" +
                "  ALL_PROCEDURES PROCEDURES\n" +
                "WHERE\n" +
                "  PROCEDURES.OWNER NOT IN \n" +
                "    ('ANONYMOUS', 'APEX_PUBLIC_USER', 'APPQOSSYS', 'BI', 'CTXSYS', 'DBSNMP', 'DIP', \n" +
                "    'EXFSYS', 'FLOWS_30000', 'FLOWS_FILES', 'HR', 'IX', 'LBACSYS', \n" +
                "    'MDDATA', 'MDSYS', 'MGMT_VIEW', 'OE', 'OLAPSYS', 'ORACLE_OCM', \n" +
                "    'ORDPLUGINS', 'ORDSYS', 'OUTLN', 'OWBSYS', 'PM', 'SCOTT', 'SH', \n" +
                "    'SI_INFORMTN_SCHEMA', 'SPATIAL_CSW_ADMIN_USR', 'SPATIAL_WFS_ADMIN_USR', \n" +
                "    'SYS', 'SYSMAN', 'SYSTEM', 'TSMSYS', 'WKPROXY', 'WKSYS', 'WK_TEST', \n" +
                "    'WMSYS', 'XDB', 'XS$NULL', 'RDSADMIN')  \n" +
                "  AND NOT REGEXP_LIKE(PROCEDURES.OWNER, '^APEX_[0-9]{6}$')\n" +
                "  AND NOT REGEXP_LIKE(PROCEDURES.OWNER, '^FLOWS_[0-9]{5,6}$')\n" +
                "  AND REGEXP_LIKE(PROCEDURES.OWNER, '${schemas}')\n" +
//                "  AND PROCEDURES.AUTHID = 'CURRENT_USER'\n" +
                "ORDER BY\n" +
                "  ROUTINE_SCHEMA,\n" +
                "  ROUTINE_NAME\n");
        return dbSpecificOptionsBuilder;
    }
}
