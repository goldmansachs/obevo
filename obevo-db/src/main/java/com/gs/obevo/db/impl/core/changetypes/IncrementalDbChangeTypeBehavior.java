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

import java.sql.Connection;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.changetypes.IncrementalChangeTypeCommandCalculator;
import org.eclipse.collections.api.block.procedure.Procedure;

public class IncrementalDbChangeTypeBehavior extends AbstractDbChangeTypeBehavior {
    private static final Pattern CREATE_TABLE_MATCHER = Pattern.compile("(?i).*create\\s+table.*", Pattern.DOTALL);

    private final DeployMetricsCollector deployMetricsCollector;
    private final int numThreads;

    public IncrementalDbChangeTypeBehavior(DbEnvironment env, DbChangeType changeType, SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, GrantChangeParser grantChangeParser, DeployMetricsCollector deployMetricsCollector, int numThreads) {
        super(env, changeType, sqlExecutor, baseArtifactDeployer, grantChangeParser);
        this.deployMetricsCollector = deployMetricsCollector;
        this.numThreads = numThreads;
    }

    @Override
    public ChangeTypeCommandCalculator getChangeTypeCalculator() {
        return new IncrementalChangeTypeCommandCalculator(deployMetricsCollector, numThreads);
    }

    @Override
    protected boolean shouldApplyGrants(Change artifact) {
        return artifact.getApplyGrants() != null ? artifact.getApplyGrants().booleanValue() : CREATE_TABLE_MATCHER.matcher(artifact.getConvertedContent()).matches();
    }

    @Override
    public void undeploy(final Change change) {
        final ChangeIncremental incrementalDeployed = (ChangeIncremental) change;
        if (!incrementalDeployed.isRollbackActivated()) {
            throw new IllegalStateException("Change is intended for rollback, but was not marked as such already; indicates a code issue: " + change);
        }

        getSqlExecutor().executeWithinContext(change.getPhysicalSchema(), new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                getBaseArtifactDeployer().deployArtifact(conn, change);
            }
        });
    }

    @Override
    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        changeAuditDao.insertNewChange(change, deployExecution);
    }

    /**
     * If we decide to implement this in the future, rely on {@link DbMetadataManager} and see
     * {@link RerunnableDbChangeTypeBehavior} for reference.
     */
    @Override
    public String getDefinitionFromEnvironment(Change exampleChange) {
        throw new UnsupportedOperationException("Not yet implemented; could be implemented/needed at some point in the future");
    }
}
