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
package com.gs.obevo.db.impl.core.changetypes;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaView;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.changetypes.RerunnableChangeTypeCommandCalculator;
import com.gs.obevo.impl.graph.GraphEnricher;
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * Represent the class of change types that are rerunnable (i.e. that we can easily drop/add or replace the
 * object w/out considering state).
 */
public class RerunnableDbChangeTypeBehavior extends AbstractDbChangeTypeBehavior {
    private final GraphEnricher graphEnricher;
    private final DbPlatform dbPlatform;
    private final DbMetadataManager dbMetadataManager;

    public RerunnableDbChangeTypeBehavior(DbEnvironment env, DbChangeType dbChangeType, SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, GrantChangeParser grantChangeParser, GraphEnricher graphEnricher, DbPlatform dbPlatform, DbMetadataManager dbMetadataManager) {
        super(env, dbChangeType, sqlExecutor, baseArtifactDeployer, grantChangeParser);
        this.graphEnricher = graphEnricher;
        this.dbPlatform = dbPlatform;
        this.dbMetadataManager = dbMetadataManager;
    }

    protected DbPlatform getDbPlatform() {
        return dbPlatform;
    }

    protected DbMetadataManager getDbMetadataManager() {
        return dbMetadataManager;
    }

    @Override
    public ChangeTypeCommandCalculator getChangeTypeCalculator() {
        return new RerunnableChangeTypeCommandCalculator(graphEnricher);
    }

    @Override
    public void deploy(Change change) {
        if (!change.isCreateOrReplace()) {
            dropObject(change, true);
        }
        super.deploy(change);
    }

    @Override
    protected boolean shouldApplyGrants(Change artifact) {
        return artifact.getApplyGrants() != null ? artifact.getApplyGrants().booleanValue() : true;
    }

    @Override
    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        changeAuditDao.updateOrInsertChange(change, deployExecution);
    }

    @Override
    public void undeploy(Change change) {
        dropObject(change, false);
    }

    @Override
    public String getDefinitionFromEnvironment(Change drop) {
        if (drop.getChangeType().getName().equals(ChangeType.VIEW_STR)) {
            DaTable table = this.dbMetadataManager.getTableInfo(drop.getPhysicalSchema(),
                    drop.getObjectName(), new DaSchemaInfoLevel().setRetrieveViewDetails(true));
            DaView view;
            if (table instanceof DaView) {
                view = (DaView) table;
            } else {
                throw new IllegalStateException("Invalid code here - should not have gotten to this point; what " +
                        "kind of object? " + table);
            }

            return view.getDefinition();
        } else if (drop.getChangeType().getName().equals(ChangeType.FUNCTION_STR)) {
            ImmutableCollection<DaRoutine> procedures = this.dbMetadataManager.getRoutineInfo(
                    drop.getPhysicalSchema(),
                    drop.getObjectName(),
                    new DaSchemaInfoLevel().setRetrieveRoutineDetails(true)
            );
            StringBuilder sb = new StringBuilder();
            for (DaRoutine routine : procedures) {
                sb.append(routine.getDefinition()).append("\nGO");
            }

            return sb.toString();
        } else if (drop.getChangeType().getName().equals(ChangeType.SEQUENCE_STR)) {
            return ""; // no dependencies for SEQUENCE
        }

        throw new UnsupportedOperationException("No other dbObjectType implemented here: " + drop.getChangeType()
                + "; " + drop);
    }
}
