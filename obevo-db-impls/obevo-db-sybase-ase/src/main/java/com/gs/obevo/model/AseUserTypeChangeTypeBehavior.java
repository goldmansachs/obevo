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
package com.gs.obevo.model;

import java.sql.Connection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.changetypes.DbSimpleArtifactDeployer;
import com.gs.obevo.db.impl.core.changetypes.GrantChangeParser;
import com.gs.obevo.db.impl.core.changetypes.RerunnableDbChangeTypeBehavior;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.graph.GraphEnricher;

/**
 * User types / domains in Sybase ASE.
 *
 * Could be a subject to refactoring at some point as user types are also there in other
 * DBMSs - however, they are implemented differently (i.e. without a sp_droptype stored procedure).
 */
public class AseUserTypeChangeTypeBehavior extends RerunnableDbChangeTypeBehavior {
    public AseUserTypeChangeTypeBehavior(DbEnvironment env, DbChangeType dbChangeType, SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, GrantChangeParser grantChangeParser, GraphEnricher graphEnricher, DbPlatform dbPlatform, DbMetadataManager dbMetadataManager) {
        super(env, dbChangeType, sqlExecutor, baseArtifactDeployer, grantChangeParser, graphEnricher, dbPlatform, dbMetadataManager);
    }

    @Override
    protected String generateDropChangeRaw(Connection conn, Change change) {
        return String.format("sp_droptype N'%s'", change.getObjectName());
    }
}
