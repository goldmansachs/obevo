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
package com.gs.obevo.dbmetadata.impl.dialects;

import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.databaseconnector.DatabaseServerType;

public final class SybaseIqOdbcDatabaseConnector extends DatabaseConnector {

    private static final long serialVersionUID = 1L;

    public SybaseIqOdbcDatabaseConnector() {
        super(new DatabaseServerType("sybaseiq", "SAP Sybase IQ"),
                "/help/Connections.sybaseiq.txt",
                "/schemacrawler-sybaseiq.config.properties",
                "/sybaseiqodbc.information_schema",
                "notapplicable:.*"
        );
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDatabaseSpecificOverrideOptionsBuilder() {
        final DatabaseSpecificOverrideOptionsBuilder databaseSpecificOverrideOptionsBuilder = super.getDatabaseSpecificOverrideOptionsBuilder();
        databaseSpecificOverrideOptionsBuilder.doesNotSupportCatalogs();  // Unlike the regular JDBC driver, catalogs are not supported
        return databaseSpecificOverrideOptionsBuilder;
    }
}