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
package com.gs.obevo.db.impl.platforms.hsql;

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
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HsqlEnvironmentSetupInfra implements EnvironmentInfraSetup<DbEnvironment> {
    private static final Logger LOG = LoggerFactory.getLogger(HsqlEnvironmentSetupInfra.class);

    private final DbEnvironment env;
    private final DataSource ds;
    private final DbMetadataManager dbMetadataManager;

    public HsqlEnvironmentSetupInfra(DbEnvironment env, DataSource ds, DbMetadataManager dbMetadataManager) {
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
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                if (!isSchemaAlreadySetup(schema)) {
                    LOG.info("Creating schema {}", schema.getPhysicalName());
                    jdbc.update(conn, "CREATE SCHEMA " + schema.getPhysicalName() + " AUTHORIZATION DBA");
                }
            }

            ImmutableSet<String> existingGroups = Sets.immutable.withAll(
                    jdbc.query(conn, "select ROLE_NAME from INFORMATION_SCHEMA.APPLICABLE_ROLES", new ColumnListHandler<String>()))
                    .collect(StringFunctions.toLowerCase());

            for (Group group : env.getGroups()) {
                if (!groupAlreadySetup(group, existingGroups)) {
                    jdbc.update(conn, "CREATE ROLE " + group.getName());
                }
            }

            ImmutableSet<String> existingUsers = Sets.immutable.withAll(
                    jdbc.query(conn, "select USER_NAME from INFORMATION_SCHEMA.SYSTEM_USERS", new ColumnListHandler<String>()))
                    .collect(StringFunctions.toLowerCase());

            for (User user : env.getUsers()) {
                if (!isUserAlreadySetup(existingUsers, user)) {
                    StringBuilder sb = new StringBuilder();

                    String password = user.getPassword() != null ? user.getPassword() : "dummypwd";

                    sb.append("CREATE USER \"").append(user.getName()).append("\"");
                    sb.append(" PASSWORD \"").append(password).append("\"");
                    if (user.isAdmin()) {
                        sb.append(" ADMIN");
                    }
                    jdbc.update(conn, sb.toString());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private boolean isSchemaAlreadySetup(PhysicalSchema physicalSchema) {
        DaCatalog schemaInfo = this.dbMetadataManager.getDatabase(physicalSchema.getPhysicalName());
        return schemaInfo != null;
    }

    private boolean isUserAlreadySetup(ImmutableSet<String> existingUsers, User user) {
        return existingUsers.contains(user.getName().toLowerCase());
    }

    private boolean groupAlreadySetup(Group group, ImmutableSet<String> existingGroups) {
        return existingGroups.contains(group.getName().toLowerCase());
    }
}
