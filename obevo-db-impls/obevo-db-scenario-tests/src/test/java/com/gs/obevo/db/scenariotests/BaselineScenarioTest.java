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

import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaselineScenarioTest {
    private static final Logger LOG = LoggerFactory.getLogger(BaselineScenarioTest.class);

    @Test
    public void testBaselineScenario() {
        LOG.info("Step 1: Setting up the initial deployment version");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/baseline-scenario/step1", "test")
                .buildAppContext()
                .setupEnvInfra().cleanEnvironment()
                .deploy();

        LOG.info("Step 2: Performing the baseline (chng1 chng2 chng3 go away from the baseline) and chng4 is added");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/baseline-scenario/step2", "test")
                .buildAppContext()
                .deploy();

        LOG.info("Step 3: Doing 1 more change on TABLE_A after the baseline, w/ the baseline entries removed (no longer needed)");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/baseline-scenario/step3", "test")
                .buildAppContext()
                .deploy();

        // TODO add some assertions
    }
}
