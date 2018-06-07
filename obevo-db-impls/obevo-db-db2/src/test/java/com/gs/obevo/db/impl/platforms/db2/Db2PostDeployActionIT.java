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

import java.io.Serializable;
import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.platforms.db2.Db2PostDeployAction.SchemaObjectRow;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Matchers;
import org.mockito.exceptions.verification.WantedButNotInvoked;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class Db2PostDeployActionIT {
    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return Db2ParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    private final DataSource dataSource;
    private final PhysicalSchema physicalSchema;
    private Db2SqlExecutor sqlExecutor;
    private Db2PostDeployAction db2PostDeployAction;
    private DbEnvironment env;
    private DeployMetricsCollector metricsCollector;

    public Db2PostDeployActionIT(DataSource dataSource, PhysicalSchema physicalSchema) {
        this.dataSource = dataSource;
        this.physicalSchema = physicalSchema;
    }

    @Before
    public void setup() {
        this.env = new DbEnvironment();
        this.env.setSchemas(Sets.immutable.with(new Schema(physicalSchema.getPhysicalName())));

        this.sqlExecutor = new Db2SqlExecutor(dataSource, env);

        this.metricsCollector = mock(DeployMetricsCollector.class);
        this.db2PostDeployAction = new Db2PostDeployAction(sqlExecutor, metricsCollector);
    }

    @Test
    public void checkForInvalidViews() throws Exception {
        sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                // Setup the invalid objects
                try {
                    sqlExecutor.getJdbcTemplate().update(conn, "drop table INVALIDTEST_TABLE");
                } catch (DataAccessException ignore) {
                    // ignore exceptions on dropping
                }
                sqlExecutor.getJdbcTemplate().update(conn, "create table INVALIDTEST_TABLE (a INT)");
                sqlExecutor.getJdbcTemplate().update(conn, "create or replace view INVALIDTEST_VIEW AS SELECT * FROM INVALIDTEST_TABLE");
                sqlExecutor.getJdbcTemplate().update(conn, "create or replace view INVALIDTEST_VIEW2 AS SELECT * FROM INVALIDTEST_VIEW WHERE 1=2");
                sqlExecutor.getJdbcTemplate().update(conn, "drop table INVALIDTEST_TABLE");

                MutableSet<String> invalidObjects = db2PostDeployAction.getInvalidObjects(conn, env.getPhysicalSchemas()).collect(new Function<SchemaObjectRow, String>() {
                    @Override
                    public String valueOf(SchemaObjectRow schemaObjectRow) {
                        return schemaObjectRow.getName();
                    }
                }).toSet();
                assertThat("The two views created should go invalid when we drop the table that they are based on",
                        invalidObjects, hasItems("INVALIDTEST_VIEW", "INVALIDTEST_VIEW2"));

                // Check that the query can return invalid objects
                db2PostDeployAction.checkForInvalidObjects(conn, env.getPhysicalSchemas());

                // With this DB2 version, verify that we did try to execute the recompile and that if it fails (which we expect to in this case) that we log a warning
                // (It is hard to simulate a case where a recopmile will fix things, compared to DB2's auto-recompile)
                try {
                    verify(metricsCollector, times(1)).addMetric(Matchers.eq(Db2PostDeployAction.POST_DEPLOY_WARNINGS), Matchers.<Serializable>any());
                } catch (WantedButNotInvoked e) {
                    Assume.assumeNoException("Expecting view to be invalid, but was not in this case", e);
                }
            }
        });
    }

    @After
    public void teardown() {
        sqlExecutor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                try {
                    sqlExecutor.getJdbcTemplate().update(conn, "drop view INVALIDTEST_VIEW2");
                } catch (DataAccessException ignore) {
                    // ignore exceptions on dropping
                }
                try {
                    sqlExecutor.getJdbcTemplate().update(conn, "drop view INVALIDTEST_VIEW");
                } catch (DataAccessException ignore) {
                    // ignore exceptions on dropping
                }
                try {
                    sqlExecutor.getJdbcTemplate().update(conn, "drop table INVALIDTEST_TABLE");
                } catch (DataAccessException ignore) {
                    // ignore exceptions on dropping
                }
            }
        });
    }
}