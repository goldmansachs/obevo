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
package com.gs.obevo.db.impl.platforms;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.jdbc.DefaultJdbcHandler;
import com.gs.obevo.db.impl.core.jdbc.JdbcHandler;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ExecuteChangeCommand;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;

public abstract class AbstractSqlExecutor implements SqlExecutor {
    private final DataSource ds;

    protected AbstractSqlExecutor(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public final JdbcHelper getJdbcTemplate() {
        // Note - pmdBroken value should be false, as otherwise the CSV inserts w/ prepared statements may not work.
        // If any sqls fail, then it may not be used correctly. This proved to be an annoyance w/ Sybase ASE, but
        // eventually we fixed it and can now assume this is false
        return createJdbcHelper(ds);
    }

    /**
     * Overload to facilitate creating the JdbcHelper given any datasource.
     */
    public JdbcHelper createJdbcHelper(DataSource ds) {
        return new JdbcHelper(this.getJdbcHandler(), this.isParameterTypeEnabled());
    }

    @Override
    public void executeWithinContext(PhysicalSchema schema, Procedure<Connection> runnable) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            setDataSourceSchema(conn, schema);
            runnable.value(conn);
        } catch (Exception e) {
            throw new DeployerRuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    @Override
    public <T> T executeWithinContext(PhysicalSchema schema, ThrowingFunction<Connection, T> callable) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            setDataSourceSchema(conn, schema);
            return callable.safeValueOf(conn);
        } catch (Exception e) {
            throw new DeployerRuntimeException(e);
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    @Override
    public void performExtraCleanOperation(final ExecuteChangeCommand command, final DbMetadataManager metaDataMgr) {
        // as a default no special extra steps
    }

    /**
     * Define a call for setting the schema on the datasource.
     * While most implementations can set the schema via simple SQL, Sybase IQ is an exceptional case that requires
     * an actual change in the DataSource; hence, we put this behind this interface
     */
    protected abstract void setDataSourceSchema(Connection conn, PhysicalSchema schema);

    protected JdbcHandler getJdbcHandler() {
        return new DefaultJdbcHandler();
    }

    protected boolean isParameterTypeEnabled() {
        return true;
    }
}
