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
package com.gs.obevo.db.impl.platforms.postgresql.changetypes;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.changetypes.DbSimpleArtifactDeployer;
import com.gs.obevo.db.impl.core.changetypes.GrantChangeParser;
import com.gs.obevo.db.impl.core.changetypes.RerunnableDbChangeTypeBehavior;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.graph.GraphEnricher;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

public class PostgreSqlFunctionChangeTypeBehavior extends RerunnableDbChangeTypeBehavior {
    public PostgreSqlFunctionChangeTypeBehavior(DbEnvironment env, DbChangeType dbChangeType, SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, GrantChangeParser grantChangeParser, GraphEnricher graphEnricher, DbPlatform dbPlatform, DbMetadataManager dbMetadataManager) {
        super(env, dbChangeType, sqlExecutor, baseArtifactDeployer, grantChangeParser, graphEnricher, dbPlatform, dbMetadataManager);
    }

    /**
     * Functions need to be referred by their signatures for drops and grants in postgresql
     * For functions query info:
     * https://www.postgresql.org/docs/9.5/static/functions-info.html
     * https://www.postgresql.org/docs/9.5/static/catalog-pg-proc.html
     * https://www.postgresql.org/docs/9.5/static/catalog-pg-namespace.html
     */
    @Override
    public Pair<Boolean, RichIterable<String>> getQualifiedObjectNames(Connection conn, PhysicalSchema physicalSchema, String objectName) {
        String schemaName = getDbPlatform().convertDbObjectName().valueOf(physicalSchema.getPhysicalName());
        String functionNameWithCase = getDbPlatform().convertDbObjectName().valueOf(objectName);

        String sql = "select format('%s.%s(%s)',n.nspname, p.proname, pg_get_function_identity_arguments(p.oid)) " +
                "as functionname\n" +
                "FROM   pg_proc p\n" +
                "LEFT JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace\n" +
                "WHERE n.nspname = '" + schemaName + "' and p.proname = '" + functionNameWithCase + "'";

        List<Map<String, Object>> funcNameResults = getSqlExecutor().getJdbcTemplate().queryForList(conn, sql);

        MutableList<String> names = Lists.mutable.empty();
        for (Map<String, Object> funcNameResult : funcNameResults) {
            names.add((String) funcNameResult.get("functionname"));
        }

        return Tuples.<Boolean, RichIterable<String>>pair(false, names);
    }

    @Override
    protected String generateDropChangeRaw(Connection conn, Change change) {
        StringBuilder sb = new StringBuilder();
        for (String functionName : this.getQualifiedObjectNames(conn, change.getPhysicalSchema(env), change.getObjectName()).getTwo()) {
            sb.append("DROP FUNCTION ").append(functionName).append(";\n");
        }

        return sb.toString();
    }
}
