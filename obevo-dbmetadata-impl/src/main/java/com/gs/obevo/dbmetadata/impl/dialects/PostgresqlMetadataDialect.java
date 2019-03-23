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
import com.gs.obevo.dbmetadata.api.DaExtension;
import com.gs.obevo.dbmetadata.api.DaExtensionImpl;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaUserTypeImpl;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.RoutineType;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

public class PostgresqlMetadataDialect extends AbstractMetadataDialect {
    private static final Logger LOG = LoggerFactory.getLogger(OracleMetadataDialect.class);

    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn) {
        // postgresql only supports FUNCTIONs in its syntax, not PROCEDUREs. However, the metadata still comes
        // back as procedure. We override the metadata value using the getRoutineOverrideValue method.
        options.setRoutineTypes(Lists.immutable.with(RoutineType.procedure).castToList());
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema, boolean searchAllTables) {
        DatabaseSpecificOverrideOptionsBuilder dbSpecificOptionsBuilder = super.getDbSpecificOptionsBuilder(conn, physicalSchema, searchAllTables);

        String sequenceSql = getSequenceSql(physicalSchema);
        if (sequenceSql != null) {
            // if null, then setting the sequences object to null won't help either
            dbSpecificOptionsBuilder.withInformationSchemaViews().withSequencesSql(sequenceSql);
        }

        return dbSpecificOptionsBuilder;
    }

    /**
     * Gets the sequence SQL for the JDBC metadata.
     * Protected visibility as subclasses may need to override.
     */
    protected String getSequenceSql(PhysicalSchema physicalSchema) {
        return "SELECT\n" +
                "  NULL AS SEQUENCE_CATALOG,\n" +
                "  SEQUENCE_SCHEMA,\n" +
                "  SEQUENCE_NAME,\n" +
                "  INCREMENT,\n" +
                "  MINIMUM_VALUE,\n" +
                "  MAXIMUM_VALUE,\n" +
                "  CYCLE_OPTION\n" +
                "FROM\n" +
                "  INFORMATION_SCHEMA.SEQUENCES\n" +
                "WHERE SEQUENCE_SCHEMA = '" + physicalSchema.getPhysicalName() + "'\n" +
                "ORDER BY\n" +
                "  SEQUENCE_CATALOG,\n" +
                "  SEQUENCE_SCHEMA,\n" +
                "  SEQUENCE_NAME\n";
    }

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        return "(?i)" + physicalSchema.getPhysicalName();
    }

    /**
     * PostgreSQL should override the routine type to "function". PostgreSQL only supports functions in its SQL syntax;
     * however, the JDBC metadata returns it as a "procedure". For consistency w/ the SQL syntax, we do this override.
     */
    @Override
    public DaRoutineType getRoutineOverrideValue() {
        return DaRoutineType.function;
    }

    @Override
    public ImmutableSet<DaExtension> getExtensionsOptional(Connection conn) throws SQLException {
        final String sql = "SELECT DISTINCT EXTNAME FROM PG_EXTENSION";
        LOG.debug("Executing extension metadata query SQL: {}", sql);

        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, sql, new MapListHandler())).toImmutable();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Results:");
            for (Map<String, Object> map : maps) {
                LOG.debug("ROW: {}", map.toString());
            }
        }

        return maps.collect(new Function<Map<String, Object>, DaExtension>() {
            @Override
            public DaExtension valueOf(Map<String, Object> map) {
                return new DaExtensionImpl((String) map.get("EXTNAME"));
            }
        }).toSet().toImmutable();
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(final DaSchema schema, Connection conn) {
        String sql = "SELECT t.typname \n" +
                "FROM        pg_type t \n" +
                "LEFT JOIN   pg_catalog.pg_namespace n ON n.oid = t.typnamespace \n" +
                "WHERE       (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)) \n" +
                "AND     NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid)\n" +
                "AND     n.nspname IN ('" + schema.getName() + "')";
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, sql, new MapListHandler())).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaUserType>() {
            @Override
            public DaUserType valueOf(Map<String, Object> map) {
                return new DaUserTypeImpl((String) map.get("typname"), schema);
            }
        });
    }

    @Override
    public ImmutableSet<String> getGroupNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return Sets.immutable
                .withAll(jdbc.query(conn, "select rolname from pg_roles", new ColumnListHandler<String>()))
                .collect(StringFunctions.trim());
    }
}
