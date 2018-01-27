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

import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.h2.H2JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.dbutils.DbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MetadataGroupTest {

    private Connection conn;

    @Before
    public void setup() throws Exception {
        DataSource ds = JdbcDataSourceFactory.createFromJdbcUrl(org.h2.Driver.class, H2JdbcDataSourceFactory.getUrl("MetadataGroup", false), new Credential("sa", ""));
        JdbcHelper jdbc = new JdbcHelper();

        this.conn = ds.getConnection();
        // clean up from previous tests if not shut down cleanly
        jdbc.update(conn, "DROP ALL objects DELETE files");
    }

    @After
    public void teardown() {
        DbUtils.closeQuietly(conn);
    }

    @Test
    public void testDeploy() {
        // step 1 - deploy the tables with FKs
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/metadata-group/step1")
                .buildAppContext()
                .setupEnvInfra().cleanEnvironment()
                .deploy();

        // step 2 - now deploy the metadata, ensure that the logic is fine
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/metadata-group/step2")
                .buildAppContext()
                .deploy();
    }
}
