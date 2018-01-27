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
package com.gs.obevo.db.impl.platforms.postgresql;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.AbstractSqlExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreSqlSqlExecutor extends AbstractSqlExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(PostgreSqlSqlExecutor.class);

    public PostgreSqlSqlExecutor(DataSource ds) {
        super(ds);
    }

    @Override
    public void setDataSourceSchema(Connection conn, PhysicalSchema schema) {
        // NOTE - SET SCHEMA 'schemaName' (with quotes) is only effective for PostgreSQL versions >= 8.4
        // For 8.3, we must use SET search_path TO schemaName (without quotes)
        // This is compatible w/ future versions as well; hence, we keep it
        // (unfortunately, can't easily bring up version 8.3 on an app server environment for easy testing)
        JdbcHelper jdbc = this.getJdbcTemplate();
        jdbc.update(conn, "SET search_path TO " + schema.getPhysicalName());
    }
}
