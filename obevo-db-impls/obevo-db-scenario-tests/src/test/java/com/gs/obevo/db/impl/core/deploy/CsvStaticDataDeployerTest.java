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
package com.gs.obevo.db.impl.core.deploy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.changetypes.CsvStaticDataDeployer;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.h2.H2DbPlatform;
import com.gs.obevo.db.impl.platforms.h2.H2JdbcDataSourceFactory;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.dbmetadata.impl.DbMetadataDialect;
import com.gs.obevo.dbmetadata.impl.DbMetadataManagerImpl;
import com.gs.obevo.dbmetadata.impl.dialects.H2MetadataDialect;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.reader.TextMarkupDocumentReader;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvStaticDataDeployerTest {
    private static final String schema = "StaticTest";
    private static final String table = "Mytest";
    private static final DbPlatform PLATFORM = new H2DbPlatform();
    private static final DbMetadataDialect dbMetadataDialect = new H2MetadataDialect();

    private DataSource ds;
    private Connection conn;
    private JdbcHelper jdbc;
    private DbMetadataManager metadataManager;

    @Before
    public void setup() throws Exception {
        this.ds = JdbcDataSourceFactory.createFromJdbcUrl(org.h2.Driver.class, H2JdbcDataSourceFactory.getUrl("staticdataChangeTest", false), new Credential("sa", ""), 3);
        this.conn = ds.getConnection();

        this.jdbc = new JdbcHelper();
        this.jdbc.execute(conn, "DROP SCHEMA IF EXISTS " + schema);
        this.jdbc.execute(conn, "CREATE SCHEMA " + schema);

        this.metadataManager = new DbMetadataManagerImpl(dbMetadataDialect, this.ds);
    }

    @After
    public void teardown() {
        DbUtils.closeQuietly(conn);
    }

    /**
     * Used to test for overriding primary key requirement with //// METADATA primaryKeys = col1,col2,col3
     */
    @Test
    public void testPrimaryKey() {
        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "STRINGFIELD VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "CID    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                ")\n");

        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);
        env.setNullToken("(null)");
        env.setDataDelimiter('^');

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);
        when(artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN)).thenReturn(
                "UPDATETIMEFIELD");
        when(artifact.getMetadataAttribute("primaryKeys")).thenReturn("AID,BID,STRINGFIELD,TIMESTAMPFIELD,CID,UPDATETIMEFIELD");

        String columnHeaders = "aId^bId^stringField^timestampField^cId";
        when(artifact.getConvertedContent()).thenReturn(
                columnHeaders + "\n" +
                        "1^2^AB,C^2012-01-01 12:12:12^\n" +
                        "2^3^(null)^2013-01-01 11:11:11.65432^9\n" +
                        "3^4^ABC^(null)^9\n" +
                        "3^4^ABC^(null)^9\n" +
                        "4^4^0006^(null)^9\n" +
                        "2^3^(null)^2013-01-01 11:11:11.65432^9\n"
        );
        LocalDateTime preDeployTime = new LocalDateTime();

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);
        List<Map<String, Object>> results = this.jdbc.query(conn, "select * from " + schema + "." + table + " order by AID",
                new MapListHandler());
        assertEquals(6, results.size());
        this.verifyRow(results.get(0), 1, 2, "AB,C", new LocalDateTime("2012-01-01T12:12:12"), null, preDeployTime, true);
        this.verifyRow(results.get(1), 2, 3, null, new LocalDateTime("2013-01-01T11:11:11.65432"), 9, preDeployTime, true);
        this.verifyRow(results.get(2), 2, 3, null, new LocalDateTime("2013-01-01T11:11:11.65432"), 9, preDeployTime, true);
        this.verifyRow(results.get(3), 3, 4, "ABC", null, 9, preDeployTime, true);
        this.verifyRow(results.get(4), 3, 4, "ABC", null, 9, preDeployTime, true);
        this.verifyRow(results.get(5), 4, 4, "0006", null, 9, preDeployTime, true);
    }

    /**
     * See Javadoc for {@link #testRowInsertionOrderIsPreserved(boolean)}.
     */
    @Test
    public void testRowInsertionOrderIsPreservedWithIntPk() {
        testRowInsertionOrderIsPreserved(true);
    }

    /**
     * See Javadoc for {@link #testRowInsertionOrderIsPreserved(boolean)}.
     */
    @Test
    public void testRowInsertionOrderIsPreservedWithStringPk() {
        testRowInsertionOrderIsPreserved(false);
    }

    /**
     * Solving for use case where a table has a self-referencing primary key. In case the rows of the file are written
     * in a different order from the natural sort order, then the insertions may fail if the difference calculation
     * doesn't maintain the original row order. This test is here to verify that the order is preserved so that the
     * inserts succeed (assuming that rows with foreign key references are added _after_ the rows where those primary
     * keys are created).
     */
    private void testRowInsertionOrderIsPreserved(boolean intDataType) {
        String keyType = intDataType ? "INT" : "VARCHAR(32)";

        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "MYID    " + keyType + " NOT NULL,\n" +
                "PARENTID    " + keyType + " NULL,\n" +
                "PRIMARY KEY (MYID)\n" +
                ")\n");
        this.jdbc.execute(conn, "ALTER TABLE " + schema + "." + table + " ADD FOREIGN KEY (PARENTID) REFERENCES " + schema + "." + table + "(MYID)");

        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);

        String columnHeaders = "MYID,PARENTID";
        when(artifact.getConvertedContent()).thenReturn(
                columnHeaders + "\n" +
                        "2,null\n" +
                        "1,2\n" +
                        "3,2\n" +
                        "4,2\n" +
                        "5,1\n" +
                        "6,1\n" +
                        ""
        );

        LocalDateTime preDeployTime = new LocalDateTime();

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);
        List<Map<String, Object>> results = this.jdbc.query(conn, "select * from " + schema + "." + table + " order by MYID",
                new MapListHandler());
        assertEquals(6, results.size());

        this.verifyFkRow(results.get(0), intDataType, 1, 2);
        this.verifyFkRow(results.get(1), intDataType, 2, null);
        this.verifyFkRow(results.get(2), intDataType, 3, 2);
        this.verifyFkRow(results.get(3), intDataType, 4, 2);
        this.verifyFkRow(results.get(4), intDataType, 5, 1);
        this.verifyFkRow(results.get(5), intDataType, 6, 1);
    }

    @Test
    public void testNormalInsertAndDeleteUseCase() {
        this.testNormalInsertAndDeleteUseCase(false);
    }

    /**
     * This use case is here to simulate if we wrote the CSV files for a case-sensitive DB like Sybase ASE, but then
     * we do the translation to run in H2, which is case-INsensitive. So we need to ensure that this still works
     */
    @Test
    public void testCaseSensitivity() {
        this.testNormalInsertAndDeleteUseCase(true);
    }

    private void testNormalInsertAndDeleteUseCase(boolean caseSensitiveCsv) {
        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "STRINGFIELD VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "CID    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");

        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);
        env.setNullToken("(null)");
        env.setDataDelimiter('^');

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);
        when(artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN)).thenReturn(
                "UPDATETIMEFIELD");

        String columnHeaders = "aId^bId^stringField^timestampField^cId";
        if (!caseSensitiveCsv) {
            columnHeaders = columnHeaders.toUpperCase();
        }

        System.out.println("First, try an insert");
        when(artifact.getConvertedContent()).thenReturn(
                columnHeaders + "\n" +
                        "1^2^AB,C^2012-01-01 12:12:12^\n" +
                        "2^3^(null)^2013-01-01 11:11:11.65432^9\n" +
                        "3^4^ABC^(null)^9\n" +
                        "4^4^0006^(null)^9\n"
        );

        LocalDateTime preDeployTime = new LocalDateTime();

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);

        List<Map<String, Object>> results = this.jdbc.query(conn, "select * from " + schema + "." + table + " order by AID",
                new MapListHandler());

        assertEquals(4, results.size());
        this.verifyRow(results.get(0), 1, 2, "AB,C", new LocalDateTime("2012-01-01T12:12:12"), null, preDeployTime, true);
        this.verifyRow(results.get(1), 2, 3, null, new LocalDateTime("2013-01-01T11:11:11.65432"), 9, preDeployTime, true);
        this.verifyRow(results.get(2), 3, 4, "ABC", null, 9, preDeployTime, true);
        this.verifyRow(results.get(3), 4, 4, "0006", null, 9, preDeployTime, true);

        System.out.println("Now, trying an update: row 2 is updated, row 3 is deleted, row 5 is added");
        when(artifact.getConvertedContent()).thenReturn(
                columnHeaders + "\n" +
                        "1^2^AB,C^2012-01-01 12:12:12^\n" +
                        "2^3^(null)^2013-02-02 22:22:22.56789^9\n" +
                        "4^4^0006^(null)^9\n" +
                        "5^5^ABCD^(null)^9\n"
        );

        preDeployTime = new LocalDateTime();
        csvStaticDataDeployer.deployArtifact(artifact);

        results = this.jdbc.query(conn, "select * from " + schema + "." + table + " order by AID", new MapListHandler());

        assertEquals(4, results.size());
        this.verifyRow(results.get(0), 1, 2, "AB,C", new LocalDateTime("2012-01-01T12:12:12"), null, preDeployTime, false);
        this.verifyRow(results.get(1), 2, 3, null, new LocalDateTime("2013-02-02T22:22:22.56789"), 9, preDeployTime, true);
        this.verifyRow(results.get(2), 4, 4, "0006", null, 9, preDeployTime, false);
        this.verifyRow(results.get(3), 5, 5, "ABCD", null, 9, preDeployTime, true);
    }

    private void verifyFkRow(Map<String, Object> stringObjectMap, boolean intDataType, int myId, Integer parentId) {
        assertEquals(intDataType ? myId : String.valueOf(myId), stringObjectMap.get("MYID"));
        assertEquals(intDataType || parentId == null ? parentId : String.valueOf(parentId), stringObjectMap.get("PARENTID"));
    }

    private void verifyRow(Map<String, Object> stringObjectMap, Integer aId, Integer bId, String stringField,
            LocalDateTime timestampField, Integer cId, LocalDateTime startTime, boolean greater) {
        assertEquals(aId, stringObjectMap.get("AID"));
        assertEquals(bId, stringObjectMap.get("BID"));
        assertEquals(stringField, stringObjectMap.get("STRINGFIELD"));
        LocalDateTime timestampFieldDbValue = stringObjectMap.get("TIMESTAMPFIELD") == null ? null : new LocalDateTime(stringObjectMap.get("TIMESTAMPFIELD"));
        assertEquals(timestampField, timestampFieldDbValue);
        assertEquals(cId, stringObjectMap.get("CID"));
        LocalDateTime updateTimeFieldDbValue = stringObjectMap.get("UPDATETIMEFIELD") == null ? null : new LocalDateTime(stringObjectMap.get("UPDATETIMEFIELD"));
        if (greater) {
            assertThat(updateTimeFieldDbValue, greaterThan(startTime));
        } else {
            assertThat(updateTimeFieldDbValue, lessThan(startTime));
        }
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testMissingPrimaryKeys() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Require a primary key or unique index on table " + table.toUpperCase()
                + " to support CSV-based static data support");
        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                ")\n");
        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);
        env.setNullToken("(null)");
        env.setDataDelimiter('^');

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);
        when(artifact.getConvertedContent()).thenReturn(
                "AID^BID\n" +
                        "1^2\n" +
                        "2^3\n" +
                        "3^4\n" +
                        "4^4\n"
        );

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);
    }

    /**
     * test the case where table created with a primary key but user specifies override tag as well
     */
    @Test
    public void testOverrideAndPrimaryKey() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Cannot specify primary key and override tag on table " + table.toUpperCase()
                + " to support CSV-based static data support");
        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");
        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);
        env.setNullToken("(null)");
        env.setDataDelimiter('^');

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);
        when(artifact.getConvertedContent()).thenReturn(
                "AID^BID\n" +
                        "1^2\n" +
                        "2^3\n" +
                        "3^4\n" +
                        "4^4\n"
        );
        when(artifact.getMetadataAttribute("primaryKeys")).thenReturn("AID,BID");

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);
    }

    @Test
    public void testInvalidUpdateTimeColumnSpecifiedDuplicatedInCsv() {
        expectedEx.expect(IllegalArgumentException.class);
        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "STRINGFIELD VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "CID    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");

        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);
        env.setNullToken("(null)");
        env.setDataDelimiter('^');

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);
        when(artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN)).thenReturn(
                "UPDATETIMEFIELD");

        when(artifact.getConvertedContent()).thenReturn(
                "AID^BID^STRINGFIELD^TIMESTAMPFIELD^CID^UPDATETIMEFIELD\n" +
                        "1^2^AB,C^2012-01-01 12:12:12^^(null)\n" +
                        "2^3^(null)^2013-01-01 11:11:11.65432^9^(null)\n" +
                        "3^4^ABC^(null)^9^(null)\n" +
                        "4^4^0006^(null)^9^(null)\n"
        );

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);
    }

    @Test
    public void testInvalidUpdateTimeColumnSpecified() {
        expectedEx.expect(IllegalArgumentException.class);
        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "AID    INT NOT NULL,\n" +
                "BID    INT NOT NULL,\n" +
                "STRINGFIELD VARCHAR(30)\tNULL,\n" +
                "TIMESTAMPFIELD TIMESTAMP\tNULL,\n" +
                "CID    INT NULL,\n" +
                "UPDATETIMEFIELD TIMESTAMP NOT NULL, \n" +
                "PRIMARY KEY (AID)\n" +
                ")\n");

        DbEnvironment env = new DbEnvironment();
        env.setPlatform(PLATFORM);
        env.setNullToken("(null)");
        env.setDataDelimiter('^');

        Change artifact = mock(Change.class);
        when(artifact.getPhysicalSchema(env)).thenReturn(new PhysicalSchema(schema));
        when(artifact.getObjectName()).thenReturn(table);
        when(artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN)).thenReturn(
                "InvalidTimeFieldColumn");

        when(artifact.getConvertedContent()).thenReturn(
                "AID^BID^STRINGFIELD^TIMESTAMPFIELD^CID^UPDATETIMEFIELD\n" +
                        "1^2^AB,C^2012-01-01 12:12:12^^(null)\n" +
                        "2^3^(null)^2013-01-01 11:11:11.65432^9^(null)\n" +
                        "3^4^ABC^(null)^9^(null)\n" +
                        "4^4^0006^(null)^9^(null)\n"
        );

        CsvStaticDataDeployer csvStaticDataDeployer = new CsvStaticDataDeployer(env, getSqlExecutor(), this.ds, metadataManager, new H2DbPlatform());
        csvStaticDataDeployer.deployArtifact(artifact);
    }

    private SqlExecutor getSqlExecutor() {
        return new SqlExecutor() {
            @Override
            public JdbcHelper getJdbcTemplate() {
                return new JdbcHelper();
            }

            @Override
            public void executeWithinContext(PhysicalSchema schema, Procedure<Connection> runnable) {
                Connection connection = null;
                try {
                    connection = ds.getConnection();
                    runnable.value(conn);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(connection);
                }
            }

            @Override
            public <T> T executeWithinContext(PhysicalSchema schema, ThrowingFunction<Connection, T> callable) {
                Connection connection = null;
                try {
                    connection = ds.getConnection();
                    return callable.safeValueOf(connection);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    DbUtils.closeQuietly(connection);
                }
            }

            @Override
            public void performExtraCleanOperation(ExecuteChangeCommand command, DbMetadataManager metaDataMgr) {

            }
        };
    }
}
