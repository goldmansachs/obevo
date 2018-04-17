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
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionAttributeImpl;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class Db2DeployerMainIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return Db2ParamReader.getParamReader().getAppContextAndJdbcDsParams();
    }

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;
    private final DataSource ds;

    public Db2DeployerMainIT(IntToObjectFunction<DbDeployerAppContext> getAppContext, DataSource ds) {
        this.getAppContext = getAppContext;
        this.ds = ds;
    }

    @Test
    public void testDeploy() throws Exception {
        int stepsToRun = 3;  // this toggle is here to help w/ local testing

        DbDeployerAppContext dbDeployerAppContext = null;

        if (stepsToRun >= 0) {
            System.out.println("Running step 0");
            dbDeployerAppContext = getAppContext.valueOf(1);
            dbDeployerAppContext
                    .cleanEnvironment()
                    .setupEnvInfra()
                    .deploy(new MainDeployerArgs().reason("try1").deployExecutionAttributes(Sets.immutable.with(
                            new DeployExecutionAttributeImpl("A", "aval"),
                            new DeployExecutionAttributeImpl("B", "bval")
                    )))
            ;
        }

        if (stepsToRun >= 1) {
            System.out.println("Running step 1");
            dbDeployerAppContext = getAppContext.valueOf(1);
            dbDeployerAppContext
                    .deploy(new MainDeployerArgs().reason("try1a-noop").deployExecutionAttributes(Sets.immutable.with(
                            new DeployExecutionAttributeImpl("A", "aval"),
                            new DeployExecutionAttributeImpl("B", "bval"),
                            new DeployExecutionAttributeImpl("C", "cval")
                    )))
            ;
        }

        if (stepsToRun >= 2) {
            System.out.println("Running step 2");
            dbDeployerAppContext = getAppContext.valueOf(2);
            dbDeployerAppContext
                    .setupEnvInfra()
                    .deploy(new MainDeployerArgs().reason("try2").deployExecutionAttributes(Sets.immutable.with(
                            new DeployExecutionAttributeImpl("C", "cval2"),
                            new DeployExecutionAttributeImpl("E", "eval")
                    )))
            ;
        }

        if (stepsToRun >= 3) {
            System.out.println("Running step 3");
            dbDeployerAppContext = getAppContext.valueOf(3);
            dbDeployerAppContext
                    .setupEnvInfra()
                    .deploy(new MainDeployerArgs().reason("try3").deployExecutionAttributes(Sets.immutable.with(
                            new DeployExecutionAttributeImpl("F", "fval")
                    )))
            ;
        }

        String schema1 = "DEPLOY_TRACKER";
        MutableList<DeployExecution> executions = dbDeployerAppContext.getDeployExecutionDao().getDeployExecutions(schema1).toSortedListBy(DeployExecution::getId);
        assertThat(executions, hasSize(4));
        DeployExecution execution4 = dbDeployerAppContext.getDeployExecutionDao().getLatestDeployExecution(schema1);
        verifyExecution1(executions.get(0));
        verifyExecution1a(executions.get(1));
        verifyExecution2(executions.get(2));
        verifyExecution3(executions.get(3));
        verifyExecution3(execution4);

        JdbcHelper db2JdbcTemplate = new JdbcHelper();

        // Assert the columns which are available in table TEST_TABLE
        String schema = dbDeployerAppContext.getEnvironment().getPhysicalSchema("DEPLOY_TRACKER").getPhysicalName();
        String columnListSql = "select colname from syscat.COLUMNS where tabschema = '" + schema + "' AND tabname = 'TABLE_D'";
        Connection conn = ds.getConnection();
        try {
            List<String> columnsInTestTable = db2JdbcTemplate.query(conn, columnListSql, new ColumnListHandler<String>());
            Assert.assertEquals(Lists.mutable.with("D_ID"), FastList.newList(columnsInTestTable));
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private void verifyExecution1(DeployExecution execution1) {
        assertEquals("try1", execution1.getReason());
        assertEquals(
                Sets.immutable.with(
                        new DeployExecutionAttributeImpl("A", "aval"),
                        new DeployExecutionAttributeImpl("B", "bval")
                ),
                execution1.getAttributes());
    }

    private void verifyExecution1a(DeployExecution execution1) {
        assertEquals("try1a-noop", execution1.getReason());
        assertEquals(
                Sets.immutable.with(
                        new DeployExecutionAttributeImpl("A", "aval"),
                        new DeployExecutionAttributeImpl("B", "bval"),
                        new DeployExecutionAttributeImpl("C", "cval")
                ),
                execution1.getAttributes());
    }

    private void verifyExecution2(DeployExecution execution2) {
        assertEquals("try2", execution2.getReason());
        assertEquals(
                Sets.immutable.with(
                        new DeployExecutionAttributeImpl("C", "cval2"),
                        new DeployExecutionAttributeImpl("E", "eval")
                ),
                execution2.getAttributes());
    }

    private void verifyExecution3(DeployExecution execution3) {
        assertEquals("try3", execution3.getReason());
        assertEquals(
                Sets.immutable.with(
                        new DeployExecutionAttributeImpl("F", "fval")
                ),
                execution3.getAttributes());
    }
}
