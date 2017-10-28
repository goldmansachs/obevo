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
package com.gs.obevo.dbmetadata.api;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.impl.DbMetadataDialect;
import com.gs.obevo.dbmetadata.impl.DbMetadataManagerImpl;
import com.gs.obevo.dbmetadata.impl.dialects.H2MetadataDialect;
import com.gs.obevo.dbmetadata.deepcompare.CompareBreak;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.eclipse.collections.api.collection.MutableCollection;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DbMetadataComparisonUtilTest {
    private static final PhysicalSchema schema = new PhysicalSchema("schemaCompareTest");
    private static final String schemaStr = schema.getPhysicalName();
    private static final String table = "MYTEST";
    private static final DbMetadataDialect dbMetadataDialect = new H2MetadataDialect();

    private BasicDataSource ds;
    private QueryRunner jdbc;
    private DbMetadataComparisonUtil dbMetadataComparisonUtil;
    private DbMetadataManager metadataManager;

    @Before
    public void setup() throws Exception {
        this.ds = new BasicDataSource();
        this.ds.setDriverClassName(org.h2.Driver.class.getName());
        this.ds.setUrl(String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", schemaStr));
        this.ds.setUsername("sa");
        this.ds.setPassword("");

        this.jdbc = new QueryRunner(this.ds);
        this.jdbc.update("DROP SCHEMA IF EXISTS " + schemaStr);
        this.jdbc.update("CREATE SCHEMA " + schemaStr);

        this.metadataManager = new DbMetadataManagerImpl(dbMetadataDialect, this.ds);
        this.dbMetadataComparisonUtil = new DbMetadataComparisonUtil();
    }

    @Test
    public void testTableStaySame() throws Exception {
        this.jdbc.update("DROP TABLE IF EXISTS " + schemaStr + "." + table);
        this.jdbc.update("CREATE TABLE " + schemaStr + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "STRINGFIELD VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "CID    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");

        DaTable tableLeft = this.metadataManager.getTableInfo(schema, DbMetadataComparisonUtilTest.table,
                new DaSchemaInfoLevel().setMaximum());

        this.jdbc.update("DROP TABLE IF EXISTS " + schemaStr + "." + table);
        this.jdbc.update("CREATE TABLE " + schemaStr + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "STRINGFIELD VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "CID    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");

        DaTable tableRight = this.metadataManager.getTableInfo(schema, DbMetadataComparisonUtilTest.table,
                new DaSchemaInfoLevel().setMaximum());
        assertEquals(0, this.dbMetadataComparisonUtil.compareTables(tableLeft, tableRight).size());
    }

    @Test
    public void testTableChanges() throws Exception {
        this.jdbc.update("DROP TABLE IF EXISTS " + schemaStr + "." + table);
        this.jdbc.update("CREATE TABLE " + schemaStr + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "B_TYPE_CHANGE    INT NOT NULL,\n" +
                "STRING_LENCHANGE VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "C_NULL_CHANGE    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");

        DaTable tableLeft = this.metadataManager.getTableInfo(schema, DbMetadataComparisonUtilTest.table,
                new DaSchemaInfoLevel().setMaximum());

        this.jdbc.update("DROP TABLE IF EXISTS " + schemaStr + "." + table);
        this.jdbc.update("CREATE TABLE " + schemaStr + "." + table + " (\n" +
                "AID_NAMECHANGE    INT NOT NULL,\n" +
                "B_TYPE_CHANGE    BIGINT NOT NULL,\n" +
                "STRING_LENCHANGE VARCHAR(60)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "C_NULL_CHANGE    INT NOT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID_NAMECHANGE)\n" +
                ")\n");

        DaTable tableRight = this.metadataManager.getTableInfo(schema, DbMetadataComparisonUtilTest.table,
                new DaSchemaInfoLevel().setMaximum());
        MutableCollection<CompareBreak> breaks = this.dbMetadataComparisonUtil.compareTables(tableLeft, tableRight);
        System.out.println("BREAKS\n" + breaks.makeString("\n"));
        assertEquals(11, breaks.size());  // TODO get a few duplicates; why?
    }
}
