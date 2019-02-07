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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaDirectory;
import com.gs.obevo.dbmetadata.api.DaDirectoryImpl;
import com.gs.obevo.dbmetadata.api.DaPackage;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaUserTypeImpl;
import com.gs.obevo.dbmetadata.impl.DaPackagePojoImpl;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.crawl.MetadataRetrievalStrategy;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;

/**
 * Oracle DBMS metadata dialect.
 */
public class OracleMetadataDialect extends AbstractMetadataDialect {
    private static final Logger LOG = LoggerFactory.getLogger(OracleMetadataDialect.class);

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        return physicalSchema.getPhysicalName();
    }

    @Override
    public ImmutableSet<String> getGroupNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return Sets.immutable
                .withAll(jdbc.query(conn, "select ROLE from DBA_ROLES", new ColumnListHandler<String>()))
                .collect(StringFunctions.trim());
    }

    @Override
    public ImmutableCollection<DaPackage> searchPackages(final DaSchema schema, String procedureName, Connection conn) throws SQLException {
        String procedureClause = procedureName == null ? "" : " AND OBJECT_NAME = '" + procedureName + "'";
        final String sql = "SELECT OBJECT_NAME, OBJECT_TYPE FROM ALL_OBJECTS\n" +
                "WHERE OBJECT_TYPE IN ('PACKAGE')\n" +
                "AND OWNER = '" + schema.getName() + "'\n" +
                procedureClause;
        LOG.debug("Executing package metadata query SQL: {}", sql);

        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn,
                sql,
                new MapListHandler()
        )).toImmutable();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Results:");
            for (Map<String, Object> map : maps) {
                LOG.debug("ROW: {}", map.toString());
            }
        }

        return maps.collect(new Function<Map<String, Object>, DaPackage>() {
            @Override
            public DaPackage valueOf(Map<String, Object> map) {
                return new DaPackagePojoImpl((String) map.get("OBJECT_NAME"), schema);
            }
        });
    }

    @Override
    public ImmutableSet<DaDirectory> getDirectoriesOptional(Connection conn) throws SQLException {
        final String sql = "SELECT DIRECTORY_NAME, DIRECTORY_PATH FROM DBA_DIRECTORIES";
        LOG.debug("Executing directory metadata query SQL: {}", sql);

        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, sql, new MapListHandler())).toImmutable();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Results:");
            for (Map<String, Object> map : maps) {
                LOG.debug("ROW: {}", map.toString());
            }
        }

        return maps.collect(new Function<Map<String, Object>, DaDirectory>() {
            @Override
            public DaDirectory valueOf(Map<String, Object> map) {
                return new DaDirectoryImpl((String) map.get("DIRECTORY_NAME"), (String) map.get("DIRECTORY_PATH"));
            }
        }).toSet().toImmutable();
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(final DaSchema schema, Connection conn) {
        String sql = "SELECT TYPE_NAME " +
                "FROM ALL_TYPES " +
                "WHERE OWNER = '" + schema.getName() + "'";
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, sql, new MapListHandler())).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaUserType>() {
            @Override
            public DaUserType valueOf(Map<String, Object> map) {
                return new DaUserTypeImpl((String) map.get("TYPE_NAME"), schema);
            }
        });
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema, boolean searchAllTables) {
        DatabaseSpecificOverrideOptionsBuilder builder = super.getDbSpecificOptionsBuilder(conn, physicalSchema, searchAllTables);

        if (!searchAllTables) {
            // the default schemacrawler logic is optimized to search for all tables in Oracle. But it is very slow for single-table lookups
            // See the original logic in the class OracleDatabaseConnector.
            builder.withTableColumnRetrievalStrategy(MetadataRetrievalStrategy.metadata)
                    .withForeignKeyRetrievalStrategy(MetadataRetrievalStrategy.metadata)
                    .withIndexRetrievalStrategy(MetadataRetrievalStrategy.metadata);
        }
        return builder;
    }
}
