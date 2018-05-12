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
package com.gs.obevo.db.impl.platforms.sybaseiq

import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.platform.SqlExecutor
import com.gs.obevo.db.impl.core.jdbc.DataAccessException
import com.gs.obevo.impl.PostDeployAction
import org.apache.commons.dbutils.handlers.ColumnListHandler
import org.slf4j.LoggerFactory

/**
 * Recompiling views after deployments as this is causing issues often in IQ.
 */
internal class IqPostDeployAction(private val dataSourceForEditsFactory: SqlExecutor) : PostDeployAction<DbEnvironment> {

    override fun value(env: DbEnvironment) {
        LOG.info("Recompiling views for Sybase IQ...")
        for (i in 0..1) {
            env.physicalSchemas.forEach { physicalSchema ->
                dataSourceForEditsFactory.executeWithinContext(physicalSchema) { conn ->
                    val jdbcTemplate = dataSourceForEditsFactory.jdbcTemplate

                    val viewRecompiles = jdbcTemplate.query(conn,
                            "SELECT 'ALTER VIEW ' || vcreator || '.' || viewname || ' RECOMPILE' FROM sys.SYSVIEWS WHERE lcase(vcreator) = '${physicalSchema.physicalName}'",
                            ColumnListHandler<String>())

                    viewRecompiles.forEach {
                        try {
                            jdbcTemplate.update(conn, it)
                        } catch (e: DataAccessException) {
                            LOG.info("Could not recompile query [{}] - skipping as we rely on executing a couple times to fix all views. Message was: {}", it, e.message)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IqPostDeployAction::class.java)
    }
}
