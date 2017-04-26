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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaSequence;
import com.gs.obevo.dbmetadata.api.DaSequenceImpl;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schema.RoutineType;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

public class PostgresqlMetadataDialect extends AbstractMetadataDialect {
    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn, String schemaName) {
        // postgresql only supports FUNCTIONs in its syntax, not PROCEDUREs. However, the metadata still comes
        // back as procedure. We override the metadata value using the getRoutineOverrideValue method.
        options.setRoutineTypes(Lists.immutable.with(RoutineType.procedure).castToList());
    }

    @Override
    public ImmutableCollection<DaSequence> searchSequences(final DaSchema schema, Connection conn) throws SQLException {
        QueryRunner query = new QueryRunner();

        // SEQTYPE <> 'I' is for identity columns; we don't want that when pulling user defined sequences
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(query.query(conn,
                "select sequence_name as sequence_name\n" +
                        "from information_schema.sequences where sequence_schema = '" + schema.getName() + "'\n",
                new MapListHandler()
        )).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaSequence>() {
            @Override
            public DaSequence valueOf(Map<String, Object> map) {
                return new DaSequenceImpl((String) map.get("sequence_name"), schema);
            }
        });
    }

    @Override
    public String getSchemaExpression(String schemaName) {
        return "(?i)" + schemaName;
    }

    /**
     * PostgreSQL should override the routine type to "function". PostgreSQL only supports functions in its SQL syntax;
     * however, the JDBC metadata returns it as a "procedure". For consistency w/ the SQL syntax, we do this override.
     */
    @Override
    public DaRoutineType getRoutineOverrideValue() {
        return DaRoutineType.function;
    }
}
