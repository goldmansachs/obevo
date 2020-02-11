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
package com.gs.obevo.db.impl.core.jdbc;

import java.sql.Driver;
import java.util.Properties;

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.dbcp.BasicDataSource;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * DataSourceFactory that will attempt to connect to the target database by constructing a JDBC URL, whether taking
 * the one directly from the client or constructing it based on the {@link DbEnvironment#dbHost}/{@link DbEnvironment#dbPort}/{@link DbEnvironment#dbServer}/etc.
 * parameters.
 */
public abstract class JdbcDataSourceFactory implements DataSourceFactory {
    @Override
    public final DataSource createDataSource(DbEnvironment environment, Credential credential, int numThreads) {
        if (environment.getJdbcUrl() == null) {
            String url = this.createUrl(environment);
            // if we have to generate the URL, then let's set it back for convenience
            environment.setJdbcUrl(url);
        }

        return createFromJdbcUrl(environment.getDriverClass(), environment.getJdbcUrl(), credential,
                numThreads, environment.getDbTranslationDialect().getInitSqls(), getExtraConnectionProperties(credential));
    }

    /**
     * Return connection properties to initialize at startup. To be optionally overridden by subclasses.
     */
    @SuppressWarnings("WeakerAccess")
    protected Properties getExtraConnectionProperties(Credential credential) {
        return null;
    }

    protected abstract String createUrl(DbEnvironment env);

    public static DataSource createFromJdbcUrl(Class<? extends Driver> driverClass, String url,
            Credential credential) {
        return createFromJdbcUrl(driverClass, url, credential, 1);
    }

    public static DataSource createFromJdbcUrl(Class<? extends Driver> driverClass, String url,
            Credential credential, int numThreads) {
        return createFromJdbcUrl(driverClass, url, credential, numThreads, Lists.immutable.<String>empty(), new Properties());
    }

    private static DataSource createFromJdbcUrl(Class<? extends Driver> driverClass, String url,
            Credential credential, int numThreads, ImmutableList<String> initSqls, Properties extraConnectionProperties) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverClass.getName());
        // TODO validate non-null host name, notably for postgresl jdbc url
        dataSource.setUrl(url);
        dataSource.setUsername(credential.getUsername());
        dataSource.setPassword(credential.getPassword());

        // connection pool settings
        dataSource.setInitialSize(numThreads);
        dataSource.setMaxActive(numThreads);
        // keep the connections open if possible; only close them via the removeAbandonedTimeout feature
        dataSource.setMaxIdle(numThreads);
        dataSource.setMinIdle(0);
        dataSource.setRemoveAbandonedTimeout(300);

        dataSource.setConnectionInitSqls(initSqls.castToList());
        if (extraConnectionProperties != null) {
            for (String key : extraConnectionProperties.stringPropertyNames()) {
                dataSource.addConnectionProperty(key, extraConnectionProperties.getProperty(key));
            }
        }

        return dataSource;
    }
}
