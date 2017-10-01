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
package com.gs.obevo.dbmetadata.api;

import javax.sql.DataSource;

import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * API for accessing the DB object metadata for a particular schema/catalog in a database server.
 */
public interface DbMetadataManager {
    void setDataSource(DataSource ds);

    /**
     * Returns the metadata for a whole catalog. Info level (e.g. for specific object types or for detail-level) can be
     * configured via the parameters.
     *
     * @throws IllegalArgumentException if the schema does not exist
     */
    DaCatalog getDatabase(String physicalSchema, DaSchemaInfoLevel schemaInfoLevel, boolean searchAllTables,
            boolean searchAllProcedures);

    /**
     * Returns a DaCatalog reference if that catalog exists - this serves as a quick "exists" check on that catalog.
     * @deprecated Use {@link #getDatabaseOptional(String)}, as it is more clearly named.
     */
    @Deprecated
    DaCatalog getDatabase(String physicalSchema);

    /**
     * Returns a DaCatalog reference if that catalog exists - this serves as a quick "exists" check on that catalog.
     */
    DaCatalog getDatabaseOptional(String physicalSchema);

    DaTable getTableInfo(String physicalSchema, String tableName);

    DaTable getTableInfo(String physicalSchema, String tableName, DaSchemaInfoLevel schemaInfoLevel);

    ImmutableCollection<DaRoutine> getProcedureInfo(String physicalSchema, String procedureName);

    ImmutableCollection<DaRoutine> getProcedureInfo(String physicalSchema, String procedureName, DaSchemaInfoLevel schemaInfoLevel);
}
