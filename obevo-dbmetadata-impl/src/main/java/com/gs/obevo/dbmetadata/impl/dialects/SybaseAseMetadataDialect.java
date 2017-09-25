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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaRule;
import com.gs.obevo.dbmetadata.api.DaRuleImpl;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaUserType;
import com.gs.obevo.dbmetadata.api.DaUserTypeImpl;
import com.gs.obevo.dbmetadata.api.RuleBinding;
import com.gs.obevo.dbmetadata.impl.DaRoutinePojoImpl;
import com.gs.obevo.dbmetadata.impl.ExtraIndexInfo;
import com.gs.obevo.dbmetadata.impl.ExtraRerunnableInfo;
import com.gs.obevo.dbmetadata.impl.RuleBindingImpl;
import com.gs.obevo.dbmetadata.impl.SchemaByCatalogStrategy;
import com.gs.obevo.dbmetadata.impl.SchemaStrategy;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.RoutineType;
import schemacrawler.schema.Schema;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

/**
 * Metadata dialect for Sybase ASE.
 *
 * See here for information on the metadata tables: http://infocenter.sybase.com/archive/index.jsp?topic=/com.sybase.help.ase_15.0.tables/html/tables/tables25.htm
 *
 * TODO Add support for Sybase DEFAULTs?
 *      But, we don't have a way yet to distinguish between pre-defined defaults (i.e. Omni) and regular defaults
 *      SELECT 10 ord, 'DEFAULT' objtype, s1.name objname, 'DROP DEFAULT ' +  s1.name sqlstatement
 *      FROM ${schemaName}..sysobjects s1
 *      WHERE s1.type = 'D'
 */
