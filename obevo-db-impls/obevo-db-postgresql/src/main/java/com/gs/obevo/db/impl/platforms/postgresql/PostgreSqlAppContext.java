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
package com.gs.obevo.db.impl.platforms.postgresql;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.DbDeployerAppContextImpl;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory;
import com.gs.obevo.db.impl.platforms.postgresql.changetypes.PostgreSqlFunctionChangeTypeBehavior;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry.ChangeTypeBehaviorRegistryBuilder;
import com.gs.obevo.impl.NoOpPostDeployAction;
import com.gs.obevo.impl.PostDeployAction;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;

public class PostgreSqlAppContext extends DbDeployerAppContextImpl {
    public SqlExecutor getSqlExecutor() {
        return this.singleton("getSqlExecutor", new Function0<PostgreSqlSqlExecutor>() {
            @Override
            public PostgreSqlSqlExecutor value() {
                return new PostgreSqlSqlExecutor(PostgreSqlAppContext.this.getManagedDataSource());
            }
        });
    }

    @Override
    public PostDeployAction getPostDeployAction() {
        return this.singleton("getPostDeployAction", new Function0<NoOpPostDeployAction>() {
            @Override
            public NoOpPostDeployAction value() {
                return new NoOpPostDeployAction();
            }
        });
    }

    @Override
    protected DataSourceFactory getDataSourceFactory() {
        return new PostgreSqlJdbcDataSourceFactory();
    }

    @Override
    public EnvironmentInfraSetup getEnvironmentInfraSetup() {
        return new PostgreSqlEnvironmentInfraSetup(this.getEnvironment(), this.getManagedDataSource(), this.deployStatsTracker(), getDbMetadataManager(), this.getChangeTypeBehaviorRegistry());
    }

    @Override
    protected ChangeTypeBehaviorRegistryBuilder getChangeTypeBehaviors() {
        ChangeType routineChangeType = platform().getChangeTypes().detect(new Predicate<ChangeType>() {
            @Override
            public boolean accept(ChangeType it) {
                return it.getName().equals(ChangeType.FUNCTION_STR);
            }
        });
        return super.getChangeTypeBehaviors()
                .putBehavior(ChangeType.FUNCTION_STR, new PostgreSqlFunctionChangeTypeBehavior(env, (DbChangeType) routineChangeType, getSqlExecutor(), simpleArtifactDeployer(), grantChangeParser(), graphEnricher(), platform(), getDbMetadataManager()));
    }
}
