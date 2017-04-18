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
package com.gs.obevo.db.sqlparser.syntaxparser;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SqlSyntaxParserTest {

    @Test
    public void simpleUseCase() {
        this.assertTable("create table hehe1 (\n" +
                        "mycol int,\n" +
                        "mycol2 float,\n" +
                        "mycol3 varchar(35),\n" +
                        "mycol4 numeric(19, 2)\n" +
                        ")",
                "hehe1", null, Arrays.asList(
                        new CreateTableColumn("mycol", new DataType("int", null), null)
                        , new CreateTableColumn("mycol2", new DataType("float", null), null)
                        , new CreateTableColumn("mycol3", new DataType("varchar", "35"), null)
                        , new CreateTableColumn("mycol4", new DataType("numeric", "19, 2"), null)
                ), Arrays.<Constraint>asList());
    }

    @Test
    public void testPostColumnAndTableText() {
        this.assertTable("create table schema.table3 (\n" +
                        "mycol int,\n" +
                        "mycol2 float default getdate(),\n" +
                        "mycol3 varchar(35) stuff two aftward(12, 23) default '123 45 6' hello (mystuff(a,b)),\n" +
                        "mycol4 numeric(19, 2) DEFAULT 0.0 NOT NULL,\n" +
                        "emptydefault varchar(35) DEFAULT '',\n" +
                        "datedefault varchar(35) DEFAULT '9999-12-01-00.00.00.000000',\n" +
                        "ID int GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1, CACHE 20) NOT NULL\n" +
                        ") any (meme) garbage ${withToken}",
                "schema.table3", "any (meme) garbage ${withToken}", Arrays.asList(
                        new CreateTableColumn("mycol", new DataType("int", null), null)
                        , new CreateTableColumn("mycol2", new DataType("float", null), "default getdate()")
                        , new CreateTableColumn("mycol3", new DataType("varchar", "35"), "stuff two aftward(12, 23) default '123 45 6' hello (mystuff(a,b))")
                        , new CreateTableColumn("mycol4", new DataType("numeric", "19, 2"), "DEFAULT 0.0 NOT NULL")
                        , new CreateTableColumn("emptydefault", new DataType("varchar", "35"), "DEFAULT ''")
                        , new CreateTableColumn("datedefault", new DataType("varchar", "35"), "DEFAULT '9999-12-01-00.00.00.000000'")
                        , new CreateTableColumn("ID", new DataType("int", null), "GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1, CACHE 20) NOT NULL")
                ), Arrays.<Constraint>asList());
    }

    @Test
    public void testComments() {
        this.assertTable("create table schema.table5 (\n" +
                        "/* mygarbage, */" +
                        "mycol int,\n" +
                        "mycol5 varchar(35) DEFAULT -1 /* \n cross line commenet \n fds */,\n" +
                        "mycol2 float DEFAULT 1, -- more garbage\n" +
                        "mycol4 numeric(19, 2) // more garbage\n" +
                        ") any garbage /* moreComment */ ${withToken}",
                "schema.table5", "any garbage  ${withToken}", Arrays.asList(
                        new CreateTableColumn("mycol", new DataType("int", null), null)
                        , new CreateTableColumn("mycol5", new DataType("varchar", "35"), "DEFAULT -1")
                        , new CreateTableColumn("mycol2", new DataType("float", null), "DEFAULT 1")
                        , new CreateTableColumn("mycol4", new DataType("numeric", "19, 2"), null)
                ), Arrays.<Constraint>asList());
    }

    @Test
    public void testConstraintPk() {
        this.assertTable("create table schema.table6 (\n" +
                        "mycol int,\n" +
                        "mycol5 varchar(35) DEFAULT -1,\n" +
                        "CONSTRAINT SQL090811181956440 PRIMARY KEY (ID)" +
                        ") any garbage ${withToken} \n\nSECOND LINE",
                "schema.table6", "any garbage ${withToken} \n\nSECOND LINE", Arrays.asList(
                        new CreateTableColumn("mycol", new DataType("int", null), null)
                        , new CreateTableColumn("mycol5", new DataType("varchar", "35"), "DEFAULT -1")
                ), Arrays.<Constraint>asList(
                        new NamedConstraint("SQL090811181956440", "PRIMARY KEY", null, "(ID)", null)
                ));
    }

    @Test
    public void testPkOnly() {
        this.assertTable("create table schema.table7 (\n" +
                        "mycol int,\n" +
                        "mycol5 varchar(35) DEFAULT -1,\n" +
                        "PRIMARY KEY (ID)" +
                        ") any garbage ${withToken}",
                "schema.table7", "any garbage ${withToken}", Arrays.asList(
                        new CreateTableColumn("mycol", new DataType("int", null), null)
                        , new CreateTableColumn("mycol5", new DataType("varchar", "35"), "DEFAULT -1")
                ), Arrays.asList(
                        new Constraint("PRIMARY KEY", null, "(ID)", null)
                ));
    }

    @Test
    public void testMultipleConstraints() {
        this.assertTable("CREATE TABLE mytable (\n" +
                        "\tID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1, NO CACHE),\n" +
                        "    TEAM VARCHAR(50) NOT NULL,\n" +
                        "    TEAM2 VARCHAR(50) NOT NULL,\n" +
                        "    PRIMARY KEY(ID),\n" +
                        "    CONSTRAINT myunique UNIQUE CLUSTERED (ID, TEAM2),\n" +
                        "    UNIQUE (TEAM)\n" +
                        ") IN ${CMDRPROD_TABLESPACE}\n",
                "mytable", "IN ${CMDRPROD_TABLESPACE}", Arrays.asList(
                        new CreateTableColumn("ID", new DataType("BIGINT", null), "NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1, NO CACHE)")
                        , new CreateTableColumn("TEAM", new DataType("VARCHAR", "50"), "NOT NULL")
                        , new CreateTableColumn("TEAM2", new DataType("VARCHAR", "50"), "NOT NULL")
                ), Arrays.asList(
                        new Constraint("PRIMARY KEY", null, "(ID)", null)
                        , new NamedConstraint("myunique", "UNIQUE", "CLUSTERED", "(ID, TEAM2)", null)
                        , new Constraint("UNIQUE", null, "(TEAM)", null)
                ));
    }

    @Test
    public void testMultipleConstraintsWithPostClauses() {
        //  NOTE - [WITH clause1 = 1, clause = 2] (i.e. multiple with clauses) inside the column def doesn't work as the
        // AST is too hard for that. For now, we rely on regexp preprocessing beforehand to remove that. post-table
        // is fine
        this.assertTable("CREATE TABLE mytable (\n" +
                        "\tID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1, NO CACHE)," +
                        "\n" +
                        "    TEAM VARCHAR(50) NOT NULL,\n" +
                        "    TEAM2 VARCHAR(50) NOT NULL,\n" +
                        "    PRIMARY KEY(ID),\n" +
                        "    CONSTRAINT myunique UNIQUE CLUSTERED (ID, TEAM2)\n" +
                        "        WITH max_rows_per_page = 1,\n" +
                        "    UNIQUE (TEAM)\n" +
                        "        WITH max_rows_per_page = 2\n" +
                        ") LOCK ALLPAGES\n" +
                        "    WITH max_rows_per_page = 3, reservepagegap = 4\n",
                "mytable", "LOCK ALLPAGES\n    WITH max_rows_per_page = 3, reservepagegap = 4", Arrays.asList(
                        new CreateTableColumn("ID", new DataType("BIGINT", null), "NOT NULL GENERATED ALWAYS AS " +
                                "IDENTITY (START WITH 1, INCREMENT BY 1, NO CACHE)")
                        , new CreateTableColumn("TEAM", new DataType("VARCHAR", "50"), "NOT NULL")
                        , new CreateTableColumn("TEAM2", new DataType("VARCHAR", "50"), "NOT NULL")
                ), Arrays.asList(
                        new Constraint("PRIMARY KEY", null, "(ID)", null)
                        , new NamedConstraint("myunique", "UNIQUE", "CLUSTERED", "(ID, TEAM2)", "WITH " +
                                "max_rows_per_page = 1")
                        , new Constraint("UNIQUE", null, "(TEAM)", "WITH max_rows_per_page = 2")
                ));
    }

    @Test
    public void testAlterTableAdd() {
        this.assertAlterTableAdd("ALTER TABLE LimitProductMap\n" +
                        "    ADD " +
                        "CONSTRAINT U_LPM_clix\n" +
                        "\tUNIQUE CLUSTERED (tobID) PostConstraintText",
                "LimitProductMap",
                new NamedConstraint("U_LPM_clix", "UNIQUE", "CLUSTERED", "(tobID)", "PostConstraintText"));
    }

    @Test
    public void testCreateIndex() throws Exception {
        this.assertCreateIndex("create unique hg index test1 on whatIsThis (a, b, c) stuff afterward",
                true, null, "hg", "test1", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create hg index test2 on whatIsThis (a, b, c) stuff afterward",
                false, null, "hg", "test2", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create unique index test3 on whatIsThis (a, b, c) stuff afterward",
                true, null, null, "test3", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create index test4 on whatIsThis (a, b, c) stuff afterward",
                false, null, null, "test4", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create unique clustered hg index test1 on whatIsThis (a, b, c) stuff afterward",
                true, "clustered", "hg", "test1", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create clustered hg index test2 on whatIsThis (a, b, c) stuff afterward",
                false, "clustered", "hg", "test2", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create unique clustered index test3 on whatIsThis (a, b, c) stuff afterward",
                true, "clustered", null, "test3", "whatIsThis", "(a, b, c)", "stuff afterward");
        this.assertCreateIndex("create clustered index test4 on whatIsThis (a, b, c) stuff afterward",
                false, "clustered", null, "test4", "whatIsThis", "(a, b, c)", "stuff afterward");
    }

    @Test
    public void testDropStatement() throws Exception {
        this.assertDropStatement("drop table abc", "table", "abc", null);
        this.assertDropStatement("drop index schema.abc", "index", "schema.abc", null);
        this.assertDropStatement("drop table schema.abc posttext", "table", "schema.abc", "posttext");
    }

    @Test
    public void testAlterTableDropStatement() throws Exception {
        this.assertAlterTableDropStatement("ALTER TABLE CALCRESULTPRISMDETAIL DROP CONSTRAINT ISNOMINALPRICE POSTTEXT",
                "CALCRESULTPRISMDETAIL", "CONSTRAINT", "ISNOMINALPRICE", "POSTTEXT");
    }

    private void assertAlterTableDropStatement(String sql, String tableName, String type, String object,
            String postObjectClauses) {
        UnparseVisitor parsedValue = new SqlSyntaxParser().getParsedValue(sql);

        assertNotNull("sql could not be parsed, see the debug logs", parsedValue);

        AlterTableDrop alterTableDrop = parsedValue.getAlterTableDrop();
        assertEquals(tableName, alterTableDrop.getTableName());
        DropStatement dropStatement = alterTableDrop.getDropStatement();
        assertEquals(type, dropStatement.getObjectType());
        assertEquals(object, dropStatement.getObjectName());
        assertEquals(postObjectClauses, dropStatement.getPostDropClauses());
    }

    private void assertDropStatement(String sql, String type, String object, String postObjectClauses) {
        UnparseVisitor parsedValue = new SqlSyntaxParser().getParsedValue(sql);

        assertNotNull("sql could not be parsed, see the debug logs", parsedValue);

        DropStatement dropStatement = parsedValue.getDropStatement();
        assertEquals(type, dropStatement.getObjectType());
        assertEquals(object, dropStatement.getObjectName());
        assertEquals(postObjectClauses, dropStatement.getPostDropClauses());
    }

    private void assertCreateIndex(String sql, boolean unique, String clusterClause, String indexQualifier, String name, String tableName,
            String columns, String postCreateObjectClauses) {
        UnparseVisitor parsedValue = new SqlSyntaxParser().getParsedValue(sql);

        assertNotNull("createIndex could not be parsed, see the debug logs", parsedValue);

        CreateIndex createIndex = parsedValue.getCreateIndex();

        assertEquals(unique, createIndex.isUnique());
        assertEquals(clusterClause, createIndex.getClusterClause());
        assertEquals(indexQualifier, createIndex.getIndexQualifier());
        assertEquals(name, createIndex.getName());
        assertEquals(tableName, createIndex.getTableName());
        assertEquals(columns, createIndex.getColumns());
        assertEquals(postCreateObjectClauses, createIndex.getPostCreateObjectClauses());
    }

    private void assertAlterTableAdd(String sql, String tableName, Constraint expectedConstraint) {
        UnparseVisitor parsedValue = new SqlSyntaxParser().getParsedValue(sql);

        assertNotNull("sql could not be parsed, see the debug logs", parsedValue);

        AlterTableAdd alterTableAdd = parsedValue.getAlterTableAdd();

        assertEquals(tableName, alterTableAdd.getTableName());
        Constraint actualConstraint = alterTableAdd.getConstraint();

        this.assertConstraint(expectedConstraint, actualConstraint);
    }

    private void assertTable(String sql, String tableName, String postTableCreateText, List<CreateTableColumn>
            columns, List<Constraint> constraints) {
        UnparseVisitor parsedValue = new SqlSyntaxParser().getParsedValue(sql);

        assertNotNull("table could not be parsed, see the debug logs", parsedValue);

        CreateTable createTable = parsedValue.getCreateTable();

        assertEquals(tableName, createTable.getName());
        assertEquals(postTableCreateText, createTable.getPostTableCreateText());

        assertEquals(columns.size(), createTable.getColumns().size());
        for (int i = 0; i < columns.size(); i++) {
            CreateTableColumn expectedColumn = columns.get(i);
            CreateTableColumn actualColumn = createTable.getColumns().get(i);

            assertEquals(expectedColumn.getName(), actualColumn.getName());
            assertEquals(expectedColumn.getType().getTypeName(), actualColumn.getType().getTypeName());
            assertEquals(expectedColumn.getType().getTypeParams(), actualColumn.getType().getTypeParams());
            assertEquals(expectedColumn.getPostColumnText(), actualColumn.getPostColumnText());
        }

        assertEquals(constraints.size(), createTable.getConstraints().size());
        for (int i = 0; i < constraints.size(); i++) {
            Constraint expectedConstraint = constraints.get(i);
            Constraint actualConstraint = createTable.getConstraints().get(i);

            this.assertConstraint(expectedConstraint, actualConstraint);
        }
    }

    private void assertConstraint(Constraint expectedConstraint, Constraint actualConstraint) {
        if (expectedConstraint instanceof NamedConstraint) {
            String expectedName = ((NamedConstraint) expectedConstraint).getName();
            String actualName = ((NamedConstraint) actualConstraint).getName();
            assertEquals(expectedName, actualName);
        }

        if (expectedConstraint.getRawText() == null) {
            assertEquals(expectedConstraint.getType(), actualConstraint.getType());
            assertEquals(expectedConstraint.getClusteredClause(), actualConstraint.getClusteredClause());
            assertEquals(expectedConstraint.getColumns(), actualConstraint.getColumns());
            assertEquals(expectedConstraint.getPostObjectClauses(), actualConstraint.getPostObjectClauses());
        } else {
            assertEquals(expectedConstraint.getRawText(), actualConstraint.getRawText());
        }
    }
}
