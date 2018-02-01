/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.dbmetadata.impl.dialects;

import java.sql.Connection;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import org.eclipse.collections.impl.factory.Lists;
import schemacrawler.schema.RoutineType;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

public class PostgresqlMetadataDialect extends AbstractMetadataDialect {
    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn) {
        // postgresql only supports FUNCTIONs in its syntax, not PROCEDUREs. However, the metadata still comes
        // back as procedure. We override the metadata value using the getRoutineOverrideValue method.
        options.setRoutineTypes(Lists.immutable.with(RoutineType.procedure).castToList());
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema) {
        DatabaseSpecificOverrideOptionsBuilder dbSpecificOptionsBuilder = super.getDbSpecificOptionsBuilder(conn, physicalSchema);

        dbSpecificOptionsBuilder.withInformationSchemaViews().withSequencesSql(
                "SELECT\n" +
                        "  NULL AS SEQUENCE_CATALOG,\n" +
                        "  SEQUENCE_SCHEMA,\n" +
                        "  SEQUENCE_NAME,\n" +
                        "  INCREMENT,\n" +
                        "  MINIMUM_VALUE,\n" +
                        "  MAXIMUM_VALUE,\n" +
                        "  CYCLE_OPTION\n" +
                        "FROM\n" +
                        "  INFORMATION_SCHEMA.SEQUENCES\n" +
                        "WHERE SEQUENCE_SCHEMA = '" + physicalSchema.getPhysicalName() + "'\n" +
                        "ORDER BY\n" +
                        "  SEQUENCE_CATALOG,\n" +
                        "  SEQUENCE_SCHEMA,\n" +
                        "  SEQUENCE_NAME\n");

        return dbSpecificOptionsBuilder;
    }

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        return "(?i)" + physicalSchema.getPhysicalName();
    }

    /**
     * PostgreSQL should override the routine type to "function". PostgreSQL only supports functions in its SQL syntax;
     * however, the JDBC metadata returns it as a "procedure". For consistency w/ the SQL syntax, we do this override.
     */
    @Override
    public DaRoutineType getRoutineOverrideValue() {
        return DaRoutineType.function;
    }
}
