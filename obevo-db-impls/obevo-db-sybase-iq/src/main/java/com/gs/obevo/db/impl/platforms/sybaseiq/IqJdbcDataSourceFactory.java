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

import java.sql.Driver;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DataSourceFactory} for the Sybase IQ implementation, which has additional complications compared to other
 * DBMS types:
 * <ul>
 * <li>Returns an {@link IqDataSource} instead of a regular DataSource, as we need to have separate datasources and
 * connections across IQ schemas due to a lack of a DBO account in typical cases</li>
 * <li>Determining which driver to use, whether using JDBC or ODBC.</li>
 * </ul>
 */
public class IqJdbcDataSourceFactory implements DataSourceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(IqJdbcDataSourceFactory.class);

    private final ImmutableList<IqDataSourceFactory> iqDsFactories = Lists.immutable.<IqDataSourceFactory>with(new IqJconnDataSourceFactory(), new IqOdbcDataSourceFactory(), new IqOldOdbcDataSourceFactory());

    @Override
    public IqDataSource createDataSource(DbEnvironment env, Credential credential, int numThreads) {
        Class<? extends Driver> driverClass = env.getDriverClass();

        IqDataSourceFactory subDataSourceFactory = determineSubDataSourceFactory(env, driverClass);
        LOG.info("Chose IqDataSourceFactory: " + subDataSourceFactory.getClass());

        return new IqDataSource(env, credential, numThreads, subDataSourceFactory);
    }

    private IqDataSourceFactory determineSubDataSourceFactory(DbEnvironment env, Class<? extends Driver> driverClass) {
        for (IqDataSourceFactory factory : iqDsFactories) {
            if (factory.isDriverAccepted(driverClass)) {
                return factory;
            }
        }

        throw new IllegalArgumentException("Could not find suitable IqDataSourceFactory for driver " + driverClass + " from the given factories: " + iqDsFactories);
    }
}
