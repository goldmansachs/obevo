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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.impl.DaRoutinePojoImpl;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.RoutineType;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

public class Db2MetadataDialect extends AbstractMetadataDialect {
    private static final Logger LOG = LoggerFactory.getLogger(Db2MetadataDialect.class);

    @Override
    public String getSchemaExpression(String schemaName) {
        return "(?i)" + schemaName;
    }

    @Override
    public ImmutableCollection<DaRoutine> searchExtraRoutines(final DaSchema schema, String procedureName, Connection conn) throws SQLException {
        QueryRunner query = new QueryRunner();  // using queryRunner so that we can reuse the connection

        String procedureClause = procedureName == null ? "" : " AND R.ROUTINENAME = '" + procedureName + "'";
        final String sql = "SELECT ROUTINENAME, SPECIFICNAME, TEXT FROM SYSCAT.ROUTINES R WHERE R.ROUTINETYPE = 'F'\n" +
                "AND R.ROUTINESCHEMA = '" + schema.getName() + "'\n" + procedureClause;
        LOG.debug("Executing function metadata query SQL: {}", sql);

        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(query.query(conn,
                sql,
                new MapListHandler()
        )).toImmutable();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Results:");
            for (Map<String, Object> map : maps) {
                LOG.debug("ROW: {}", map.toString());
            }
        }

        return maps.collect(new Function<Map<String, Object>, DaRoutine>() {
            @Override
            public DaRoutine valueOf(Map<String, Object> map) {
                return new DaRoutinePojoImpl(
                        (String) map.get("ROUTINENAME"),
                        schema,
                        DaRoutineType.function,
                        (String) map.get("SPECIFICNAME"),
                        clobToString((Clob) map.get("TEXT"))
                );
            }
        });
    }

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, String schemaName) {
        DatabaseSpecificOverrideOptionsBuilder dbSpecificOptionsBuilder = super.getDbSpecificOptionsBuilder(conn, schemaName);

        // SEQTYPE <> 'I' is for identity columns; we don't want that when pulling user defined sequences
        dbSpecificOptionsBuilder.withInformationSchemaViews().withSequencesSql(
                "SELECT\n" +
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
                        "WHERE SEQSCHEMA = '" + schemaName + "' AND SEQTYPE <> 'I'\n" +
                        //"  SYSCAT.SEQUENCES.ORIGIN = 'U'\n" +
                        "ORDER BY\n" +
                        "  SYSCAT.SEQUENCES.SEQSCHEMA,\n" +
                        "  SYSCAT.SEQUENCES.SEQNAME\n" +
                        "WITH UR\n"
                );

        return dbSpecificOptionsBuilder;
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
    public void customEdits(SchemaCrawlerOptions options, Connection conn, String schemaName) {
        super.customEdits(options, conn, schemaName);

        // DB2 driver doesn't support function lookups; hence, we limit it here to avoid the error message and use the searchExtraRoutines method instead to pull them in.
        options.setRoutineTypes(Lists.immutable.with(RoutineType.procedure).castToList());
    }
}
