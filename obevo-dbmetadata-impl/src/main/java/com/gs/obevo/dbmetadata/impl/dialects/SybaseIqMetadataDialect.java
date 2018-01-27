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
import java.sql.SQLException;

import com.gs.obevo.api.appdata.PhysicalSchema;
import org.eclipse.collections.impl.factory.Lists;
import schemacrawler.schema.RoutineType;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.server.sybaseiq.SybaseIQDatabaseConnector;
import schemacrawler.server.sybaseiq.SybaseIQOdbcDatabaseConnector;

public class SybaseIqMetadataDialect extends AbstractMetadataDialect {
    private boolean odbcDriverUsed;

    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn) {
        this.odbcDriverUsed = checkIfOdbcDriver(conn);

        // IQ only officially supports procedures. Function syntax is supported, but is still counted as a procedure in its metadata
        options.setRoutineTypes(Lists.immutable.with(RoutineType.procedure).castToList());
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema) {
        if (odbcDriverUsed) {
            return new SybaseIQOdbcDatabaseConnector().getDatabaseSpecificOverrideOptionsBuilder();
        } else {
            return new SybaseIQDatabaseConnector().getDatabaseSpecificOverrideOptionsBuilder();
        }
    }

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        if (odbcDriverUsed) {
            return physicalSchema.getPhysicalName();
        } else {
            return ".*\\." + physicalSchema.getPhysicalName();
        }
    }

    private static boolean checkIfOdbcDriver(Connection conn) {
        try {
            return conn.getMetaData().getDriverName().contains("odbc")
                    || conn.getMetaData().getDriverName().contains("SQL Anywhere");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
