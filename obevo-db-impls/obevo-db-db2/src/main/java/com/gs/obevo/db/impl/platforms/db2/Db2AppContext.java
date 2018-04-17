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
package com.gs.obevo.db.impl.platforms.db2;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.DbDeployerAppContextImpl;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory;
import com.gs.obevo.db.impl.platforms.db2.changetypes.Db2RoutineChangeTypeBehavior;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry.ChangeTypeBehaviorRegistryBuilder;
import com.gs.obevo.impl.PostDeployAction;
import org.eclipse.collections.api.block.function.Function0;

public class Db2AppContext extends DbDeployerAppContextImpl {
    public SqlExecutor getSqlExecutor() {
        return this.singleton("getSqlExecutor", new Function0<SqlExecutor>() {
            @Override
            public SqlExecutor value() {
                return new Db2SqlExecutor(getManagedDataSource(), env);
            }
        });
    }

    @Override
    public PostDeployAction getPostDeployAction() {
        return this.singleton("getPostDeployAction", new Function0<PostDeployAction>() {
            @Override
            public PostDeployAction value() {
                // Doing this cast here as Srping Java Config works better w/ interfaces directly.
                // I'd like to avoid having this cast though
                return new Db2PostDeployAction((Db2SqlExecutor) Db2AppContext.this.getSqlExecutor(), deployStatsTracker());
            }
        });
    }

    @Override
    protected DataSourceFactory getDataSourceFactory() {
        return new Db2JdbcDataSourceFactory();
    }

    @Override
    public EnvironmentInfraSetup getEnvironmentInfraSetup() {
        return new Db2EnvironmentInfraSetup(this.getEnvironment(), this.getManagedDataSource(), this.deployStatsTracker(), this.getDbMetadataManager(), this.getChangeTypeBehaviorRegistry());
    }

    @Override
    protected ChangeTypeBehaviorRegistryBuilder getChangeTypeBehaviors() {
        ChangeTypeBehaviorRegistryBuilder changeTypeBehaviors = super.getChangeTypeBehaviors();
        changeTypeBehaviors.putBehavior(ChangeType.SP_STR, updateRoutineType(ChangeType.SP_STR));
        changeTypeBehaviors.putBehavior(ChangeType.FUNCTION_STR, updateRoutineType(ChangeType.FUNCTION_STR));
        return changeTypeBehaviors;
    }

    private ChangeTypeBehavior updateRoutineType(String routineTypeName) {
        ChangeType routineChangeType = platform().getChangeTypes().detect(_this -> _this.getName().equals(routineTypeName));
        Db2RoutineChangeTypeBehavior behavior = new Db2RoutineChangeTypeBehavior(env, (DbChangeType) routineChangeType, getSqlExecutor(), simpleArtifactDeployer(), grantChangeParser(), graphEnricher(), platform(), getDbMetadataManager());
        return behavior;
    }
}
