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

import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollbackScenarioTest {
    private static final Logger LOG = LoggerFactory.getLogger(RollbackScenarioTest.class);

    @Test
    public void testRollbackScenario() {
        LOG.info("Step 1: Setting up the initial deployment version");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/rollback-scenario/step1", "test")
                .buildAppContext()
                .setupEnvInfra().cleanEnvironment()
                .deploy();

        LOG.info("Step 2: Doing the following changes:\n"
                +
                "TABLE_A stays the same\n"
                +
                "TABLE_B has a change with a rollback script prepared, and another change immediately rolled back (step2a has a bad rollback script, step2b corrects it)\n"
                +
                "TABLE_C has a change, but without a rollback script\n" +
                "VIEW1 stays the same\n" +
                "VIEW2 has a change\n" +
                "VIEW3 is dropped\n" +
                "VIEW4 is added\n");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/rollback-scenario/step2a", "test")
                .buildAppContext()
                .deploy();
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/rollback-scenario/step2b", "test")
                .buildAppContext()
                .deploy();

        LOG.info("Step 3: Now executing the rolback:\n" +
                "TABLE_A stays the same\n" +
                "TABLE_B has the rollback script executed, and the other rolled-back change added back\n" +
                "TABLE_C stays the same, but a warning message is given\n" +
                "VIEW1 stays the same\n" +
                "VIEW2 has a change rolled back\n" +
                "VIEW3 is re-added\n" +
                "VIEW4 is dropped\n");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/rollback-scenario/step1", "test")
                .buildAppContext()
                .deploy(new MainDeployerArgs().rollback(true));

        // TODO add some assertions
    }
}