public class SybaseAseMetadataDialect extends AbstractMetadataDialect {
    @Override
    public DatabaseSpecificOverrideOptionsBuilder getDbSpecificOptionsBuilder(Connection conn, String schemaName) {
        return new DatabaseSpecificOverrideOptionsBuilder();
    }

    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn, String schemaName) {
        options.getSchemaInfoLevel().setRetrieveDatabaseInfo(false);  // Fails for Sybase when connections are retrieved using Sybase's native JDBC pools

        // Sybase driver supports SP metadata, but not functions. As a result, we must disable SchemaCrawler's own
        // lookups entirely and use our own query. (SchemaCrawler's inherent behavior for the SQL only adds to existing
        // routine data, not loading in entire new ones).
        options.setRoutineTypes(Lists.mutable.<RoutineType>empty());
    }

    @Override
    public void setSchemaOnConnection(Connection conn, String schema) {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement("use " + schema);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DbUtils.closeQuietly(ps);
        }
    }

    @Override
    public String getSchemaExpression(String schemaName) {
        // Start w/ the catalog name, then take any word string for the second part (i.e. schema name like dbo or DACT_RO)
        return schemaName + "\\.\\w*";
    }

    @Override
    public void validateDatabase(Catalog database, final String schema) {
        MutableCollection<Schema> schemasWithIncorrectCatalog = CollectionAdapter.adapt(database.getSchemas()).reject(new Predicate<Schema>() {
            @Override
            public boolean accept(Schema each) {
                return each.getCatalogName().equals(schema);
            }
        });

        if (schemasWithIncorrectCatalog.notEmpty()) {
            throw new IllegalArgumentException("Returned ASE schemas should be in " + schema + " catalog; however, these were not: " + schemasWithIncorrectCatalog);
        }
    }

    @Override
    public ImmutableCollection<RuleBinding> getRuleBindings(DaSchema schema, Connection conn) {
        String schemaName = schema.getName();
        // return the bindings to columns and bindings to domains
        String sql = "select tab.name 'object', rul.name 'rule', " +
                "'sp_bindrule ' || rul.name || ', ''' || tab.name || '.' || col.name || '''' 'sql'\n" +
                "from " + schemaName + "..syscolumns col, " + schemaName + "..sysobjects rul, " + schemaName + "..sysobjects tab\n" +
                "where col.domain = rul.id and col.id = tab.id and tab.type='U' and col.domain <> 0\n" +
                "union\n" +
                "select obj.name 'object', rul.name 'rule', " +
                "'sp_bindrule ' || rul.name || ', ' || obj.name 'sql'\n" +
                "from " + schemaName + "..systypes obj, " + schemaName + "..sysobjects rul\n" +
                "where obj.domain = rul.id and obj.domain <> 0\n";
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
    public ImmutableCollection<ExtraIndexInfo> searchExtraConstraintIndices(DaSchema schema, String tableName, Connection conn) throws SQLException {
        QueryRunner query = new QueryRunner();  // using queryRunner so that we can reuse the connection

        // Do not use ANSI JOIN as it does not work in Sybase 11.x - the SQL below works across all versions
        String tableClause = tableName == null ? "" : " AND tab.name = '" + tableName + "'";
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(query.query(conn,
                "select tab.name TABLE_NAME, ind.name INDEX_NAME, status2 & 8 IS_CONSTRAINT, status2 & 512 IS_CLUSTERED " +
                        "from " + schema.getName() + "..sysindexes ind, " + schema.getName() + "..sysobjects tab " +
                        "where ind.id = tab.id " + tableClause,
                new MapListHandler()
        )).toImmutable();

        return maps.collect(new Function<Map<String, Object>, ExtraIndexInfo>() {
            @Override
            public ExtraIndexInfo valueOf(Map<String, Object> map) {
                return new ExtraIndexInfo(
                        (String) map.get("TABLE_NAME"),
                        (String) map.get("INDEX_NAME"),
                        (Integer) map.get("IS_CONSTRAINT") != 0,
                        (Integer) map.get("IS_CLUSTERED") != 0
                );
            }
        });
    }

    @Override
    public ImmutableCollection<ExtraRerunnableInfo> searchExtraViewInfo(DaSchema schema, String tableName, Connection conn) throws SQLException {
        String query = String.format("select obj.name name, com.number number, colid2 colid2, colid colid, text text\n" +
                "from %1$s..syscomments com\n" +
                ", %1$s..sysobjects obj\n" +
                "where com.id = obj.id\n" +
                "and com.texttype = 0\n" +
                "and obj.type in ('V')\n" +
                "order by com.id, number, colid2, colid\n", schema.getName());
        QueryRunner qr = new QueryRunner();  // using queryRunner so that we can reuse the connection
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(qr.query(conn, query, new MapListHandler())).toImmutable();

        ImmutableList<ExtraRerunnableInfo> viewInfos = maps.collect(new Function<Map<String, Object>, ExtraRerunnableInfo>() {
            @Override
            public ExtraRerunnableInfo valueOf(Map<String, Object> object) {
                return new ExtraRerunnableInfo(
                        (String) object.get("name"),
                        null,
                        (String) object.get("text"),
                        null,
                        ((Integer) object.get("colid2")).intValue(),
                        ((Integer) object.get("colid")).intValue()
                );
            }
        });

        return viewInfos.groupBy(ExtraRerunnableInfo.TO_NAME).multiValuesView().collect(new Function<RichIterable<ExtraRerunnableInfo>, ExtraRerunnableInfo>() {
            @Override
            public ExtraRerunnableInfo valueOf(RichIterable<ExtraRerunnableInfo> objectInfos) {
                MutableList<ExtraRerunnableInfo> sortedInfos = objectInfos.toSortedList(Comparators.fromFunctions(ExtraRerunnableInfo.TO_ORDER2, ExtraRerunnableInfo.TO_ORDER1));
                StringBuilder definitionString = sortedInfos.injectInto(new StringBuilder(), new Function2<StringBuilder, ExtraRerunnableInfo, StringBuilder>() {
                    @Override
                    public StringBuilder value(StringBuilder sb, ExtraRerunnableInfo rerunnableInfo) {
                        return sb.append(rerunnableInfo.getDefinition());
                    }
                });
                return new ExtraRerunnableInfo(
                        sortedInfos.get(0).getName(),
                        null,
                        definitionString.toString()
                );
            }
        }).toList().toImmutable();
    }

    @Override
    public ImmutableCollection<DaRule> searchRules(final DaSchema schema, Connection conn) throws SQLException {
        QueryRunner query = new QueryRunner();  // using queryRunner so that we can reuse the connection

        // Do not use ANSI JOIN as it does not work in Sybase 11.x - the SQL below works across all versions
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(query.query(conn,
                "SELECT rul.name as RULE_NAME\n" +
                        "FROM " + schema.getName() + "..sysobjects rul\n" +
                        "WHERE rul.type = 'R'\n" +
                        "and not exists (\n" +
                        "\t-- Ensure that the entry is not attached to a table; otherwise, it is a regular table constraint, and will already be dropped when the table is dropped\n" +
                        "\tselect 1 from " + schema.getName() + "..sysconstraints c\n" +
                        "\twhere c.constrid = rul.id\n" +
                        ")\n",
                new MapListHandler()
        )).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaRule>() {
            @Override
            public DaRule valueOf(Map<String, Object> map) {
                return new DaRuleImpl((String) map.get("RULE_NAME"), schema);
            }
        });
    }

    @Override
    public ImmutableCollection<DaUserType> searchUserTypes(final DaSchema schema, Connection conn) throws SQLException {
        QueryRunner query = new QueryRunner();

        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(query.query(conn,
                "SELECT s1.name as USER_TYPE_NAME\n" +
                        "FROM " + schema.getName() + "..systypes s1\n" +
                        "WHERE s1.usertype>100",
                new MapListHandler()
        )).toImmutable();

        return maps.collect(new Function<Map<String, Object>, DaUserType>() {
            @Override
            public DaUserType valueOf(Map<String, Object> map) {
                return new DaUserTypeImpl((String) map.get("USER_TYPE_NAME"), schema);
            }
        });
    }

    @Override
    public ImmutableCollection<DaRoutine> searchExtraRoutines(final DaSchema schema, String procedureName, Connection conn) throws SQLException {
        String nameClause = procedureName != null ? "and obj.name = '" + procedureName + "'\n" : "";

        String query = String.format("select obj.name name, obj.type type, com.number number, colid2 colid2, colid colid, text text\n" +
                "from %1$s..syscomments com\n" +
                ", %1$s..sysobjects obj\n" +
                "where com.id = obj.id\n" +
                "and com.texttype = 0\n" +
                "and obj.type in ('SF', 'P')\n" +
                nameClause +
                "order by com.id, number, colid2, colid\n", schema.getName());
        QueryRunner qr = new QueryRunner();  // using queryRunner so that we can reuse the connection
        ImmutableList<Map<String, Object>> maps = ListAdapter.adapt(qr.query(conn, query, new MapListHandler())).toImmutable();

        ImmutableList<ExtraRerunnableInfo> routineInfos = maps.collect(new Function<Map<String, Object>, ExtraRerunnableInfo>() {
            @Override
            public ExtraRerunnableInfo valueOf(Map<String, Object> object) {
                String basename = (String) object.get("name");
                int number = ((Integer) object.get("number")).intValue();
                String specificName = number > 1 ? basename + ";" + number : basename;
                return new ExtraRerunnableInfo(
                        basename,
                        specificName,
                        (String) object.get("text"),
                        ((String) object.get("type")).trim(),
                        ((Integer) object.get("colid2")).intValue(),
                        ((Integer) object.get("colid")).intValue()
                );
            }
        });

        return routineInfos.groupBy(ExtraRerunnableInfo.TO_SPECIFIC_NAME).multiValuesView().collect(new Function<RichIterable<ExtraRerunnableInfo>, DaRoutine>() {
            @Override
            public DaRoutine valueOf(RichIterable<ExtraRerunnableInfo> objectInfos) {
                MutableList<ExtraRerunnableInfo> sortedInfos = objectInfos.toSortedList(Comparators.fromFunctions(ExtraRerunnableInfo.TO_ORDER2, ExtraRerunnableInfo.TO_ORDER1));
                StringBuilder definitionString = sortedInfos.injectInto(new StringBuilder(), new Function2<StringBuilder, ExtraRerunnableInfo, StringBuilder>() {
                    @Override
                    public StringBuilder value(StringBuilder sb, ExtraRerunnableInfo rerunnableInfo) {
                        return sb.append(rerunnableInfo.getDefinition());
                    }
                });
                return new DaRoutinePojoImpl(
                        sortedInfos.get(0).getName(),
                        schema,
                        sortedInfos.get(0).getType().equals("P") ? DaRoutineType.procedure : DaRoutineType.function,
                        sortedInfos.get(0).getSpecificName(),
                        definitionString.toString()
                );
            }
        }).toList().toImmutable();
    }

    @Override
    public SchemaStrategy getSchemaStrategy() {
        // Sybase stores the "database"/catalog first, then the schema. schema is usually meaningless for ASE, i.e. dbo value
        return SchemaByCatalogStrategy.INSTANCE;
    }
}
