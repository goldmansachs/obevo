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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common baseclass for {@link IqDataSourceFactory} implementations.
 */
public abstract class AbstractIqDataSourceFactory implements IqDataSourceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIqDataSourceFactory.class);

    @Override
    public final DataSource createDataSource(DbEnvironment env, Credential credential, String schema, int numThreads) {
        String url;
        String password;
        if (env.getJdbcUrl() != null) {
            url = env.getJdbcUrl();
            password = credential.getPassword();
        } else {
            Pair<String, String> urlPasswordPair = this.getUrl(env, schema, credential);
            url = urlPasswordPair.getOne();
            password = urlPasswordPair.getTwo() != null ? urlPasswordPair.getTwo() : credential.getPassword();
        }

        LOG.info("Connecting using URL: {}", url);
        Credential schemaCredential = new Credential(schema, password);
        return JdbcDataSourceFactory.createFromJdbcUrl(env.getDriverClass(), url, schemaCredential, numThreads);
    }

    protected abstract Pair<String, String> getUrl(DbEnvironment env, String schema, Credential credential);
}
