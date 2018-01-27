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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.DbDeployerAppContextImpl;
import com.gs.obevo.db.impl.core.changetypes.CsvStaticDataDeployer;
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory;
import com.gs.obevo.db.impl.platforms.sybaseiq.iqload.IqBulkLoadCsvStaticDataDeployer;
import com.gs.obevo.db.impl.platforms.sybaseiq.iqload.IqLoadMode;
import com.gs.obevo.impl.PostDeployAction;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.collections.api.block.function.Function0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SybaseIqAppContext extends DbDeployerAppContextImpl {
    private static final Logger LOG = LoggerFactory.getLogger(SybaseIqAppContext.class);

    public SqlExecutor getSqlExecutor() {
        return this.singleton("getSqlExecutor", new Function0<SqlExecutor>() {
            @Override
            public SqlExecutor value() {
                return new IqSqlExecutor(getIqDataSource());
            }
        });
    }

    private IqDataSource getIqDataSource() {
        return (IqDataSource) getManagedDataSource();
    }

    @Override
    public CsvStaticDataDeployer getCsvStaticDataLoader() {
        if (getIqDataSource().isIqClientLoadSupported()) {
            LOG.info("Using IQ Client load mechanism for IQ CSV Loads");
            IqLoadMode iqLoadMode = SystemUtils.IS_OS_WINDOWS ? IqLoadMode.IQ_CLIENT_WINDOWS : IqLoadMode.IQ_CLIENT;
            return new IqBulkLoadCsvStaticDataDeployer(this.env, this.getSqlExecutor(), this.getIqDataSource(),
                    this.getDbMetadataManager(), this.env.getPlatform(), iqLoadMode, this.getWorkDir());
        } else {
            LOG.info("Using the default SQL insert/update/delete statements for IQ CSV Loads");
            return super.getCsvStaticDataLoader();
        }
    }

    @Override
    public PostDeployAction getPostDeployAction() {
        return new IqPostDeployAction(this.getSqlExecutor());
    }

    @Override
    protected DataSourceFactory getDataSourceFactory() {
        return new IqJdbcDataSourceFactory();
    }
}
