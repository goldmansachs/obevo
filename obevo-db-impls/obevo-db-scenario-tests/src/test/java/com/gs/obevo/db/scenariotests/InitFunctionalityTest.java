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
import java.sql.SQLException;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.DbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import static org.junit.Assert.fail;

public class InitFunctionalityTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule();

    private JdbcHelper jdbc;
    private Connection conn;

    @Before
    public void setup() throws SQLException {
        DbDeployerAppContext dbDeployerAppContext = DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step1")
                .buildAppContext("abc", "abc");

        this.jdbc = new JdbcHelper();

        this.conn = dbDeployerAppContext.getDataSource().getConnection();

        // clean up from previous tests if not shut down cleanly
        jdbc.update(conn, "DROP ALL objects DELETE files");
    }

    @After
    public void teardown() {
        DbUtils.closeQuietly(conn);
    }

    @Test
    public void testDeploy() {
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step1")
                .buildAppContext("abc", "abc")
                .setupEnvInfra().cleanEnvironment()
                .deploy();

        // TODO the SystemOutRule doesn't work w/ Maven, as no system output is captured
        // Leave this commented-code in; I REALLY would like to get this to work!

//        systemOutRule.enableLog();
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step2")
                .buildAppContext("abc", "abc")
                .deploy(new MainDeployerArgs().performInitOnly(true));  // init

        // validate that the warning message is getting printed out for doing an INIT when tables already exist
//        assertThat(systemOutRule.getLog(), containsString(DbDeployer.INIT_WARNING_MESSAGE));

        DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step2")
                .buildAppContext("abc", "abc")
                .deploy();
    }

    @Test
    public void testFailureDeploy() {
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step1")
                .buildAppContext("abc", "abc")
                .setupEnvInfra().cleanEnvironment()
                .deploy();

        try {
            DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step2")
                    .buildAppContext("abc", "abc")
                    .deploy();  // don't do init this time
            fail("Expecting a failure here as we have changed the files w/out doing the -performInitOnly step");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test(expected = DeployerRuntimeException.class)
    public void testNonAutoInitExpectFailure() {
        setupPreExistingEnvironmentForAutoInitTests();

        // Now try rerunning the deploy when autoInitEnabled == false. This should fail as it tries to recreate an existing table
        DbEnvironment dbEnvironment = DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step1");
        dbEnvironment.buildAppContext("abc", "abc").deploy();  // should fail here
    }

    /**
     * builds a base environment w/ tables but without the audit table so that we can simulate the auto-init behavior.
     */
    private void setupPreExistingEnvironmentForAutoInitTests() {
        DbDeployerAppContext dbDeployerAppContext = DbEnvironmentFactory.getInstance().readOneFromSourcePath("./src/test/resources/scenariotests/initusecase/step1")
                .buildAppContext("abc", "abc");
        dbDeployerAppContext.setupEnvInfra().cleanEnvironment()
                .deploy();

        jdbc.update(conn, "DROP TABLE SCHEMA1.ARTIFACTDEPLOYMENT");
    }
}
