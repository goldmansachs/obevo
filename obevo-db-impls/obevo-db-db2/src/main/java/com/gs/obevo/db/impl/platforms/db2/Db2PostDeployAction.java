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
package com.gs.obevo.db.impl.platforms.db2;

import java.sql.Connection;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.PostDeployAction;
import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs standard health checks on a DB2 environments. e.g.
 * <ul>
 *     <li>Checking for invalid views</li>
 *     <li>Checking for tables requiring reorg</li>
 * </ul>
 */
public class Db2PostDeployAction implements PostDeployAction<DbEnvironment> {
    private static final Logger LOG = LoggerFactory.getLogger(Db2PostDeployAction.class);

    static final String POST_DEPLOY_WARNINGS = DeployMetrics.WARNINGS_PREFIX + ".postDeployWarnings";
    private final Db2SqlExecutor stmtExecutor;
    private final DeployMetricsCollector deployMetricsCollector;

    public Db2PostDeployAction(Db2SqlExecutor stmtExec, DeployMetricsCollector deployMetricsCollector) {
        this.stmtExecutor = stmtExec;
        this.deployMetricsCollector = deployMetricsCollector;
    }

    @Override
    public void value(final DbEnvironment env) {
        // TODO would prefer to avoid this hack w/ the "executeWithinContext" here and picking a schema arbitrarily
        stmtExecutor.executeWithinContext(env.getPhysicalSchemas().getFirst(), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                // TODO refactor these into separate components that can be chained together (GITHUB#5)

                // TODO measure how long each of these takes: GITHUB#6
                if (env.isInvalidObjectCheckEnabled()) {
                    checkForInvalidObjects(conn, env.getPhysicalSchemas());
                } else {
                    LOG.info("Skipping invalid object check as configured in your environment");
                }

                if (env.isReorgCheckEnabled()) {
                    checkForTablesNeedingReorg(conn, env);
                } else {
                    LOG.info("Skipping check for tables pending reorg as configured in your environment");
                }
                LOG.info("Done in DB2 post-deploy action:");
            }
        });
    }

    @VisibleForTesting
    void checkForInvalidObjects(Connection conn, RichIterable<PhysicalSchema> physicalSchemas) {
        MutableList<ReorgQueryResult> invalidObjects = this.getInvalidObjects(conn, physicalSchemas);
        if (!invalidObjects.isEmpty()) {
            LOG.info("Found invalid objects, will attempt to recompile: {}", invalidObjects);
            recompileInvalidObjects(physicalSchemas);
        }
    }

    @VisibleForTesting
    MutableList<ReorgQueryResult> getInvalidObjects(Connection conn, RichIterable<PhysicalSchema> physicalSchemas) {
        LOG.info("Checking for invalid objects");

        String schemaInClause = physicalSchemas.collect(PhysicalSchema.TO_PHYSICAL_NAME).makeString("('", "','", "')");

        try {
            String sql = "SELECT OBJECTSCHEMA schema, OBJECTNAME name FROM SYSCAT.INVALIDOBJECTS WHERE OBJECTSCHEMA IN " + schemaInClause;
            return ListAdapter.adapt(this.stmtExecutor.getJdbcTemplate().query(conn, sql, new BeanListHandler<ReorgQueryResult>(ReorgQueryResult.class)));
        } catch (DataAccessException e) {
            LOG.debug("Failed to execute new invalid objects SQL; falling back to old query");
            deployMetricsCollector.addMetric("oldInvalidObjectQueryRequired", true);
            String sql = "SELECT CREATOR schema, NAME name FROM SYSIBM.SYSTABLES WHERE TYPE = 'V' AND STATUS = 'X' AND CREATOR IN " + schemaInClause;
            return ListAdapter.adapt(this.stmtExecutor.getJdbcTemplate().query(conn, sql, new BeanListHandler<ReorgQueryResult>(ReorgQueryResult.class)));
        }
    }

    private void recompileInvalidObjects(RichIterable<PhysicalSchema> physicalSchemas) {
        MutableList<String> warnings = Lists.mutable.empty();
        for (final PhysicalSchema physicalSchema : physicalSchemas) {
            try {
                this.stmtExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
                    @Override
                    public void value(Connection conn) {
                        stmtExecutor.getJdbcTemplate().update(conn, "CALL SYSPROC.ADMIN_REVALIDATE_DB_OBJECTS(NULL, '" + physicalSchema.getPhysicalName() + "', NULL)");
                    }
                });
                LOG.info("Successfully recompiled objects in schema", physicalSchema);
            } catch (DataAccessException e) {
                warnings.add(physicalSchema.getPhysicalName() + ": " + e.getMessage());
                LOG.warn("Failed to recompile objects on schema {}; will not fail the overall deployment due to this", physicalSchema, e);
            }
        }

        if (warnings.notEmpty()) {
            deployMetricsCollector.addMetric(POST_DEPLOY_WARNINGS, "Failures on recompiling invalid objects: " + warnings.makeString("\n"));
        }
    }

    private void checkForTablesNeedingReorg(Connection conn, DbEnvironment env) {
        RichIterable<ReorgQueryResult> results = this.getTablesNeedingReorg(conn, env);

        if (results.isEmpty()) {
            LOG.info("No tables to reorg.");
        } else {
            LOG.info("The following tables require reorgs:");
            for (ReorgQueryResult result : results) {
                LOG.info("* " + result.getPhysicalSchema() + "." + result.getName());
            }

            if (env.isAutoReorgEnabled()) {
                LOG.info("autoReorg is enabled; executing the reorgs now...");
                for (ReorgQueryResult result : results) {
                    LOG.info("Reorging table: " + result.getPhysicalSchema() + "." + result.getName());
                    Db2SqlExecutor.executeReorg(this.stmtExecutor.getJdbcTemplate(), conn, result.getPhysicalSchema(), result.getName());
                }
            } else {
                LOG.warn("autoReorg is disabled; please remember to manually execute reorgs on these tables: {}", results);
            }
        }
    }

    ImmutableSet<ReorgQueryResult> getTablesNeedingReorg(final Connection conn, final DbEnvironment env) {
        // keeping as system.out for now to facilitate output to maven output
        LOG.info("Starting DB2 post-deploy action: Querying for tables in reorg-ending state (this may take a minute)");
        // trim the schema as DB2 seems to sometimes return the schema w/ spaces (even though where clauses can still
        // work w/out the spaces)

        try {
            return env.getPhysicalSchemas().flatCollect(new Function<PhysicalSchema, Iterable<ReorgQueryResult>>() {
                @Override
                public Iterable<ReorgQueryResult> valueOf(PhysicalSchema physicalSchema) {
                    final String sql = String.format(
                            "select '%1$s' schema, trim(TABNAME) name, NUM_REORG_REC_ALTERS, REORG_PENDING\n" +
                                    "FROM TABLE (SYSPROC.ADMIN_GET_TAB_INFO('%1$s', null)) WHERE REORG_PENDING = 'Y'"
                            , physicalSchema.getPhysicalName());
                    LOG.debug("Executing SQL: " + sql);

                    return stmtExecutor.getJdbcTemplate()
                            .query(conn, sql, new BeanListHandler<ReorgQueryResult>(ReorgQueryResult.class));
                }
            });
        } catch (RuntimeException e) {
            // TODO would like a better way to decide on using this SQL apart from catching the exception
            LOG.info("Query in new >= 9.7 syntax didn't work: {} (debug log shows the full stack trace). Falling back to older (and slower) syntax", e.getMessage());
            LOG.debug("Full exception stack trace", e);
            String sql = "SELECT TRIM(TABSCHEMA) schema, TABNAME name, NUM_REORG_REC_ALTERS, REORG_PENDING\n" +
                    "FROM SYSIBMADM.ADMINTABINFO WHERE REORG_PENDING = 'Y'\n" +
                    "AND TABSCHEMA IN ('" + env.getPhysicalSchemas().makeString("','") + "')";
            return CollectionAdapter.wrapSet(Db2PostDeployAction.this.stmtExecutor.getJdbcTemplate()
                    .query(conn, sql, new BeanListHandler<ReorgQueryResult>(ReorgQueryResult.class))).toImmutable();
        }
    }

    public static class ReorgQueryResult {
        public static final Function<ReorgQueryResult, String> TO_SCHEMA = new Function<ReorgQueryResult, String>() {
            @Override
            public String valueOf(ReorgQueryResult object) {
                return object.getSchema();
            }
        };
        public static final Function<ReorgQueryResult, String> TO_NAME = new Function<ReorgQueryResult, String>() {
            @Override
            public String valueOf(ReorgQueryResult object) {
                return object.getName();
            }
        };
        private String schema;
        private String name;
        private String objecttype;

        public PhysicalSchema getPhysicalSchema() {
            return PhysicalSchema.parseFromString(this.schema);
        }

        public String getSchema() {
            return this.schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getObjecttype() {
            return objecttype;
        }

        public void setObjecttype(String objecttype) {
            this.objecttype = objecttype;
        }

        @Override
        public String toString() {
            return this.schema + "." + this.name;
        }
    }
}
