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
package com.gs.obevo.db.impl.platforms.h2;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.dbutils.DbUtils;

public class H2EnvironmentSetupInfra implements EnvironmentInfraSetup<DbEnvironment> {
    private final DbEnvironment env;
    private final DataSource ds;
    private final DbMetadataManager dbMetadataManager;

    public H2EnvironmentSetupInfra(DbEnvironment env, DataSource ds, DbMetadataManager dbMetadataManager) {
        this.env = env;
        this.ds = ds;
        this.dbMetadataManager = dbMetadataManager;
    }

    @Override
    public void setupEnvInfra(boolean failOnSetupException) {
        JdbcHelper jdbc = new JdbcHelper();

        Connection conn = null;
        try {
            conn = ds.getConnection();
            // now setup the base infrastructure (schemas + roles)
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                DaCatalog schemaInfo = this.dbMetadataManager.getDatabase(schema.getPhysicalName());
                if (schemaInfo == null) {
                    jdbc.update(conn, "CREATE SCHEMA " + schema.getPhysicalName());
                }
            }

            for (Group group : env.getGroups()) {
                jdbc.update(conn, "CREATE ROLE IF NOT EXISTS " + group.getName());
            }
            for (User user : env.getUsers()) {
                String password = user.getPassword() != null ? user.getPassword() : "dummypwd";
                jdbc.update(conn, "CREATE USER IF NOT EXISTS " + user.getName() + " PASSWORD '" + password + "'");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }
}
