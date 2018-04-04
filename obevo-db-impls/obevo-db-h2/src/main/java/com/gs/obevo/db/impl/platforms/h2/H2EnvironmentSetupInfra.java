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
package com.gs.obevo.db.impl.platforms.h2;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.impl.core.envinfrasetup.AbstractEnvironmentInfraSetup;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;

class H2EnvironmentSetupInfra extends AbstractEnvironmentInfraSetup {
    private final DbMetadataManager dbMetadataManager;

    H2EnvironmentSetupInfra(DbEnvironment env, DataSource ds, DbMetadataManager dbMetadataManager) {
        super(env, ds, null, dbMetadataManager);
        this.dbMetadataManager = dbMetadataManager;
    }

    @Override
    protected void createSchema(Connection conn, PhysicalSchema schema) {
        jdbc.update(conn, "CREATE SCHEMA " + schema.getPhysicalName());
    }

    @Override
    protected void createGroup(Connection conn, Group group, PhysicalSchema physicalSchema) {
        jdbc.update(conn, "CREATE ROLE IF NOT EXISTS " + group.getName());
    }

    @Override
    protected void createUser(Connection conn, User user, PhysicalSchema physicalSchema) {
        String password = user.getPassword() != null ? user.getPassword() : "dummypwd";
        jdbc.update(conn, "CREATE USER IF NOT EXISTS " + user.getName() + " PASSWORD '" + password + "'");
    }
}
