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
package com.gs.obevo.db.impl.core.jdbc;

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.inputreader.Credential;

/**
 * DataSourceFactory that will switch between an LDAP-lookup based implementation and a JDBC URL based implementation
 * depending on the parameters passed in (specifying {@link DbEnvironment#dbDataSourceName} will cause the LDAP-lookup
 * factory to be used.
 */
public class CompositeDataSourceFactory implements DataSourceFactory {
    private final DataSourceFactory dsLookupDataSourceFactory;
    private final DataSourceFactory jdbcUrlDataSourceFactory;

    public CompositeDataSourceFactory(DataSourceFactory dsLookupDataSourceFactory, DataSourceFactory jdbcUrlDataSourceFactory) {
        this.dsLookupDataSourceFactory = dsLookupDataSourceFactory;
        this.jdbcUrlDataSourceFactory = jdbcUrlDataSourceFactory;
    }

    @Override
    public DataSource createDataSource(DbEnvironment env, Credential credential, int numThreads) {
        if (env.getDbDataSourceName() != null) {
            return this.dsLookupDataSourceFactory.createDataSource(env, credential, numThreads);
        }

        return this.jdbcUrlDataSourceFactory.createDataSource(env, credential, numThreads);
    }
}
