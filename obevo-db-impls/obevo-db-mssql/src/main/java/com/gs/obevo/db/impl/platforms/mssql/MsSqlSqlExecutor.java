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
package com.gs.obevo.db.impl.platforms.mssql;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.AbstractSqlExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MsSqlSqlExecutor extends AbstractSqlExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(MsSqlSqlExecutor.class);

    public MsSqlSqlExecutor(DataSource ds) {
        super(ds);
    }

    @Override
    public void setDataSourceSchema(Connection conn, PhysicalSchema schema) {
        JdbcHelper jdbc = this.getJdbcTemplate();
        jdbc.update(conn, "use " + schema.getPhysicalName());
    }
}
