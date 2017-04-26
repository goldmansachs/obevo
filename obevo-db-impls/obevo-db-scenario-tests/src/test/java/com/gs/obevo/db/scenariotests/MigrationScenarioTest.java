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
package com.gs.obevo.db.scenariotests;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.h2.H2JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.dbutils.DbUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MigrationScenarioTest {
    @Test
    public void testMigration() throws Exception {
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/migration-example/step1")
                .buildAppContext("sa", "")
                .setupEnvInfra()
                .cleanEnvironment()
                .deploy();

        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/migration-example/step2")
                .buildAppContext("sa", "")
                .deploy();

        // Run it twice to ensure that we don't duplicate data
        DbDeployerAppContext context = DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/migration-example/step2")
                .buildAppContext("sa", "")
                .deploy();

        this.validateInstance("MigrationExample", "SCHEMA1");
    }

    private void validateInstance(String instanceName, String schemaName) throws Exception {
        DataSource ds = JdbcDataSourceFactory.createFromJdbcUrl(org.h2.Driver.class, H2JdbcDataSourceFactory.getUrl(instanceName, false), new Credential("sa", ""));
        JdbcHelper jdbc = new JdbcHelper();

        Connection conn = ds.getConnection();
        try {
            int count = jdbc.queryForInt(conn, "select count(*) from " + schemaName + ".TABLE_A");
            assertEquals(6, count);  // TODO verify the timestamp and ordering of rows in the table?
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    /**
     * TODO test - verify that rollback works here too.
     */
}
