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
package com.gs.obevo.db.scenariotests;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.h2.H2JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiEnvDeployScenarioTest {
    @Test
    public void testMultiEnvDeploy() throws Exception {
        DbEnvironmentFactory.getInstance().readFromSourcePath("scenariotests/multiEnvDeploy", "test*").forEach(new Procedure<DbEnvironment>() {
            @Override
            public void value(DbEnvironment dbEnvironment) {
                dbEnvironment.buildAppContext()
                        .setupEnvInfra().cleanEnvironment()
                        .deploy();
            }
        });

        this.validateInstance("MultiEnvTest1", "SCH1");
        this.validateInstance("MultiEnvTest2", "SCH2");
    }

    private void validateInstance(String instanceName, String schemaName) throws Exception {
        DataSource ds = JdbcDataSourceFactory.createFromJdbcUrl(org.h2.Driver.class, H2JdbcDataSourceFactory.getUrl(instanceName, false), new Credential("sa", ""));
        JdbcHelper jdbc = new JdbcHelper();

        Connection conn = ds.getConnection();
        try {
            int count = jdbc.queryForInt(conn, "select count(*) from " + schemaName + ".TABLE_A where 1=0");
            assertEquals("Expecting a successful return for " + instanceName + " and schemaName as the table should exist",
                    0, count);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }
}
