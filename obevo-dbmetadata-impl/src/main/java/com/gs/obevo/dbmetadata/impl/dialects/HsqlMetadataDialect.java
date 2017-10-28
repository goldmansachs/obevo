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

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaUserTypeImpl;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.server.hsqldb.HyperSQLDatabaseConnector;

public class HsqlMetadataDialect extends AbstractMetadataDialect {

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema) {
        return new HyperSQLDatabaseConnector().getDatabaseSpecificOverrideOptionsBuilder();
    }

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        return "(?i).*\\.?" + physicalSchema.getPhysicalName();
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(final DaSchema schema, Connection conn) throws SQLException {
        QueryRunner query = new QueryRunner();

        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(query.query(conn,
                "select dom.DOMAIN_NAME AS USER_TYPE_NAME\n" +
                        "from INFORMATION_SCHEMA.DOMAINS dom\n" +
                        "WHERE dom.DOMAIN_SCHEMA = ucase('" + schema.getName() + "')\n",
                new MapListHandler()
        )).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaUserType>() {
            @Override
            public DaUserType valueOf(Map<String, Object> map) {
                return new DaUserTypeImpl((String) map.get("USER_TYPE_NAME"), schema);
            }
        });
    }
}
