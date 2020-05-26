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
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.impl.DaRoutinePojoImpl;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.RoutineType;
import schemacrawler.schemacrawler.InformationSchemaKey;
import schemacrawler.schemacrawler.LimitOptionsBuilderFixed;
import schemacrawler.schemacrawler.MetadataRetrievalStrategy;
import schemacrawler.schemacrawler.SchemaInfoMetadataRetrievalStrategy;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;

public class Db2MetadataDialect extends AbstractMetadataDialect {
    private static final Logger LOG = LoggerFactory.getLogger(Db2MetadataDialect.class);

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        return "(?i)" + physicalSchema.getPhysicalName();
    }

    @Override
    public ImmutableCollection<DaRoutine> searchExtraRoutines(final DaSchema schema, String procedureName, Connection conn) throws SQLException {
        String procedureClause = procedureName == null ? "" : " AND R.ROUTINENAME = '" + procedureName + "'";
        final String sql = "SELECT ROUTINENAME, SPECIFICNAME, TEXT FROM SYSCAT.ROUTINES R WHERE R.ROUTINETYPE = 'F'\n" +
                "AND R.ROUTINESCHEMA = '" + schema.getName() + "'\n" + procedureClause;
        LOG.debug("Executing function metadata query SQL: {}", sql);

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

        return maps.collect(map -> new DaRoutinePojoImpl(
                (String) map.get("ROUTINENAME"),
                schema,
                DaRoutineType.function,
                (String) map.get("SPECIFICNAME"),
                clobToString((Clob) map.get("TEXT"))
        ));
    }


    @Override
    public SchemaRetrievalOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema, boolean searchAllTables) throws IOException {
        SchemaRetrievalOptionsBuilder dbSpecificOptionsBuilder = super.getDbSpecificOptionsBuilder(conn, physicalSchema, searchAllTables);

        if (!searchAllTables) {
            // the default schemacrawler logic is optimized to search for all tables in DB2. But it is very slow for single-table lookups
            // See the original logic in the class DB2DatabaseConnector.
            dbSpecificOptionsBuilder.with(SchemaInfoMetadataRetrievalStrategy.tableColumnsRetrievalStrategy, MetadataRetrievalStrategy.metadata);
        }

        return dbSpecificOptionsBuilder;
    }

    @Override
    public MutableMap<InformationSchemaKey, String> getInfoSchemaSqlOverrides(PhysicalSchema physicalSchema) {
        // the SQL in SchemaCrawler does not define the VALID column, so we add it here
        String viewSql = "SELECT " +
                "  NULLIF(1, 1) " +
                "    AS TABLE_CATALOG, " +
                "  STRIP(SYSCAT.VIEWS.VIEWSCHEMA) " +
                "    AS TABLE_SCHEMA, " +
                "  STRIP(SYSCAT.VIEWS.VIEWNAME) " +
                "    AS TABLE_NAME, " +
                "  SYSCAT.VIEWS.TEXT " +
                "    AS VIEW_DEFINITION, " +
                "  CASE WHEN STRIP(SYSCAT.VIEWS.VIEWCHECK) = 'N' THEN 'NONE' ELSE 'CASCADED' END " +
                "    AS CHECK_OPTION, " +
                "  CASE WHEN STRIP(SYSCAT.VIEWS.READONLY) = 'Y' THEN 'NO' ELSE 'YES' END " +
                "    AS IS_UPDATABLE " +
                ", " +
                "  VALID   " +
                "FROM " +
                "  SYSCAT.VIEWS " +
                "WHERE VIEWSCHEMA = '" + physicalSchema.getPhysicalName() + "' " +
                "ORDER BY " +
                "  SYSCAT.VIEWS.VIEWSCHEMA, " +
                "  SYSCAT.VIEWS.VIEWNAME, " +
                "  SYSCAT.VIEWS.SEQNO " +
                "WITH UR   ";

        // SEQTYPE <> 'I' is for identity columns; we don't want that when pulling user defined sequences
        String sequencesSql = "SELECT\n" +
                "  NULLIF(1, 1)\n" +
                "    AS SEQUENCE_CATALOG,\n" +
                "  STRIP(SYSCAT.SEQUENCES.SEQSCHEMA)\n" +
                "    AS SEQUENCE_SCHEMA,\n" +
                "  STRIP(SYSCAT.SEQUENCES.SEQNAME)\n" +
                "    AS SEQUENCE_NAME,\n" +
                "  INCREMENT,\n" +
                "  MINVALUE AS MINIMUM_VALUE,\n" +
                "  MAXVALUE AS MAXIMUM_VALUE,\n" +
                "  CASE WHEN CYCLE = 'Y' THEN 'YES' ELSE 'NO' END AS CYCLE_OPTION,\n" +
                "  SEQID,\n" +
                "  SEQTYPE,\n" +
                "  START,\n" +
                "  NEXTCACHEFIRSTVALUE,\n" +
                "  CACHE,\n" +
                "  ORDER,\n" +
                "  CREATE_TIME,\n" +
                "  ALTER_TIME,\n" +
                "  REMARKS\n" +
                "FROM\n" +
                "  SYSCAT.SEQUENCES\n" +
                "WHERE SEQSCHEMA = '" + physicalSchema.getPhysicalName() + "' AND SEQTYPE <> 'I'\n" +

                //"  SYSCAT.SEQUENCES.ORIGIN = 'U'\n" +
                "ORDER BY\n" +
                "  SYSCAT.SEQUENCES.SEQSCHEMA,\n" +
                "  SYSCAT.SEQUENCES.SEQNAME\n" +
                "WITH UR\n";

        return Maps.mutable.of(
                InformationSchemaKey.VIEWS, viewSql,
                InformationSchemaKey.SEQUENCES, sequencesSql
        );
    }

    private String clobToString(Clob clob) {
        if (clob == null) {
            return null;
        }

        try {
            InputStream in = clob.getAsciiStream();
            StringWriter w = new StringWriter();
            IOUtils.copy(in, w);
            return w.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateLimitOptionsBuilder(LimitOptionsBuilderFixed options) {
        super.updateLimitOptionsBuilder(options);

        // DB2 driver doesn't support function lookups; hence, we limit it here to avoid the error message and use the searchExtraRoutines method instead to pull them in.
        options.routineTypes(Lists.immutable.with(RoutineType.procedure).castToList());
    }

    @Override
    public ImmutableSet<String> getGroupNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return Sets.immutable
                .withAll(jdbc.query(conn, "select ROLENAME from sysibm.SYSROLES", new ColumnListHandler<String>()))
                .newWithAll(jdbc.query(conn, "select GRANTEE from sysibm.SYSDBAUTH", new ColumnListHandler<String>()))
                .collect(StringFunctions.trim());  // db2 sometimes has whitespace in its return results that needs trimming
    }
}
