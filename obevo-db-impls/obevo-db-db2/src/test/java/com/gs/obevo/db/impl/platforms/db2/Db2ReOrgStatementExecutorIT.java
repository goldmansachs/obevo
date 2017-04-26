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
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.db2.Db2PostDeployAction.ReorgQueryResult;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class Db2ReOrgStatementExecutorIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return Db2ParamReader.getParamReader().getJdbcDsAndSchemaParams(2);
    }

    private final DataSource db2DataSource;
    private final String schemaName;

    private final PhysicalSchema physicalSchema;
    private Db2SqlExecutor executor;
    private JdbcHelper executorJdbc;
    private JdbcHelper db2JdbcTemplate;
    private DbEnvironment environment;

    public Db2ReOrgStatementExecutorIT(DataSource db2DataSource, String schemaName) {
        this.db2DataSource = db2DataSource;
        this.schemaName = schemaName;
        this.physicalSchema = new PhysicalSchema(schemaName);
    }

    private void setupExecutor(boolean autoReorg) {
        this.db2JdbcTemplate = new JdbcHelper();

        this.environment = new DbEnvironment();
        this.environment.setSchemas(Sets.immutable.with(new Schema(schemaName)));
        this.environment.setAutoReorgEnabled(autoReorg);
        this.executor = new Db2SqlExecutor(db2DataSource, this.environment);

        executorJdbc = this.executor.getJdbcTemplate();
    }

    @Test
    @Ignore
    public void testReorgPendingSql() {
        this.setupExecutor(false);

        this.executor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                createTable(conn, "A", true);
                createTable(conn, "B", true);
                createTable(conn, "C", true);
                createTable(conn, "D", false);
            }
        });

        final Db2PostDeployAction postDeployAction = new Db2PostDeployAction(executor, new DeployMetricsCollectorImpl());
        MutableList<ReorgQueryResult> tables = executor.executeWithinContext(physicalSchema, new ThrowingFunction<Connection, MutableList<ReorgQueryResult>>() {
            @Override
            public MutableList<ReorgQueryResult> safeValueOf(Connection conn) throws Exception {
                return postDeployAction.getTablesNeedingReorg(conn, environment)
                        .toList().sortThis(Comparators.fromFunctions(ReorgQueryResult.TO_SCHEMA, ReorgQueryResult.TO_NAME));
            }
        });

        assertEquals(3, tables.size());
        assertEquals("A", tables.get(0).getName());
        assertEquals("B", tables.get(1).getName());
        assertEquals("C", tables.get(2).getName());

        postDeployAction.value(environment);
    }

    @Test
    public void testReorgExecution20054() {
        this.performReorgExecution(true, 20054);
    }

    @Test
    public void testReorgExecution20054Disabled() {
        this.performReorgExecution(false, 20054);
    }

    @Test
    public void testReorgExecution668() {
        this.performReorgExecution(true, 668);
    }

    @Test
    public void testReorgExecution668Disabled() {
        this.performReorgExecution(false, 668);
    }

    private void performReorgExecution(final boolean autoReorgEnabled, final int errorCode) {
        this.setupExecutor(autoReorgEnabled);
        this.executor.executeWithinContext(physicalSchema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                try {
                    executorJdbc.update(conn, "DROP TABLE a");
                } catch (Exception ignore) {
                    // Ignoring the exception, as no clear "DROP TABLE IF EXISTS" is
                    // available in DB2
                }

                executorJdbc.update(conn, "create table a (a integer, b integer, c integer, d integer, e integer) ");
                executorJdbc.update(conn, "insert into a (a) values (3)");
                executorJdbc.update(conn, "alter table a drop column b");
                executorJdbc.update(conn, "alter table a drop column c");
                executorJdbc.update(conn, "alter table a drop column d");

                MutableSet<String> expectedColumns = null;
                try {
                    // this next statement will fire a reorg
                    switch (errorCode) {
                    case 668:
                        expectedColumns = Sets.mutable.with("A", "E");
                        executorJdbc.update(conn, "insert into a (a) values (5)");
                        break;
                    case 20054:
                        expectedColumns = Sets.mutable.with("A");
                        executorJdbc.update(conn, "alter table a drop column e");
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported error code for this test: " + errorCode);
                    }

                    if (!autoReorgEnabled) {
                        fail("Expected an exception here if we do not have autoReorgEnabled");
                    }
                } catch (RuntimeException e) {
                    if (autoReorgEnabled) {
                        fail("If reorg is enabled, then we should not have thrown an exception here: " + e.getMessage());
                    } else {
                        return;
                    }
                }

                // Assert the columns which are available in table A
                String columnListSql = "select colname from syscat.COLUMNS where tabschema = '" + schemaName + "' AND tabname = 'A'";
                List<String> columnsInTableA = db2JdbcTemplate.query(conn, columnListSql,
                        new ColumnListHandler<String>());
                assertEquals(expectedColumns, Sets.mutable.withAll(columnsInTableA));
            }
        });
    }

    private void createTable(Connection conn, String tableName, boolean requireReorg) {
        try {
            executorJdbc.update(conn, "DROP TABLE " + tableName);
        } catch (Exception ignore) {
            // Ignoring the exception, as no clear "DROP TABLE IF EXISTS" is
            // available in DB2
        }

        executorJdbc.update(conn, "create table " + tableName + " (a integer, b integer, c integer, d integer, e integer) ");
        executorJdbc.update(conn, "insert into " + tableName + " (a) values (3)");
        MutableList<String> expectedColumns;
        if (requireReorg) {
            executorJdbc.update(conn, "alter table " + tableName + " drop column b");
            executorJdbc.update(conn, "alter table " + tableName + " drop column c");
            executorJdbc.update(conn, "alter table " + tableName + " drop column d");
            expectedColumns = Lists.mutable.with("A", "E");
        } else {
            expectedColumns = Lists.mutable.with("A", "B", "C", "D", "E");
        }
        // Assert the columns which are available in table A
        String columnListSql = "select colname from syscat.COLUMNS where tabschema = '" + schemaName + "' AND tabname = '"
                + tableName + "'";
        List<String> columnsInTableA = this.db2JdbcTemplate.query(conn, columnListSql,
                new ColumnListHandler<String>());
        assertEquals(expectedColumns, FastList.newList(columnsInTableA));
    }
}
