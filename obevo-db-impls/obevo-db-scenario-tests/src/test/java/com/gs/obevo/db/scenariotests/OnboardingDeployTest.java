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

import java.io.File;

import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class OnboardingDeployTest {
    private static final Logger LOG = LoggerFactory.getLogger(OnboardingDeployTest.class);

    @Test
    public void onboardingTest() throws Exception {
        final File srcDir = new File("./src/test/resources/scenariotests/onboardingDeploy");
        final File destDir = new File("./target/scenariotests/onboardingDeploy");
        FileUtils.deleteQuietly(destDir);

        FileUtils.copyDirectory(srcDir, destDir);

        LOG.info("Part 1 - do the deployment w/ onboarding mode");
        DbDeployerAppContext deployContext = DbEnvironmentFactory.getInstance().readOneFromSourcePath(destDir.getAbsolutePath(), "test")
                .buildAppContext();

        try {
            deployContext.setupEnvInfra().cleanEnvironment()
                    .deploy(new MainDeployerArgs().onboardingMode(true));
        } catch (RuntimeException exc) {
            exc.printStackTrace();
            // ignoring
        }

        // Verify that the exception files have been created properly
        assertTrue(new File(destDir, "SCHEMA1/staticdata/exceptions/TABLE_A.csv").exists());
        assertTrue(new File(destDir, "SCHEMA1/staticdata/exceptions/TABLE_B.csv").exists());
        assertTrue(new File(destDir, "SCHEMA1/table/exceptions/TABLE_C.ddl").exists());
        assertTrue(new File(destDir, "SCHEMA1/table/exceptions/TABLE_WITH_ERROR.ddl").exists());
        assertTrue(new File(destDir, "SCHEMA1/view/exceptions/VIEW_WITH_ERROR.sql").exists());
        assertTrue(new File(destDir, "SCHEMA1/view/dependentOnExceptions/VIEW_DEPENDING_ON_BAD_VIEW.sql").exists());

        LOG.info("Part 2 - rerun the deploy and ensure the data remains as is");
        deployContext = DbEnvironmentFactory.getInstance().readOneFromSourcePath(destDir.getAbsolutePath(), "test")
                .buildAppContext();

        try {
            deployContext.deploy(new MainDeployerArgs().onboardingMode(true));
        } catch (RuntimeException exc) {
            exc.printStackTrace();
            // ignoring
        }

        // Same assertions as before should hold
        assertTrue(new File(destDir, "SCHEMA1/staticdata/exceptions/TABLE_A.csv").exists());
        assertTrue(new File(destDir, "SCHEMA1/staticdata/exceptions/TABLE_B.csv").exists());
        assertTrue(new File(destDir, "SCHEMA1/table/exceptions/TABLE_C.ddl").exists());
        assertTrue(new File(destDir, "SCHEMA1/table/exceptions/TABLE_WITH_ERROR.ddl").exists());
        assertTrue(new File(destDir, "SCHEMA1/view/exceptions/VIEW_WITH_ERROR.sql").exists());
        assertTrue(new File(destDir, "SCHEMA1/view/dependentOnExceptions/VIEW_DEPENDING_ON_BAD_VIEW.sql").exists());

        LOG.info("Part 3 - fix the view and verify that it is moved back to the regular folder");
        FileUtils.copyFile(new File(srcDir, "VIEW_WITH_ERROR.corrected.sql"), new File(destDir, "SCHEMA1/view/exceptions/VIEW_WITH_ERROR.sql"));

        deployContext = DbEnvironmentFactory.getInstance().readOneFromSourcePath(destDir.getAbsolutePath(), "test")
                .buildAppContext();

        try {
            deployContext.deploy(new MainDeployerArgs().onboardingMode(true));
        } catch (RuntimeException exc) {
            exc.printStackTrace();
            // ignoring
        }

        assertTrue(new File(destDir, "SCHEMA1/staticdata/exceptions/TABLE_A.csv").exists());
        assertTrue(new File(destDir, "SCHEMA1/staticdata/exceptions/TABLE_B.csv").exists());
        assertTrue(new File(destDir, "SCHEMA1/table/exceptions/TABLE_C.ddl").exists());
        assertTrue(new File(destDir, "SCHEMA1/table/exceptions/TABLE_WITH_ERROR.ddl").exists());
        // These two have changed from above
        assertTrue(new File(destDir, "SCHEMA1/view/VIEW_WITH_ERROR.sql").exists());
        assertTrue(new File(destDir, "SCHEMA1/view/VIEW_DEPENDING_ON_BAD_VIEW.sql").exists());
    }
}
