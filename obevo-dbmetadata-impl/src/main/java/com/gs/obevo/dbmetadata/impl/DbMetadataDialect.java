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
package com.gs.obevo.dbmetadata.impl;

import java.sql.Connection;
import java.sql.SQLException;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaPackage;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import org.eclipse.collections.api.collection.ImmutableCollection;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Schema;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

public interface DbMetadataDialect {
    DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, PhysicalSchema physicalSchema);

    /**
     * Initializes the metadata class and the incoming options variable.
     * @param options The options object to be edited.
     * @param conn The connection to use to help w/ seting the options. Optional to use
     */
    void customEdits(SchemaCrawlerOptions options, Connection conn);

    /**
     * Sets the schema on the connection. This is needed prior to the schemacrawler calls for some DBMS types.
     */
    void setSchemaOnConnection(Connection conn, String schema);

    String getSchemaExpression(PhysicalSchema physicalSchema);

    String getTableExpression(PhysicalSchema physicalSchema, String tableName);

    String getRoutineExpression(PhysicalSchema physicalSchema, String procedureName);

    void validateDatabase(Catalog database, PhysicalSchema physicalSchema);

    ImmutableCollection<RuleBinding> getRuleBindings(DaSchema schema, Connection conn);

    ImmutableCollection<DaRoutine> searchExtraRoutines(DaSchema schema, String procedureName, Connection conn) throws SQLException;

    ImmutableCollection<DaPackage> searchPackages(DaSchema schema, String packageName, Connection conn) throws SQLException;

    ImmutableCollection<ExtraIndexInfo> searchExtraConstraintIndices(DaSchema schema, String tableName, Connection conn) throws SQLException;

    ImmutableCollection<ExtraRerunnableInfo> searchExtraViewInfo(DaSchema schema, String tableName, Connection conn) throws SQLException;

    ImmutableCollection<DaRule> searchRules(DaSchema schema, Connection conn) throws SQLException;

    ImmutableCollection<DaUserType> searchUserTypes(DaSchema schema, Connection conn) throws SQLException;

    /**
     * Overrides the routine type value in the {@link DaRoutine} if a value is returned. If null, then default to what
     * SchemaCrawler provides.
     */
    DaRoutineType getRoutineOverrideValue();

    /**
     * Indicates how we will read the desired catalog name from the SchemaCrawler {@link Schema} object.
     */
    SchemaStrategy getSchemaStrategy();
}
