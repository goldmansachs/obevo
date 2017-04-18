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
package com.gs.obevo.db.impl.platforms.hsql;

import java.sql.Connection;
import java.sql.SQLException;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.unittest.UnitTestDbBuilder;
import org.apache.commons.dbutils.DbUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HsqlDeployerTest {

    private JdbcHelper jdbc;

    private void setup(DbDeployerAppContext context) {
        this.jdbc = new JdbcHelper();
    }

    @Test
    public void testDeploy() throws SQLException {
        DbEnvironment env = DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/platforms/hsql");
        DbDeployerAppContext context = env.buildAppContext("sa", "");

        context.setupEnvInfra();
        context.cleanEnvironment();
        context.deploy();
        // do a clean and deploy again to ensure that the clean functionality works
        context.cleanEnvironment();
        context.deploy();

        this.setup(context);
        // simple test to assert that the table has been created
        int result;
        Connection conn = context.getDataSource().getConnection();
        try {
            result = this.jdbc.queryForInt(conn, "select count(*) from SCHEMA1.TABLE_A");
            assertEquals(3, result);
            result = this.jdbc.queryForInt(conn, "select count(*) from SCHEMA1.VIEW1");
            assertEquals(3, result);
            // String columnListSql =
            // "select name from syscolumns where id in (select id from sysobjects where name = 'TEST_TABLE')";
            // List<String> columnsInTestTable = db2JdbcTemplate.query(columnListSql, new SingleColumnRowMapper<String>());
            // Assert.assertEquals(Lists.mutable.with("ID", "STRING", "MYNEWCOL"), FastList.newList(columnsInTestTable));
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    @Test
    public void testUnitTestDeploy() throws SQLException {
        DbDeployerAppContext context = UnitTestDbBuilder.newBuilder()
                .setEnvName("test2")
                .setDbPlatform(new HsqlDbPlatform())
                .setSourcePath("platforms/hsql")
                .setDbServer("HsqlCustomName")
                .buildContext();
        context.setupEnvInfra();

        // run it twice to ensure that we can drop the schema
        context.cleanAndDeploy();
        context.cleanAndDeploy();

        DbEnvironment env = context.getEnvironment();
        System.out.println("Created env at " + env.getJdbcUrl());
        int result;

        this.setup(context);
        Connection conn = context.getDataSource().getConnection();
        try {
            result = this.jdbc.queryForInt(conn, "select count(*) from SCHEMA1.TABLE_A");
            assertEquals(3, result);
            result = this.jdbc.queryForInt(conn, "select count(*) from SCHEMA1.VIEW1");
            assertEquals(3, result);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }
}
