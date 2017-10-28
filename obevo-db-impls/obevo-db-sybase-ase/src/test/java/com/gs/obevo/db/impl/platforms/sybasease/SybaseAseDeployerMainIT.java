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
package com.gs.obevo.db.impl.platforms.sybasease;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import com.gs.obevo.api.appdata.DeployExecutionAttributeImpl;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SybaseAseDeployerMainIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return AseParamReader.getParamReader().getAppContextAndJdbcDsParams();
    }

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;
    private final DataSource ds;

    public SybaseAseDeployerMainIT(IntToObjectFunction<DbDeployerAppContext> getAppContext, DataSource ds) {
        this.getAppContext = getAppContext;
        this.ds = ds;
    }

    @Test
    public void testAseDeploy() throws Exception {
        MainDeployerArgs args1 = new MainDeployerArgs()
                .deployExecutionAttributes(Sets.immutable.<DeployExecutionAttribute>with(
                        new DeployExecutionAttributeImpl("attr1", "v1_val1"),
                        new DeployExecutionAttributeImpl("attr2", "v1_val2")
                ))
                .reason("try1");

        DbDeployerAppContext context1 = getAppContext.valueOf(1);
        context1
                .cleanEnvironment()
                .setupEnvInfra()
                .deploy(args1);

        String schema = "oats";
        PhysicalSchema physicalSchema = context1.getEnvironment().getPhysicalSchema(schema);
        String schemaPrefix = context1.getEnvironment().getPlatform().getSchemaPrefix(physicalSchema);

        this.validateStep1(ds, new JdbcHelper(), schemaPrefix);
        DeployExecution execution1 = context1.getDeployExecutionDao().getLatestDeployExecution(schema);
        verifyExecution1(execution1);

        MainDeployerArgs args2 = new MainDeployerArgs()
                .deployExecutionAttributes(Sets.immutable.<DeployExecutionAttribute>with(
                        new DeployExecutionAttributeImpl("attr1", "v2_val1"),
                        new DeployExecutionAttributeImpl("attr2", "v2_val2"),
                        new DeployExecutionAttributeImpl("attr3", "v2_val3")
                ))
                .reason("try2");
        DbDeployerAppContext context2 = getAppContext.valueOf(2);
        context2.setupEnvInfra().deploy(args2);
        this.validateStep2(ds, new JdbcHelper(), schemaPrefix);
        DeployExecution execution2 = context2.getDeployExecutionDao().getLatestDeployExecution(schema);
        verifyExecution2(execution2);

        MutableList<DeployExecution> executions = context2.getDeployExecutionDao().getDeployExecutions(schema).toSortedListBy(DeployExecution.TO_ID);
        verifyExecution1(executions.get(0));
        verifyExecution2(executions.get(1));
    }

    private void verifyExecution1(DeployExecution execution1) {
        assertEquals("try1", execution1.getReason());
        assertEquals(
                Sets.immutable.<DeployExecutionAttribute>with(new DeployExecutionAttributeImpl("attr1", "v1_val1"), new DeployExecutionAttributeImpl("attr2", "v1_val2")),
                execution1.getAttributes());
    }

    private void verifyExecution2(DeployExecution execution2) {
        assertEquals("try2", execution2.getReason());
        assertEquals(
                Sets.immutable.<DeployExecutionAttribute>with(new DeployExecutionAttributeImpl("attr1", "v2_val1"), new DeployExecutionAttributeImpl("attr2", "v2_val2"), new DeployExecutionAttributeImpl("attr3", "v2_val3")),
                execution2.getAttributes());
    }

    public static void validateStep1(DataSource ds, JdbcHelper jdbc, String schemaPrefix) throws Exception {
        List<Map<String, Object>> results;
        Connection conn = ds.getConnection();
        try {
            results = jdbc.queryForList(conn, "select * from " + schemaPrefix + "TestTable order by idField");
        } finally {
            DbUtils.closeQuietly(conn);
        }

        assertEquals(4, results.size());
        validateResultRow(results.get(0), 1, "str1", 0);
        validateResultRow(results.get(1), 2, "str2", 0);
        validateResultRow(results.get(2), 3, "str3", 0);
        validateResultRow(results.get(3), 4, "str4", 0);
    }

    public static void validateStep2(DataSource ds, JdbcHelper jdbc, String schemaPrefix) throws Exception {
        List<Map<String, Object>> results;
        Connection conn = ds.getConnection();
        try {
            results = jdbc.queryForList(conn, "select * from " + schemaPrefix + "TestTable order by idField");
        } finally {
            DbUtils.closeQuietly(conn);
        }

        assertEquals(5, results.size());
        validateResultRow(results.get(0), 1, "str1", 0);
        validateResultRow(results.get(1), 3, "str3Changed", 0);
        validateResultRow(results.get(2), 4, "str4", 0);
        validateResultRow(results.get(3), 5, "str5", 0);
        validateResultRow(results.get(4), 6, "str6", 0);
    }

    private static void validateResultRow(Map<String, Object> map, Integer idField,
            String stringField, Integer booleanCol) {
        assertEquals(idField, map.get("idField"));
        assertEquals(stringField, map.get("stringField"));
        assertEquals(booleanCol, map.get("myBooleanCol"));
    }
}
