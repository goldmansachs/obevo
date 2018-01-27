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
package com.gs.obevo.db.impl.platforms.db2.changetypes;

import java.sql.Connection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.changetypes.DbSimpleArtifactDeployer;
import com.gs.obevo.db.impl.core.changetypes.GrantChangeParser;
import com.gs.obevo.db.impl.core.changetypes.RerunnableDbChangeTypeBehavior;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.graph.GraphEnricher;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChangeType behavior for DB2 routines (SPs, Functions), notably to handle DB2's support for routine
 * overloads and specific names.
 */
public class Db2RoutineChangeTypeBehavior extends RerunnableDbChangeTypeBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(Db2RoutineChangeTypeBehavior.class);

    public Db2RoutineChangeTypeBehavior(DbEnvironment env, DbChangeType dbChangeType, SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, GrantChangeParser grantChangeParser, GraphEnricher graphEnricher, DbPlatform dbPlatform, DbMetadataManager dbMetadataManager) {
        super(env, dbChangeType, sqlExecutor, baseArtifactDeployer, grantChangeParser, graphEnricher, dbPlatform, dbMetadataManager);
    }

    @Override
    public Pair<Boolean, RichIterable<String>> getQualifiedObjectNames(Connection conn, PhysicalSchema physicalSchema, final String objectName) {
        ImmutableCollection<String> specificNames = getDbMetadataManager().getRoutineInfo(physicalSchema, objectName)
                .collect(DaRoutine.TO_SPECIFIC_NAME);

        return Tuples.<Boolean, RichIterable<String>>pair(true, specificNames);
    }

    @Override
    protected String generateDropChangeRaw(Connection conn, Change change) {
        StringBuilder sb = new StringBuilder();

        final ImmutableCollection<DaRoutine> routines = getDbMetadataManager().getRoutineInfo(change.getPhysicalSchema(env), change.getObjectName());
        LOG.info("Found {} routines with name {} to drop", routines.size(), change.getObjectName());
        for (DaRoutine routine : routines) {
            sb.append("DROP SPECIFIC ").append(getDbChangeType().getDefaultObjectKeyword()).append(" ").append(routine.getSpecificName()).append("\nGO\n");
        }

        return sb.toString();
    }
}
