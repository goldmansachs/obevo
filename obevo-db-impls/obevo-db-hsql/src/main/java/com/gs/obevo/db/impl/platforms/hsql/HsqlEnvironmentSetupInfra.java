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
package com.gs.obevo.db.impl.platforms.hsql;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.impl.core.envinfrasetup.AbstractEnvironmentInfraSetup;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HsqlEnvironmentSetupInfra extends AbstractEnvironmentInfraSetup {
    private static final Logger LOG = LoggerFactory.getLogger(HsqlEnvironmentSetupInfra.class);

    HsqlEnvironmentSetupInfra(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector, DbMetadataManager dbMetadataManager, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        super(env, ds, deployMetricsCollector, dbMetadataManager, changeTypeBehaviorRegistry);
    }

    @Override
    protected void createSchema(Connection conn, PhysicalSchema schema) {
        jdbc.update(conn, "CREATE SCHEMA " + schema.getPhysicalName() + " AUTHORIZATION DBA");
    }

    @Override
    protected void createGroup(Connection conn, Group group, PhysicalSchema physicalSchema) {
        jdbc.update(conn, "CREATE ROLE " + group.getName());
    }

    @Override
    protected void createUser(Connection conn, User user, PhysicalSchema physicalSchema) {
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
