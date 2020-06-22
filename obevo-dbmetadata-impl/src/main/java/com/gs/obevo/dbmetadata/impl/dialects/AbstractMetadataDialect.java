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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaDirectory;
import com.gs.obevo.dbmetadata.api.DaExtension;
import com.gs.obevo.dbmetadata.api.DaPackage;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import com.gs.obevo.dbmetadata.impl.DbMetadataDialect;
import com.gs.obevo.dbmetadata.impl.ExtraIndexInfo;
import com.gs.obevo.dbmetadata.impl.ExtraRerunnableInfo;
import com.gs.obevo.dbmetadata.impl.SchemaByNameStrategy;
import com.gs.obevo.dbmetadata.impl.SchemaStrategy;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.databaseconnector.DatabaseConnectorRegistry;

public abstract class AbstractMetadataDialect implements DbMetadataDialect {
    protected final JdbcHelper jdbc = new JdbcHelper();

    @Override
    public SchemaRetrievalOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema, boolean searchAllTables) throws IOException {
        DatabaseConnectorRegistry registry = DatabaseConnectorRegistry.getDatabaseConnectorRegistry();
        DatabaseConnector databaseConnector = registry.lookupDatabaseConnector(conn);
        return databaseConnector.getSchemaRetrievalOptionsBuilder(conn);
    }

    /**
     * Executes the given update statement on the connection - convenience method for subclasses.
     */
    void executeUpdate(Connection conn, String sql) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validateDatabase(Catalog database, PhysicalSchema physicalSchema) {
        if (database.getSchemas().size() != 1) {
            throw new IllegalArgumentException("Should find 1 schema only for schema " + physicalSchema + "; found "
                    + CollectionAdapter.adapt(database.getSchemas()).makeString(", "));
        }
    }

    @Override
    public final String getTableExpression(PhysicalSchema physicalSchema, String tableName) {
        return getSchemaExpression(physicalSchema) + "\\." + tableName;
    }

    @Override
    public final String getRoutineExpression(PhysicalSchema physicalSchema, String procedureName) {
        return getSchemaExpression(physicalSchema) + "\\." + procedureName;
    }

    @Override
    public ImmutableCollection<RuleBinding> getRuleBindings(DaSchema schema, Connection conn) {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaRoutine> searchExtraRoutines(DaSchema schema, String procedureName, Connection conn) throws SQLException {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<ExtraIndexInfo> searchExtraConstraintIndices(DaSchema schema, String tableName, Connection conn) throws SQLException {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<ExtraRerunnableInfo> searchExtraViewInfo(DaSchema schema, String tableName, Connection conn) throws SQLException {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaRule> searchRules(DaSchema schema, Connection conn) throws SQLException {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(DaSchema schema, Connection conn) throws SQLException {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaPackage> searchPackages(DaSchema schema, String packageName, Connection conn) throws SQLException {
        return Lists.immutable.empty();
    }

    @Override
    public DaRoutineType getRoutineOverrideValue() {
        return null;
    }

    @Override
    public SchemaStrategy getSchemaStrategy() {
        return SchemaByNameStrategy.INSTANCE;
    }

    @Override
    public ImmutableSet<String> getGroupNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return null;  // by default, we will not support these operations
    }

    @Override
    public ImmutableSet<String> getUserNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return null;  // by default, we will not support these operations
    }

    @Override
    public ImmutableSet<DaDirectory> getDirectoriesOptional(Connection conn) throws SQLException {
        return null;  // by default, we will not support these operations
    }

    @Override
    public ImmutableSet<DaExtension> getExtensionsOptional(Connection conn) throws SQLException {
        return null;  // by default, we will not support these operations
    }
}
