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
package com.gs.obevo.db.impl.platforms.hsql;

import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.DbDeployerAppContextImpl;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory;
import org.eclipse.collections.api.block.function.Function0;

public class HsqlAppContext extends DbDeployerAppContextImpl {

    public SqlExecutor getSqlExecutor() {
        return this.singleton("getSqlExecutor", new Function0<SqlExecutor>() {
            @Override
            public SqlExecutor value() {
                return new HsqlSqlExecutor(getManagedDataSource());
            }
        });
    }

    @Override
    public EnvironmentInfraSetup getEnvironmentInfraSetup() {
        return new HsqlEnvironmentSetupInfra(this.getEnvironment(), this.getManagedDataSource(), this.getDbMetadataManager());
    }

    @Override
    protected DataSourceFactory getDataSourceFactory() {
        return new HsqlJdbcDataSourceFactory();
    }
}
