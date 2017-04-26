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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Datasource to allow modifications of schemas in IQ using the schema user ID, instead of a dbo account. Some
 * installations of IQ in enterprises prefer not to use a dbo account, but to login as the schema user to execute
 * deployments. With this methodology, we create a separate datasource for each schema (using that schema's login) and
 * switch it in the setCurrentSchema method, transparent from clients
 */
public class IqDataSource implements DataSource {
    private static final Logger LOG = LoggerFactory.getLogger(IqSqlExecutor.class);

    private final DbEnvironment env;
    private final IqDataSourceFactory subDataSourceFactory;
    private final ImmutableMap<PhysicalSchema, DataSource> dsMap;

    /**
     * only used to help w/ the non-connection delegate methods of DataSource
     * actual connections should only be obtained via the currentConnection variable
     */
    private DataSource currentDataSource;

    public IqDataSource(DbEnvironment env, Credential userCredential, int numThreads, IqDataSourceFactory subDataSourceFactory) {
        this.env = env;
        this.subDataSourceFactory = subDataSourceFactory;

        MutableMap<PhysicalSchema, DataSource> dsMap = Maps.mutable.empty();
        for (PhysicalSchema physicalSchema : env.getPhysicalSchemas()) {
            String schema = physicalSchema.getPhysicalName();
            LOG.info("Creating datasource against schema {}", schema);
            DataSource ds = subDataSourceFactory.createDataSource(env,
                    userCredential,
                    schema,
                    numThreads
            );

            dsMap.put(physicalSchema, ds);
        }

        this.dsMap = dsMap.toImmutable();
        this.setCurrentSchema(this.env.getPhysicalSchemas().getFirst());  // set one arbitrarily as the default
    }

    public boolean isIqClientLoadSupported() {
        return subDataSourceFactory.isIqClientLoadSupported();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.currentDataSource.getConnection();
    }

    public void setCurrentSchema(PhysicalSchema schema) {
        this.currentDataSource = this.dsMap.get(schema);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException("Not supporting getConnection(String username, " +
                "String password) in obevo");
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.currentDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.currentDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.currentDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.currentDataSource.getLoginTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.currentDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.currentDataSource.isWrapperFor(iface);
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("To be implemented as getDataSource().getParentLogger() once default compile is 1.7");
    }
}
