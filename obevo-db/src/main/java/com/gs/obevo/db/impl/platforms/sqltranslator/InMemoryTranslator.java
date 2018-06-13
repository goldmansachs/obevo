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
package com.gs.obevo.db.impl.platforms.sqltranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.gs.obevo.api.appdata.ChangeInput;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.core.changetypes.StaticDataChangeTypeBehavior;
import com.gs.obevo.db.impl.platforms.sqltranslator.impl.DefaultSqlTranslatorNameMapper;
import com.gs.obevo.db.sqlparser.syntaxparser.AlterTableDrop;
import com.gs.obevo.db.sqlparser.syntaxparser.Constraint;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateIndex;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTable;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTableColumn;
import com.gs.obevo.db.sqlparser.syntaxparser.DropStatement;
import com.gs.obevo.db.sqlparser.syntaxparser.NamedConstraint;
import com.gs.obevo.db.sqlparser.syntaxparser.SqlSyntaxParser;
import com.gs.obevo.db.sqlparser.syntaxparser.UnparseVisitor;
import com.gs.obevo.impl.PrepareDbChange;
import com.gs.obevo.impl.text.CommentRemover;
import com.gs.obevo.impl.util.MultiLineStringSplitter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryTranslator implements PrepareDbChange {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryTranslator.class);

    private final SqlTranslatorNameMapper nameMapper;
    private final ListIterable<PreParsedSqlTranslator> preParsedSqlTranslators;
    private final ListIterable<UnparsedSqlTranslator> unparsedSqlTranslators;
    private final ListIterable<ColumnSqlTranslator> columnSqlTranslators;
    private final ListIterable<PostColumnSqlTranslator> postColumnSqlTranslators;
    private final ListIterable<PostParsedSqlTranslator> postParsedSqlTranslators;

    private InMemoryTranslator() {
        this.nameMapper = new DefaultSqlTranslatorNameMapper();
        this.preParsedSqlTranslators = Lists.mutable.empty();
        this.unparsedSqlTranslators = Lists.mutable.empty();
        this.columnSqlTranslators = Lists.mutable.empty();
        this.postColumnSqlTranslators = Lists.mutable.empty();
        this.postParsedSqlTranslators = Lists.mutable.empty();
    }

    public InMemoryTranslator(SqlTranslatorConfigHelper configHelper) {
        this.nameMapper = configHelper.getNameMapper();
        this.preParsedSqlTranslators = configHelper.getPreParsedSqlTranslators();
        this.unparsedSqlTranslators = configHelper.getUnparsedSqlTranslators();
        this.columnSqlTranslators = configHelper.getColumnSqlTranslators();
        this.postColumnSqlTranslators = configHelper.getPostColumnSqlTranslators();
        this.postParsedSqlTranslators = configHelper.getPostParsedSqlTranslators();
    }

    @Override
    public final String prepare(String sql, final ChangeInput change, final Environment env) {
        if (change != null && Objects.equals(change.getChangeKey().getChangeType().getName(), ChangeType.STATICDATA_STR)
                && !StaticDataChangeTypeBehavior.isInsertModeStaticData(sql)) {
            return sql;
        }

        sql = CommentRemover.removeComments(sql, change != null ? change.getChangeKey().toString() : sql);
        MutableList<String> sqls = MultiLineStringSplitter.createSplitterOnSpaceAndLine("GO").valueOf(sql);

        MutableList<String> convertedSqls = sqls.collect(new Function<String, String>() {
            @Override
            public String valueOf(String object) {
                return InMemoryTranslator.this.translateStatement(object, change);
            }
        });

        return convertedSqls.makeString("\n\nGO\n\n");
    }

    private String translateStatement(String sql, final ChangeInput change) {
        sql = this.preParsedSqlTranslators.injectInto(sql, new Function2<String, PreParsedSqlTranslator, String>() {
            @Override
            public String value(String s, PreParsedSqlTranslator preParsedSqlTranslator) {
                return preParsedSqlTranslator.preprocessSql(s);
            }
        });
        sql = this.renderTree(sql, change);

        return this.postParsedSqlTranslators.injectInto(sql, new Function2<String, PostParsedSqlTranslator, String>() {
            @Override
            public String value(String s, PostParsedSqlTranslator postParsedSqlTranslator) {
                return postParsedSqlTranslator.handleAnySqlPostTranslation(s, change);
            }
        });
    }

    private String renderTree(String sql, final ChangeInput change) {
        UnparseVisitor parsedValue = new SqlSyntaxParser().getParsedValue(sql);

        if (parsedValue == null) {
            LOG.debug("AST parsing did not work for this SQL; falling back to the unparsedSqlTranslators");
            return this.unparsedSqlTranslators.injectInto(sql, new Function2<String, UnparsedSqlTranslator, String>() {
                @Override
                public String value(String s, UnparsedSqlTranslator unparsedSqlTranslator) {
                    return unparsedSqlTranslator.handleRawFullSql(s, change);
                }
            });
        }
        StringBuilder sb = new StringBuilder();

        final CreateTable table = parsedValue.getCreateTable();
        DropStatement dropStatement = parsedValue.getDropStatement();
        AlterTableDrop alterTableDrop = parsedValue.getAlterTableDrop();
        if (table != null) {
            sb.append("CREATE TABLE ").append(table.getName());
            sb.append("(");
            List<String> colStrs = new ArrayList<String>();
            for (final CreateTableColumn interimCol : table.getColumns()) {
                final CreateTableColumn column = this.columnSqlTranslators.injectInto(interimCol, new Function2<CreateTableColumn, ColumnSqlTranslator, CreateTableColumn>() {
                    @Override
                    public CreateTableColumn value(CreateTableColumn col, ColumnSqlTranslator postColumnSqlTranslator) {
                        return postColumnSqlTranslator.handleColumn(col, table);
                    }
                });

                StringBuilder colSb = new StringBuilder();
                colSb.append(column.getName()).append(" ").append(column.getType().getTypeName());
                if (column.getType().getTypeParams() != null) {
                    colSb.append("(").append(column.getType().getTypeParams()).append(")");
                }

                if (column.getPostColumnText() != null) {
                    String postColumnSql = this.postColumnSqlTranslators.injectInto(column.getPostColumnText(), new Function2<String, PostColumnSqlTranslator, String>() {
                        @Override
                        public String value(String s, PostColumnSqlTranslator postColumnSqlTranslator) {
                            return postColumnSqlTranslator.handlePostColumnText(s, column, table);
                        }
                    });
                    colSb.append(" ").append(postColumnSql);
                }

                colStrs.add(colSb.toString());
            }
            sb.append(StringUtils.join(colStrs, ","));
            for (Constraint constraint : table.getConstraints()) {
                sb.append(",");
                this.printConstraint(sb, constraint, change.getObjectName());
            }

            sb.append(")");

            // NOTE - this is where the table.getPostTableCreateText() would normally come in; but we always drop this for in-memory translation
        } else if (parsedValue.getCreateIndex() != null) {
            CreateIndex createIndex = parsedValue.getCreateIndex();
            if (createIndex != null) {
                sb.append("CREATE ");

                // NOTE - this is where the createIndex.getIndexQualifier() would normally come in (Sybase IQ index types like HG, LF); but we always drop this for in-memory translation

                if (createIndex.isUnique()) {
                    sb.append("UNIQUE ");
                }
                String indexName = this.nameMapper.remapIndexName(createIndex.getName(), createIndex.getTableName());
                sb.append("INDEX ").append(indexName).append(" ON ").append(createIndex.getTableName());
                sb.append(" ").append(createIndex.getColumns());

                // NOTE - this is where the createIndex.getPostCreateObjectClauses() would normally come in; but we always drop this for in-memory translation
            }
        } else if (parsedValue.getAlterTableAdd() != null) {
            sb.append("ALTER TABLE ").append(parsedValue.getAlterTableAdd().getTableName()).append(" ADD ");
            this.printConstraint(sb, parsedValue.getAlterTableAdd().getConstraint(), change.getObjectName());
        } else if (alterTableDrop != null) {
            sb.append("ALTER TABLE ").append(alterTableDrop.getTableName()).append(" ");
            this.printDrop(sb, dropStatement, alterTableDrop.getTableName());
        } else if (dropStatement != null) {
            this.printDrop(sb, dropStatement, change.getObjectName());
        } else {
            LOG.warn("This output was not mapped in this code branch; it should have been: {}", parsedValue);
            return this.unparsedSqlTranslators.injectInto(sql, new Function2<String, UnparsedSqlTranslator, String>() {
                @Override
                public String value(String s, UnparsedSqlTranslator unparsedSqlTranslator) {
                    return unparsedSqlTranslator.handleRawFullSql(s, change);
                }
            });
        }

        return sb.toString();
    }

    private void printDrop(StringBuilder sb, DropStatement dropStatement, String parentObjectName) {
        String objectName;
        if (dropStatement.getObjectType().equalsIgnoreCase("index")) {
            objectName = this.nameMapper.remapIndexName(dropStatement.getObjectName(), parentObjectName);
        } else if (dropStatement.getObjectType().equalsIgnoreCase("constraint")) {
            objectName = this.nameMapper.remapConstraintName(dropStatement.getObjectName(), parentObjectName);
        } else {
            objectName = dropStatement.getObjectName();
        }

        sb.append("DROP ").append(dropStatement.getObjectType()).append(" ").append(objectName);
    }

    private void printConstraint(StringBuilder sb, Constraint constraint, String tableName) {
        if (constraint instanceof NamedConstraint) {
            String constraintName = ((NamedConstraint) constraint).getName();
            constraintName = this.nameMapper.remapConstraintName(constraintName, tableName);
            sb.append(" CONSTRAINT ").append(constraintName);
        }

        if (constraint.getRawText() == null) {
            sb.append(" ").append(constraint.getType());
//            sb.append(" ").append(constraint.getClusteredClause());
            sb.append(constraint.getColumns());
//            sb.append(" ").append(constraint.getPostObjectClauses()));
        } else {
            sb.append(" ").append(constraint.getRawText());
        }
    }
}
