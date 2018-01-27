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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import java.sql.Connection;
import java.util.List;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.impl.PostDeployAction;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recompiling views after deployments as this is causing issues often in IQ.
 */
class IqPostDeployAction implements PostDeployAction<DbEnvironment> {
    private static final Logger LOG = LoggerFactory.getLogger(IqPostDeployAction.class);

    private final SqlExecutor dataSourceForEditsFactory;

    IqPostDeployAction(SqlExecutor dataSourceForEditsFactory) {
        this.dataSourceForEditsFactory = dataSourceForEditsFactory;
    }

    @Override
    public void value(DbEnvironment env) {
        LOG.info("Recompiling views for Sybase IQ...");
        for (int i = 0; i < 2; i++) {
            for (final PhysicalSchema physicalSchema : env.getPhysicalSchemas()) {
                dataSourceForEditsFactory.executeWithinContext(physicalSchema, new Procedure<Connection>() {
                    @Override
                    public void value(Connection conn) {
                        JdbcHelper jdbcTemplate =
                                dataSourceForEditsFactory.getJdbcTemplate();

                        List<String> viewRecompiles = jdbcTemplate.query(
                                conn, "SELECT 'ALTER VIEW ' || vcreator || '.' || viewname || ' RECOMPILE' FROM sys.SYSVIEWS WHERE lcase(vcreator) = '"
                                        + physicalSchema.getPhysicalName() + "'", new ColumnListHandler<String>());

                        for (String viewRecompile : viewRecompiles) {
                            try {
                                jdbcTemplate.update(conn, viewRecompile);
                            } catch (DataAccessException e) {
                                LOG.info("Could not recompile query [{}] - skipping as we rely on executing a couple times to fix all views. Message was: {}", viewRecompile, e.getMessage());
                            }
                        }
                    }
                });
            }
        }
    }
}
