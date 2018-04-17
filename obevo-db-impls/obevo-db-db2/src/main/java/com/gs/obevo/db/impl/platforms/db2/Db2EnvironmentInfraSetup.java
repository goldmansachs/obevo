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
package com.gs.obevo.db.impl.platforms.db2;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.envinfrasetup.AbstractEnvironmentInfraSetup;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the groups specified in the system configuration actually exist in the database.
 *
 * Note that user verification cannot really be done as DB2 does not define it. http://www.dbforums.com/showthread.php?1645929-Finding-Users-and-Groups
 *
 * For now, we rely on an approximation based on the sysibm.SYSROLES and sysibm.SYSDBAUTH tables.
 */
class Db2EnvironmentInfraSetup extends AbstractEnvironmentInfraSetup {
    private static final Logger LOG = LoggerFactory.getLogger(Db2EnvironmentInfraSetup.class);

    Db2EnvironmentInfraSetup(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector, DbMetadataManager dbMetadataManager, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        super(env, ds, deployMetricsCollector, dbMetadataManager, changeTypeBehaviorRegistry);
    }

    @Override
    protected void createSchema(Connection conn, PhysicalSchema schema) {
        jdbc.update(conn, "CREATE SCHEMA " + schema.getPhysicalName());
    }
}
