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
package com.gs.obevo.dbmetadata.api;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.set.ImmutableSet;

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
     * @deprecated Use the PhysicalSchema overload: {@link #getDatabase(PhysicalSchema, DaSchemaInfoLevel, boolean, boolean)}.
     */
    @Deprecated
    DaCatalog getDatabase(String physicalSchema, DaSchemaInfoLevel schemaInfoLevel, boolean searchAllTables,
            boolean searchAllRoutines);

    /**
     * Returns the metadata for a whole catalog. Info level (e.g. for specific object types or for detail-level) can be
     * configured via the parameters.
     *
     * @throws IllegalArgumentException if the schema does not exist
     * @since 6.4.0
     */
    DaCatalog getDatabase(PhysicalSchema physicalSchema, DaSchemaInfoLevel schemaInfoLevel, boolean searchAllTables,
            boolean searchAllRoutines);

    /**
     * Returns a DaCatalog reference if that catalog exists - this serves as a quick "exists" check on that catalog.
     *
     * @deprecated Use {@link #getDatabaseOptional(String)}, as it is more clearly named.
     */
    @Deprecated
    DaCatalog getDatabase(String physicalSchema);

    /**
     * Returns a DaCatalog reference if that catalog exists - this serves as a quick "exists" check on that catalog.
     *
     * @since 6.3.0
     */
    DaCatalog getDatabaseOptional(String physicalSchema);

    /**
     * Returns a DaCatalog reference if that catalog exists - this serves as a quick "exists" check on that catalog.
     *
     * @since 6.4.0
     */
    DaCatalog getDatabaseOptional(PhysicalSchema physicalSchema);

    /**
     * Retrieves the metadata for the requested table.
     *
     * @deprecated Use the PhysicalSchema overload {@link #getTableInfo(PhysicalSchema, String)}
     */
    @Deprecated
    DaTable getTableInfo(String physicalSchema, String tableName);

    /**
     * Retrieves the metadata for the requested table.
     *
     * @deprecated Use the PhysicalSchema overload {@link #getTableInfo(PhysicalSchema, String, DaSchemaInfoLevel)}
     */
    @Deprecated
    DaTable getTableInfo(String physicalSchema, String tableName, DaSchemaInfoLevel schemaInfoLevel);

    /**
     * Retrieves the metadata for the requested table.
     *
     * @since 6.4.0
     */
    DaTable getTableInfo(PhysicalSchema physicalSchema, String tableName);

    /**
     * Retrieves the metadata for the requested table.
     *
     * @since 6.4.0
     */
    DaTable getTableInfo(PhysicalSchema physicalSchema, String tableName, DaSchemaInfoLevel schemaInfoLevel);

    /**
     * Retrieves the metadata for the requested routine.
     *
     * @deprecated Use the PhysicalSchema overload {@link #getRoutineInfo(PhysicalSchema, String)}
     */
    @Deprecated
    ImmutableCollection<DaRoutine> getProcedureInfo(String physicalSchema, String procedureName);

    /**
     * Retrieves the metadata for the requested routine.
     *
     * @deprecated Use the PhysicalSchema overload {@link #getRoutineInfo(PhysicalSchema, String, DaSchemaInfoLevel)}
     */
    @Deprecated
    ImmutableCollection<DaRoutine> getProcedureInfo(String physicalSchema, String procedureName, DaSchemaInfoLevel schemaInfoLevel);

    /**
     * Retrieves the metadata for the requested routine.
     *
     * @since 6.4.0
     */
    ImmutableCollection<DaRoutine> getRoutineInfo(PhysicalSchema physicalSchema, String routineName);

    /**
     * Retrieves the metadata for the requested routine.
     *
     * @since 6.4.0
     */
    ImmutableCollection<DaRoutine> getRoutineInfo(PhysicalSchema physicalSchema, String routineName, DaSchemaInfoLevel schemaInfoLevel);

    /**
     * Retrieves the groups setup at the database or schema level, or null if the dialect doesn't support this operation.
     * @since 6.6.0
     * @param physicalSchema
     */
    ImmutableSet<String> getGroupNamesOptional(PhysicalSchema physicalSchema);

    /**
     * Retrieves the users setup at the database or schema level, or null if the dialect doesn't support this operation.
     * @since 6.6.0
     * @param physicalSchema
     */
    ImmutableSet<String> getUserNamesOptional(PhysicalSchema physicalSchema);

    /**
     * Retrieves the directory objects setup at the database level; pertinent for Oracle only.
     * @since 6.6.0
     */
    ImmutableSet<String> getDirectoryNamesOptional();
}
