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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaRuleImpl;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaUserTypeImpl;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import com.gs.obevo.dbmetadata.impl.DaRoutinePojoImpl;
import com.gs.obevo.dbmetadata.impl.RuleBindingImpl;
import com.gs.obevo.dbmetadata.impl.SchemaByCatalogStrategy;
import com.gs.obevo.dbmetadata.impl.SchemaStrategy;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.RoutineType;
import schemacrawler.schema.Schema;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

/**
 * Metadata dialect for MS SQL.
 *
 * See here for information on the metadata tables:
 * <a href="https://docs.microsoft.com/en-us/sql/relational-databases/system-information-schema-views/system-information-schema-views-transact-sql">MSSQL</a>,
 * <a href="http://infocenter.sybase.com/archive/index.jsp?topic=/com.sybase.help.ase_15.0.tables/html/tables/tables25.htm">Sybase ASE</a>
 */
public class MsSqlMetadataDialect extends AbstractMetadataDialect {
    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn) {
        // MS SQL driver supports SP metadata, but not functions. As a result, we must disable SchemaCrawler's own
        // lookups entirely and use our own query. (SchemaCrawler's inherent behavior for the SQL only adds to existing
        // routine data, not loading in entire new ones).
        options.setRoutineTypes(Lists.mutable.<RoutineType>empty());
    }

    @Override
    public void setSchemaOnConnection(Connection conn, PhysicalSchema physicalSchema) {
        executeUpdate(conn, "use " + physicalSchema.getPhysicalName());
    }

    @Override
    public String getSchemaExpression(PhysicalSchema physicalSchema) {
        String subschema = ObjectUtils.defaultIfNull(physicalSchema.getSubschema(), "dbo");
        return physicalSchema.getPhysicalName() + "\\." + subschema;
    }

    @Override
    public void validateDatabase(Catalog database, final PhysicalSchema physicalSchema) {
        MutableCollection<Schema> schemasWithIncorrectCatalog = CollectionAdapter.adapt(database.getSchemas()).reject(new Predicate<Schema>() {
            @Override
            public boolean accept(Schema each) {
                return each.getCatalogName().equals(physicalSchema.getPhysicalName());
            }
        });

        if (schemasWithIncorrectCatalog.notEmpty()) {
            throw new IllegalArgumentException("Returned ASE schemas should be in " + physicalSchema.getPhysicalName() + " catalog; however, these were not: " + schemasWithIncorrectCatalog);
        }
    }

    @Override
    public ImmutableCollection<RuleBinding> getRuleBindings(DaSchema schema, Connection conn) {
        String schemaName = schema.getName();
        // return the bindings to columns and bindings to domains
        String sql = "select tab.name 'object', rul.name 'rule', " +
                "'sp_bindrule ' + rul.name + ', ''' + tab.name + '.' + col.name + '''' 'sql'\n" +
                "from " + schemaName + "..syscolumns col, " + schemaName + "..sysobjects rul, " + schemaName + "..sysobjects tab\n" +
                "    , sys.schemas sch\n" +
                "where col.domain = rul.id and col.id = tab.id and tab.type='U' and col.domain <> 0\n" +
                "    and tab.uid = sch.schema_id and sch.name = '" + schema.getSubschemaName() + "'\n" +
                "union\n" +
                "select obj.name 'object', rul.name 'rule', " +
                "'sp_bindrule ' + rul.name + ', ' + obj.name 'sql'\n" +
                "from " + schemaName + "..systypes obj, " + schemaName + "..sysobjects rul\n" +
                "    , sys.schemas sch\n" +
                "where obj.domain = rul.id and obj.domain <> 0\n" +
                "    and obj.uid = sch.schema_id and sch.name = '" + schema.getSubschemaName() + "'\n";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            MutableList<RuleBinding> ruleBindings = Lists.mutable.empty();
            while (rs.next()) {
                RuleBindingImpl ruleBinding = new RuleBindingImpl();
                ruleBinding.setObject(rs.getString("object"));
                ruleBinding.setRule(rs.getString("rule"));
                ruleBinding.setSql(rs.getString("sql"));
                ruleBindings.add(ruleBinding);
            }
            return ruleBindings.toImmutable();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(ps);
        }
    }

    @Override
    public ImmutableCollection<DaRoutine> searchExtraRoutines(final DaSchema schema, String procedureName, Connection conn) throws SQLException {
        String nameClause = procedureName != null ? " and ROUTINE_NAME = '" + procedureName + "'\n" : " ";

        String query = "SELECT" +
                "    ROUTINE_CATALOG," +
                "    ROUTINE_SCHEMA," +
                "    ROUTINE_NAME," +
                "    SPECIFIC_NAME," +
                "    ROUTINE_TYPE," +
                "    OBJECT_DEFINITION(OBJECT_ID(ROUTINE_CATALOG + '.' + ROUTINE_SCHEMA + '.' + ROUTINE_NAME)) AS ROUTINE_DEFINITION" +
                " FROM INFORMATION_SCHEMA.ROUTINES" +
                " WHERE ROUTINE_CATALOG = '" + schema.getName() + "'" +
                " AND ROUTINE_SCHEMA = '" + schema.getSubschemaName() + "'" +
                nameClause;
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, query, new MapListHandler())).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaRoutine>() {
            @Override
            public DaRoutine valueOf(Map<String, Object> object) {
                DaRoutineType routineType = DaRoutineType.valueOf(((String) object.get("ROUTINE_TYPE")).toLowerCase());
                return new DaRoutinePojoImpl(
                        (String) object.get("ROUTINE_NAME"),
                        schema,
                        routineType,
                        (String) object.get("SPECIFIC_NAME"),
                        (String) object.get("ROUTINE_DEFINITION")
                );
            }
        });
    }

    @Override
    public ImmutableCollection<DaRule> searchRules(final DaSchema schema, Connection conn) throws SQLException {
        // Do not use ANSI JOIN as it does not work in Sybase 11.x - the SQL below works across all versions
        String sql = "SELECT rul.name as RULE_NAME\n" +
                "FROM " + schema.getName() + "..sysobjects rul\n" +
                "    , sys.schemas sch\n" +
                "WHERE rul.type = 'R'\n" +
                "    and rul.uid = sch.schema_id and sch.name = '" + schema.getSubschemaName() + "' " +
                "and not exists (\n" +
                "\t-- Ensure that the entry is not attached to a table; otherwise, it is a regular table constraint, and will already be dropped when the table is dropped\n" +
                "\tselect 1 from " + schema.getName() + "..sysconstraints c\n" +
                "\twhere c.constid = rul.id\n" +
                ")\n";
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, sql, new MapListHandler())).toImmutable();

        return maps.collect(map -> new DaRuleImpl((String) map.get("RULE_NAME"), schema));
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(final DaSchema schema, Connection conn) throws SQLException {
        String sql = "SELECT DOMAIN_NAME as USER_TYPE_NAME " +
                "FROM INFORMATION_SCHEMA.DOMAINS " +
                "WHERE DOMAIN_CATALOG = '" + schema.getName() + "' " +
                "AND DOMAIN_SCHEMA = '" + schema.getSubschemaName() + "'";
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(jdbc.query(conn, sql, new MapListHandler())).toImmutable();

        return maps.collect(map -> new DaUserTypeImpl((String) map.get("USER_TYPE_NAME"), schema));
    }

    @Override
    public SchemaStrategy getSchemaStrategy() {
        // Sybase stores the "database"/catalog first, then the schema. schema is usually meaningless for ASE, i.e. dbo value
        return SchemaByCatalogStrategy.INSTANCE;
    }
}
