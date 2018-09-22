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
package com.gs.obevo.db.impl.platforms.hsql

import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.appdata.Group
import com.gs.obevo.db.api.appdata.User
import com.gs.obevo.db.impl.core.envinfrasetup.AbstractEnvironmentInfraSetup
import com.gs.obevo.dbmetadata.api.DbMetadataManager
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry
import com.gs.obevo.impl.DeployMetricsCollector
import java.sql.Connection
import javax.sql.DataSource

internal class HsqlEnvironmentSetupInfra(env: DbEnvironment, ds: DataSource, deployMetricsCollector: DeployMetricsCollector, dbMetadataManager: DbMetadataManager, changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry) : AbstractEnvironmentInfraSetup(env, ds, deployMetricsCollector, dbMetadataManager, changeTypeBehaviorRegistry) {

    override fun createSchema(conn: Connection, schema: PhysicalSchema) {
        jdbc.update(conn, "CREATE SCHEMA " + schema.physicalName + " AUTHORIZATION DBA")
    }

    override fun createGroup(conn: Connection, group: Group, physicalSchema: PhysicalSchema?) {
        jdbc.update(conn, "CREATE ROLE " + group.name)
    }

    override fun createUser(conn: Connection, user: User, physicalSchema: PhysicalSchema?) {
        val sb = StringBuilder()

        val password = if (user.password != null) user.password else "dummypwd"

        sb.append("CREATE USER \"").append(user.name).append("\"")
        sb.append(" PASSWORD \"").append(password).append("\"")
        if (user.isAdmin) {
            sb.append(" ADMIN")
        }
        jdbc.update(conn, sb.toString())
    }
}
