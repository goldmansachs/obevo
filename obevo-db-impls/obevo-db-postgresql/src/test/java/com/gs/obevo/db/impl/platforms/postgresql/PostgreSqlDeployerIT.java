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
package com.gs.obevo.db.impl.platforms.postgresql;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PostgreSqlDeployerIT {
    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return PostgreSqlParamReader.getParamReader().getAppContextAndJdbcDsParams();
    }

    private final IntToObjectFunction<DbDeployerAppContext> getAppContext;
    private final DataSource ds;

    public PostgreSqlDeployerIT(IntToObjectFunction<DbDeployerAppContext> getAppContext, DataSource ds) {
        this.getAppContext = getAppContext;
        this.ds = ds;
    }

    @Test
    public void testDeploy() throws Exception {
        getAppContext.valueOf(1)
                .cleanEnvironment()
                .deploy();

        // ensuring that we can modify
        DbDeployerAppContext dbDeployerAppContext = getAppContext.valueOf(2);
        dbDeployerAppContext
                .cleanEnvironment()
                .setupEnvInfra()
                .deploy();

        JdbcHelper jdbc = new JdbcHelper();

        Connection conn = ds.getConnection();
        try {
            List<Map<String, Object>> results = jdbc.queryForList(conn, "select * from " + dbDeployerAppContext.getEnvironment().getPhysicalSchema("schema1") + ".TABLE_A order by a_id");
            assertEquals(3, results.size());
            this.validateResults(results.get(0), 2, 3, "fasdfasd", "2013-02-02 11:11:11.65432", 9);
            this.validateResults(results.get(1), 3, 4, "ABC", null, 9);
            this.validateResults(results.get(2), 4, 2, "ABC", "2012-01-01 12:12:12", null);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private void validateResults(Map<String, Object> map, Integer aId,
            Integer bId, String stringField, String timestampField, Integer cId) {

        assertEquals(aId, map.get("A_ID"));
        assertEquals(bId, map.get("B_ID"));
        assertEquals(stringField, map.get("STRING_FIELD"));
        Timestamp millis = timestampField == null ? null : Timestamp.valueOf(timestampField);
        assertEquals(millis, map.get("TIMESTAMP_FIELD"));
        assertEquals(cId, map.get("C_ID"));
    }
}
