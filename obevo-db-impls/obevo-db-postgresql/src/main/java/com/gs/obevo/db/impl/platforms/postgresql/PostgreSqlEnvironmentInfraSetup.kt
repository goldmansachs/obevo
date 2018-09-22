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
package com.gs.obevo.db.impl.platforms.postgresql

import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.appdata.Extension
import com.gs.obevo.db.api.appdata.Group
import com.gs.obevo.db.impl.core.envinfrasetup.AbstractEnvironmentInfraSetup
import com.gs.obevo.dbmetadata.api.DbMetadataManager
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry
import com.gs.obevo.impl.DeployMetricsCollector
import java.sql.Connection
import javax.sql.DataSource

/**
 * Setup the ASE schema group and users. Logic for finding groups is moved to the metadata dialect class.
 */
internal class PostgreSqlEnvironmentInfraSetup(env: DbEnvironment, ds: DataSource, deployMetricsCollector: DeployMetricsCollector, dbMetadataManager: DbMetadataManager, changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry) : AbstractEnvironmentInfraSetup(env, ds, deployMetricsCollector, dbMetadataManager, changeTypeBehaviorRegistry) {

    override fun createSchema(conn: Connection, schema: PhysicalSchema) {
        jdbc.update(conn, "CREATE SCHEMA " + schema.physicalName)
    }

    override fun createGroup(conn: Connection, group: Group, physicalSchema: PhysicalSchema?) {
        jdbc.update(conn, "CREATE ROLE " + group.name + " NOLOGIN")
    }

    override fun createExtension(conn: Connection, extension: Extension) {
        jdbc.update(conn, "CREATE EXTENSION \"" + extension.name + "\"")
    }
}
