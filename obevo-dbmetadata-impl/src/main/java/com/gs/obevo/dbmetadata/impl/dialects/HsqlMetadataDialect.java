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
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaUserTypeImpl;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.server.hsqldb.HyperSQLDatabaseConnector;

public class HsqlMetadataDialect extends AbstractMetadataDialect {

    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema, boolean searchAllTables) {
        return new HyperSQLDatabaseConnector().getDatabaseSpecificOverrideOptionsBuilder();
    }

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        return "(?i).*\\.?" + physicalSchema.getPhysicalName();
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(final DaSchema schema, Connection conn) throws SQLException {
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn,
                "select dom.DOMAIN_NAME AS USER_TYPE_NAME\n" +
                        "from INFORMATION_SCHEMA.DOMAINS dom\n" +
                        "WHERE dom.DOMAIN_SCHEMA = ucase('" + schema.getName() + "')\n",
                new MapListHandler()
        )).toImmutable();

        return maps.collect(map -> new DaUserTypeImpl((String) map.get("USER_TYPE_NAME"), schema));
    }

    @Override
    public ImmutableSet<String> getGroupNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return Sets.immutable.withAll(jdbc.query(conn, "select ROLE_NAME from INFORMATION_SCHEMA.APPLICABLE_ROLES", new ColumnListHandler<>()));
    }

    @Override
    public ImmutableSet<String> getUserNamesOptional(Connection conn, PhysicalSchema physicalSchema) throws SQLException {
        return Sets.immutable.withAll(jdbc.query(conn, "select USER_NAME from INFORMATION_SCHEMA.SYSTEM_USERS", new ColumnListHandler<>()));
    }
}
