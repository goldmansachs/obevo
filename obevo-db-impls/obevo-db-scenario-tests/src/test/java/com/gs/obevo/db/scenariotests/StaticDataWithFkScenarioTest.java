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

public class StaticDataWithFkScenarioTest {
    /**
     * Two use cases here:
     * 1) The regular use case (represented via the TABLE_* tables) where we maintain the static data in CSV and see
     * that we can do delete/insert/update of data)
     * 2) The backwards-compatibility case (represented via INSERTFK_* tables) where we have tables w/ FKs but
     * with static data as insert sqls. In our current logic, as the InsertInto mode for static data
     * insertion does not automatically do a "delete", it won't work with FKs as we have no way to separate the
     * delete execution order from insert (as we do w/ CSVs). Either we add support for this on the InsertInto side,
     * or we just encourage folks to move over to CSV
     */
    @Test
    public void testBaselineScenario() {
        System.out.println("Step 1: Setting up the initial data");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/staticdata-with-fk/step1", "test")
                .buildAppContext()
                .setupEnvInfra().cleanEnvironment()
                .deploy();

        System.out
                .println("Step 2: now doing some changes in the static data to verify that the correct order is " +
                        "preserved. TABLE_A depends on TABLE_B (thus, B's inserts need to go before A's, " +
                        "but B's deletes need to go after A's), while TABLE_C is independent");
        DbEnvironmentFactory.getInstance().readOneFromSourcePath("scenariotests/staticdata-with-fk/step2", "test")
                .buildAppContext()
                .deploy();

        // TODO add some assertions
    }
}
