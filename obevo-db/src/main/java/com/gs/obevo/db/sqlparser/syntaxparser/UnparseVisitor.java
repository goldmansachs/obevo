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
package com.gs.obevo.db.sqlparser.syntaxparser;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnparseVisitor implements SqlParserVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(UnparseVisitor.class);

    private final PrintStream out;
    private CreateIndex createIndex;
    private AlterTableDrop alterTableDrop;

    public UnparseVisitor(PrintStream o) {
        this.out = o;
    }

    private static final boolean debug = false;

    private Object print(SimpleNode node, Object data) {
        if (debug) {
            this.out.println(">>>> Calling print from " + node.getClass() + " ***");
        }
        Token t1 = node.getFirstToken();
        Token t = new Token();
        t.next = t1;

        SimpleNode n;
        for (int ord = 0; ord < node.jjtGetNumChildren(); ord++) {
            n = (SimpleNode) node.jjtGetChild(ord);
            while (true) {
                t = t.next;
                if (t == n.getFirstToken()) {
                    break;
                }
                this.print(t);
            }
            n.jjtAccept(this, data);
            t = n.getLastToken();
        }

        while (t != node.getLastToken()) {
            t = t.next;
            this.print(t);
        }
        if (debug) {
            this.out.println("<< Exiting print for " + node.getClass() + " ***");
        }
        return data;
    }

    private void print(Token t) {
        Token tt = t.specialToken;
        if (tt != null) {
            while (tt.specialToken != null) {
                tt = tt.specialToken;
            }
            while (tt != null) {
                // To preserve the whitespace
                switch (tt.kind) {
                case SqlParserConstants.FORMAL_COMMENT:
                case SqlParserConstants.MULTI_LINE_COMMENT:
                case SqlParserConstants.SINGLE_LINE_COMMENT:
                    break;
                default:
                    this.currentSb().append(this.addUnicodeEscapes(tt.image));
                }
                tt = tt.next;
            }
        }

        // To remove comments from the output
        switch (t.kind) {
        case SqlParserConstants.FORMAL_COMMENT:
        case SqlParserConstants.MULTI_LINE_COMMENT:
        case SqlParserConstants.SINGLE_LINE_COMMENT:
        case SqlParserConstants.IN_FORMAL_COMMENT:
        case SqlParserConstants.IN_MULTI_LINE_COMMENT:
        case SqlParserConstants.IN_SINGLE_LINE_COMMENT:
            break;
        default:
            this.currentSb().append(this.addUnicodeEscapes(t.image));
        }

        if (debug) {
            this.out.print("\n");  // shant added
        }
    }

    private StringBuilder currentSb() {
        if (this.sbs.isEmpty()) {
            return new StringBuilder(); // essentially writing to null
        } else {
            return this.sbs.peek();
        }
    }

    private String addUnicodeEscapes(String str) {
        String retval = "";
        char ch;
        for (int i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            if ((ch < 0x20 || ch > 0x7e) &&
                    ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f') {
                String s = "0000" + Integer.toString(ch, 16);
                retval += "\\u" + s.substring(s.length() - 4, s.length());
            } else {
                retval += ch;
            }
        }
        return retval;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTCompilationUnit node, Object data) {
        return this.print(node, data);
    }

    private CreateTable createTable;

    public CreateTable getCreateTable() {
        return this.createTable;
    }

    private CreateTableColumn createTableColumn;

    @Override
    public Object visit(ASTCreateTable node, Object data) {
        this.createTable = new CreateTable();
        Object obj = this.print(node, data);
        this.createTable.setName(this.tableName);
        this.createTable.setPostTableCreateText(this.postCreateObjectClauses);
        this.createTable.getConstraints().addAll(this.constraints);
        return obj;
    }

    private String tableName = null;

    @Override
    public Object visit(ASTTableName node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.tableName = sb.toString().trim();
        return obj;
    }

    private String dropObjectName = null;

    @Override
    public Object visit(ASTDropObjectName node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.dropObjectName = sb.toString().trim();
        return obj;
    }

    @Override
    public Object visit(ASTCreateTableColumnList node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTCreateTableColumn node, Object data) {
        this.createTableColumn = new CreateTableColumn();
        Object obj = this.print(node, data);
        this.createTableColumn.setName(this.columnName);
        this.createTableColumn.setPostColumnText(this.postColumnClauses);

        this.createTable.getColumns().add(this.createTableColumn);
        this.createTableColumn = null;
        this.postColumnClauses = null;
        this.columnName = null;
        return obj;
    }

    @Override
    public Object visit(ASTDataType node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        LOG.trace("We only pop the string off the stack; will not use the ASTDataType string: {}", sb);
        this.createTableColumn.getType().setTypeName(this.dataTypeName);
        return obj;
    }

    @Override
    public Object visit(ASTDataTypeLenList node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.createTableColumn.getType().setTypeParams(sb.toString().trim());
        return obj;
    }

    private final Stack<StringBuilder> sbs = new Stack<StringBuilder>();

    private String columnName;

    @Override
    public Object visit(ASTColumnName node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.columnName = sb.toString().trim();
        return obj;
    }

    private String dataTypeName;

    @Override
    public Object visit(ASTDataTypeName node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.dataTypeName = sb.toString().trim();
        return obj;
    }

    @Override
    public Object visit(ASTCreateStatement node, Object data) {
        return this.print(node, data);
    }

    public CreateIndex getCreateIndex() {
        return this.createIndex;
    }

    @Override
    public Object visit(ASTCreateIndex node, Object data) {
        this.createIndex = new CreateIndex();
        Object obj = this.print(node, data);
        this.createIndex.setName(this.indexName);
        this.createIndex.setUnique(this.uniqueIndex);
        this.createIndex.setClusterClause(this.clusterClause);
        this.createIndex.setIndexQualifier(this.indexQualifier);
        this.createIndex.setTableName(this.tableName);
        this.createIndex.setColumns(this.indexColumnList);
        this.createIndex.setPostCreateObjectClauses(this.postCreateObjectClauses);
        return obj;
    }

    private boolean uniqueIndex;

    @Override
    public Object visit(ASTUnique node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        LOG.trace("We only pop the string off the stack; will not use the ASTUnique string: {}", sb);
        this.uniqueIndex = true;
        return obj;
    }

    private String indexName;

    @Override
    public Object visit(ASTIndexName node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.indexName = sb.toString().trim();
        return obj;
    }

    private String indexQualifier;

    @Override
    public Object visit(ASTIndexQualifier node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.indexQualifier = sb.toString().trim();
        return obj;
    }

    private String clusterClause;

    @Override
    public Object visit(ASTClusterClause node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.clusterClause = sb.toString().trim();
        return obj;
    }

    @Override
    public Object visit(ASTCreateTableEnd node, Object data) {
        return this.print(node, data);
    }

    private String postCreateObjectClauses;

    @Override
    public Object visit(ASTPostObjectTableClauses node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.postCreateObjectClauses = sb.toString().trim();
        return obj;
    }

    private String indexColumnList;

    @Override
    public Object visit(ASTIndexColumnList node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.indexColumnList = sb.toString().trim();
        return obj;
    }

    private String postConstraintClauses;

    @Override
    public Object visit(ASTPostConstraintClauses node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.postConstraintClauses = sb.toString().trim();
        return obj;
    }

    private String postColumnClauses;

    @Override
    public Object visit(ASTPostColumnClauses node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.postColumnClauses = sb.toString().trim();
        return obj;
    }

    @Override
    public Object visit(ASTIdentifierName node, Object data) {
        return this.print(node, data);
    }

    private String constraintName;

    @Override
    public Object visit(ASTConstraintName node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.constraintName = sb.toString().trim();
        return obj;
    }

    private String objectType;

    @Override
    public Object visit(ASTDropObjectType node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();
        this.objectType = sb.toString().trim();
        return obj;
    }

    private DropStatement dropStatement;

    @Override
    public Object visit(ASTDropStatement node, Object data) {
        Object obj = this.print(node, data);
        this.dropStatement = new DropStatement();
        this.dropStatement.setObjectType(this.objectType);
        this.dropStatement.setObjectName(this.dropObjectName);
        this.dropStatement.setPostDropClauses(this.postCreateObjectClauses);
        return obj;
    }

    public DropStatement getDropStatement() {
        return this.dropStatement;
    }

    private final List<Constraint> constraints = new ArrayList<Constraint>();

    @Override
    public Object visit(ASTConstraintClause node, Object data) {
        Object obj = this.print(node, data);
        NamedConstraint constraint = new NamedConstraint();
        constraint.setName(this.constraintName);

        Constraint currentConstraint = this.constraints.get(this.constraints.size() - 1);
        if (currentConstraint.getRawText() == null) {
            constraint.setPostObjectClauses(currentConstraint.getPostObjectClauses());
            constraint.setColumns(currentConstraint.getColumns());
            constraint.setClusteredClause(currentConstraint.getClusteredClause());
            constraint.setType(currentConstraint.getType());
        } else {
            constraint.setRawText(currentConstraint.getRawText());
        }

        this.constraints.set(this.constraints.size() - 1, constraint);
        return obj;
    }

    @Override
    public Object visit(ASTPrimaryKeyClause node, Object data) {
        this.clusterClause = null;
        this.indexColumnList = null;
        this.postCreateObjectClauses = null;
        Object obj = this.print(node, data);
        Constraint currentConstraint = new Constraint();
        currentConstraint.setType("PRIMARY KEY");
        currentConstraint.setClusteredClause(this.clusterClause);
        currentConstraint.setColumns(this.indexColumnList);
        currentConstraint.setPostObjectClauses(this.postConstraintClauses);

        this.constraints.add(currentConstraint);
        return obj;
    }

    @Override
    public Object visit(ASTUniqueClause node, Object data) {
        this.clusterClause = null;
        this.indexColumnList = null;
        this.postCreateObjectClauses = null;
        Object obj = this.print(node, data);
        Constraint currentConstraint = new Constraint();
        currentConstraint.setType("UNIQUE");
        currentConstraint.setClusteredClause(this.clusterClause);
        currentConstraint.setColumns(this.indexColumnList);
        currentConstraint.setPostObjectClauses(this.postConstraintClauses);

        this.constraints.add(currentConstraint);
        return obj;
    }

    @Override
    public Object visit(ASTOtherConstraintClause node, Object data) {
        this.sbs.push(new StringBuilder());
        Object obj = this.print(node, data);
        StringBuilder sb = this.sbs.pop();

        Constraint currentConstraint = new Constraint();
        currentConstraint.setRawText(sb.toString().trim());

        this.constraints.add(currentConstraint);
        return obj;
    }

    @Override
    public Object visit(ASTExpression node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTSimpleExpression node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTExpressionList node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNestedExpression node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNestedExpressionList node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNoCommaSimpleExpression node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNoCommaExpressionList node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNoCommaExpression node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNoCommaNestedExpression node, Object data) {
        return this.print(node, data);
    }

    @Override
    public Object visit(ASTNoCommaNestedExpressionList node, Object data) {
        return this.print(node, data);
    }

    private AlterTableAdd alterTableAdd;

    public AlterTableAdd getAlterTableAdd() {
        return this.alterTableAdd;
    }

    @Override
    public Object visit(ASTAlterStatement node, Object data) {
        Object obj = this.print(node, data);
        if (this.alterTableAdd == null) {
            this.alterTableDrop = new AlterTableDrop();
            this.alterTableDrop.setTableName(this.tableName);
            this.alterTableDrop.setDropStatement(this.dropStatement);
        }
        // the rest is processed in ASTAlterTableAdd - may refactor at some point
        return obj;
    }

    public AlterTableDrop getAlterTableDrop() {
        return this.alterTableDrop;
    }

    @Override
    public Object visit(ASTAlterTableAdd node, Object data) {
        this.alterTableAdd = new AlterTableAdd();
        Object obj = this.print(node, data);
        this.alterTableAdd.setTableName(this.tableName);
        this.alterTableAdd.setConstraint((NamedConstraint) this.constraints.get(0));
        return obj;
    }
}

