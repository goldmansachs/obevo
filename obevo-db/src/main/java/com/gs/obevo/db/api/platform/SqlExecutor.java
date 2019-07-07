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
package com.gs.obevo.db.api.platform;

import java.sql.Connection;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.AuditLock;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ExecuteChangeCommand;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;

/**
 * The interface through which to access the database for {@link DbPlatform} implementations.
 *
 * While JdbcHelper does the actual SQL invocation, we must access it from this class due to the complexities around
 * accessing the data source, notably that:
 * 1) We need to set the schema for the duration of a connection across many API calls
 * 2) For Sybase IQ, we must explicitly have a different connection to change schemas as edits are done by connecting
 * as that schema user.
 *
 * Along with that, this class is used to "set the context" of the data source, i.e. assign a single connection of the
 * data source to a thread. This is akin to a transaction manager or txn management context in JTA
 *
 * This class is a bit convoluted now; could be simpler to just resort to passing a connection around. That will be a
 * refactor for a later time...
 */
public interface SqlExecutor {
    /**
     * Returns the JdbcHelper for accessing the database. The "context" methods must be used in conjunction with this.
     */
    JdbcHelper getJdbcTemplate();

    /**
     * Convenience method that will execute the given runnable while wrapping the setContext/unsetContext methods in
     * a try-finally block.
     */
    void executeWithinContext(PhysicalSchema schema, Procedure<Connection> runnable);

    /**
     * Convenience method that will execute the given runnable while wrapping the setContext/unsetContext methods in
     * a try-finally block.
     */
    <T> T executeWithinContext(PhysicalSchema schema, ThrowingFunction<Connection, T> callable);

    /**
     * Unused operation.
     *
     * @deprecated Do not use
     */
    @Deprecated
    void performExtraCleanOperation(ExecuteChangeCommand command, DbMetadataManager metaDataMgr);

    AuditLock lock(Connection conn);
}
