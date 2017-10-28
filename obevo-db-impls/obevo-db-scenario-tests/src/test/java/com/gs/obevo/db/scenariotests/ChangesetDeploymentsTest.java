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

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangesetDeploymentsTest {
    @Test
    public void testChangesetsAppliedInPhases() {
        DbDeployerAppContext dbDeployerAppContext = DbEnvironmentFactory.getInstance()
                .readOneFromSourcePath("scenariotests/changesetDeploy/step1")
                .buildAppContext()
                .setupEnvInfra().cleanEnvironment();

        dbDeployerAppContext.deploy();

        // in the default phase, all the
        checkForIndex(dbDeployerAppContext, "TABLE_A", "TABLE_A_IND1", false);
        checkForIndex(dbDeployerAppContext, "TABLE_B", "TABLE_B_IND1", false);
        checkForIndex(dbDeployerAppContext, "TABLE_C", "TABLE_C_IND1", false);

        dbDeployerAppContext.deploy(new MainDeployerArgs().changesetNames(Sets.immutable.with("phaseA")));

        checkForIndex(dbDeployerAppContext, "TABLE_A", "TABLE_A_IND1", true);
        checkForIndex(dbDeployerAppContext, "TABLE_B", "TABLE_B_IND1", true);
        checkForIndex(dbDeployerAppContext, "TABLE_C", "TABLE_C_IND1", false);

        dbDeployerAppContext.deploy(new MainDeployerArgs().changesetNames(Sets.immutable.with("phaseB")));

        checkForIndex(dbDeployerAppContext, "TABLE_A", "TABLE_A_IND1", true);
        checkForIndex(dbDeployerAppContext, "TABLE_B", "TABLE_B_IND1", true);
        checkForIndex(dbDeployerAppContext, "TABLE_C", "TABLE_C_IND1", true);
    }

    @Test
    public void testChangesetAppliedAtOnce() {
        DbDeployerAppContext dbDeployerAppContext = DbEnvironmentFactory.getInstance()
                .readOneFromSourcePath("scenariotests/changesetDeploy/step1")
                .buildAppContext()
                .setupEnvInfra().cleanEnvironment();

        dbDeployerAppContext.deploy(new MainDeployerArgs().allChangesets(true));

        checkForIndex(dbDeployerAppContext, "TABLE_A", "TABLE_A_IND1", true);
        checkForIndex(dbDeployerAppContext, "TABLE_B", "TABLE_B_IND1", true);
        checkForIndex(dbDeployerAppContext, "TABLE_C", "TABLE_C_IND1", true);
    }

    private void checkForIndex(DbDeployerAppContext dbDeployerAppContext, final String tableName, String indexName, boolean shouldExist) {
        final DbMetadataManager dbMetadataManager = dbDeployerAppContext.getDbMetadataManager();
        DaTable tableInfo = dbMetadataManager.getTableInfo(new PhysicalSchema("SCHEMA1"), tableName, new DaSchemaInfoLevel().setRetrieveTableIndexes(true));

        DaIndex index = tableInfo.getIndices().detect(Predicates.attributeEqual(DaIndex.TO_NAME, indexName));
        assertEquals("Index " + indexName + " should exist==" + shouldExist, shouldExist, index != null);
    }
}
