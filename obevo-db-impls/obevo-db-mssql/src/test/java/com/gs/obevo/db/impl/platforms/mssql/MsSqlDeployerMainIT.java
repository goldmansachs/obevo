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
package com.gs.obevo.db.impl.platforms.mssql;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MsSqlDeployerMainIT {
    @Parameterized.Parameters
    public static Collection<Object[]> params() {
        return MsSqlParamReader.getParamReader().getAppContextAndJdbcDsParams();
    }

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;
    private final DataSource ds;

    public MsSqlDeployerMainIT(IntToObjectFunction<DbDeployerAppContext> getAppContext, DataSource ds) {
        this.getAppContext = getAppContext;
        this.ds = ds;
    }

    @Test
    public void testAseDeploy() throws Exception {
        DbDeployerAppContext step1Context = getAppContext.valueOf(1);
        step1Context
                .cleanEnvironment()
                .setupEnvInfra()
                .deploy();

        String physicalSchemaStr = step1Context.getEnvironment().getPlatform().getSchemaPrefix(step1Context.getEnvironment().getPhysicalSchema("oats"));
        this.validateStep1(step1Context.getDataSource(), physicalSchemaStr, new JdbcHelper());

        DbDeployerAppContext step2Context = getAppContext.valueOf(2);
        step2Context
                .setupEnvInfra()
                .deploy();
        this.validateStep2(step2Context.getDataSource(), physicalSchemaStr, new JdbcHelper());
    }

    private void validateStep1(DataSource ds, String physicalSchemaStr, JdbcHelper jdbc) throws Exception {
        List<Map<String, Object>> results;
        Connection conn = ds.getConnection();
        try {
            results = jdbc.queryForList(conn, "select * from " + physicalSchemaStr + "TestTable order by idField");
        } finally {
            DbUtils.closeQuietly(conn);
        }

        assertEquals(4, results.size());
        this.validateResultRow(results.get(0), 1, "str1", 0);
        this.validateResultRow(results.get(1), 2, "str2", 0);
        this.validateResultRow(results.get(2), 3, "str3", 0);
        this.validateResultRow(results.get(3), 4, "str4", 0);
    }

    private void validateStep2(DataSource ds, String physicalSchemaStr, JdbcHelper jdbc) throws Exception {
        List<Map<String, Object>> results;
        Connection conn = ds.getConnection();
        try {
            results = jdbc.queryForList(conn, "select * from " + physicalSchemaStr + "TestTable order by idField");
        } finally {
            DbUtils.closeQuietly(conn);
        }

        assertEquals(5, results.size());
        this.validateResultRow(results.get(0), 1, "str1", 0);
        this.validateResultRow(results.get(1), 3, "str3Changed", 0);
        this.validateResultRow(results.get(2), 4, "str4", 0);
        this.validateResultRow(results.get(3), 5, "str5", 0);
        this.validateResultRow(results.get(4), 6, "str6", 0);
    }

    private void validateResultRow(Map<String, Object> map, Integer idField,
            String stringField, Integer booleanCol) {
        assertEquals(idField, map.get("idField"));
        assertEquals(stringField, map.get("stringField"));
        assertEquals(booleanCol, map.get("myBooleanCol"));
    }
}
