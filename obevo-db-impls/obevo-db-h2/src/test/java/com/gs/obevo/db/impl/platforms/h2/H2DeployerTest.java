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
package com.gs.obevo.db.impl.platforms.h2;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.unittest.UnitTestDbBuilder;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class H2DeployerTest {
    private static final Logger LOG = LoggerFactory.getLogger(H2DeployerTest.class);

    private JdbcHelper jdbc;
    private Connection conn;

    /**
     * This will test out the ability for the tool to return the in-memory url
     * that was created
     */
    private void setupVerification(DbDeployerAppContext context) {
        this.jdbc = new JdbcHelper();
    }

    @After
    public void teardown() {
        DbUtils.closeQuietly(conn);
    }

    @Test
    public void testDeploy() throws Exception {
        // First, test out the new inserts, including strings that look like numbers (the 0006 case)

        DbEnvironment dbEnv = new DbEnvironment();
        dbEnv.setSourceDirs(Lists.mutable.with(FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/platforms/h2/step1")));
        dbEnv.setName("test");
        dbEnv.setPlatform(new H2DbPlatform());
        dbEnv.setSchemas(Sets.immutable.with(new Schema("SCHEMA1"), new Schema("SCHEMA2")));
        dbEnv.setDbServer("BLAH");

        dbEnv.setSchemaNameOverrides(Maps.immutable.with("SCHEMA1", "bogusSchema"));
        dbEnv.setNullToken("(null)");
        dbEnv.setDataDelimiter('^');
        dbEnv.setCleanBuildAllowed(true);

        LOG.info("Step 1 - Setup context");
        DbDeployerAppContext context = dbEnv.getAppContextBuilder()
                .setCredential(new Credential("sa", ""))
                .build();

        context.setupEnvInfra();
        LOG.info("Step 1 - Clean Environment");
        context.cleanEnvironment();
        LOG.info("Step 1 - Deploy");
        context.deploy();
        LOG.info("Step 1- Re-deploy (should be no-op)");
        context.deploy();

        this.setupVerification(context);

        this.conn = context.getDataSource().getConnection();
        // simple test to assert that the table has been created
        List<Map<String, Object>> results = this.jdbc.queryForList(conn, "select * from bogusSchema.TABLE_A order by A_ID");
        assertEquals(4, results.size());
        this.validateResults(results.get(0), 1, 2, "AB,C", "2012-01-01 12:12:12.00000", null);
        this.validateResults(results.get(1), 2, 3, null, "2013-01-01 11:11:11.65432", 9);
        this.validateResults(results.get(2), 3, 4, "ABC", null, 9);
        this.validateResults(results.get(3), 4, 4, "0006", null, 9);
        int result;
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.VIEW1");
        assertEquals(4, result);

        dbEnv.setSourceDirs(Lists.mutable.with(FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/platforms/h2/step2")));

        LOG.info("Step 2 - Setup");
        context = dbEnv.getAppContextBuilder()
                .setCredential(new Credential("sa", ""))
                .build();

        LOG.info("Step 2 - Deploy");
        // now test the update and delete path (row w/ A_ID = 1 is deleted, row 2 has the time field updated,
        // row 3 has the string field updated, row 4 is preserved (we assure that the string preservation of 0009 is
        // fine)
        context.deploy();

        results = this.jdbc.queryForList(conn, "select * from bogusSchema.TABLE_A order by A_ID");
        assertEquals(3, results.size());
        this.validateResults(results.get(0), 2, 3, null, "2013-01-01 12:12:12.65432", 9);
        this.validateResults(results.get(1), 3, 4, "00\n9", null, 9);
        this.validateResults(results.get(2), 4, 4, "0006", null, 9);
    }

    private void validateResults(Map<String, Object> map, Integer aId,
            Integer bId, String stringField, String timestampField, Integer cId) {

        assertEquals(aId, map.get("A_ID"));
        assertEquals(bId, map.get("B_ID"));
        assertEquals(stringField, map.get("STRING_FIELD"));
        Timestamp millis = timestampField == null ? null : Timestamp.valueOf(timestampField);
        assertEquals(millis, map.get("TIMESTAMP_FIELD"));
        assertEquals(cId, map.get("C_ID"));
    }

    @Test
    public void testUnitTestDeploy() throws Exception {
        DbDeployerAppContext builder = new UnitTestDbBuilder()
                .setReferenceEnvName("test")
                .setSourcePath("./src/test/resources/platforms/h2/step1")
                .setDbPlatform(new H2DbPlatform())
                .setDbServer("MYCUSTOMDB")
                .buildContext();
        builder.setupEnvInfra();
        builder.cleanAndDeploy();
        DbDeployerAppContext context = builder.cleanAndDeploy();// run it twice to ensure clean ability

        DbEnvironment env = builder.getEnvironment();

        this.setupVerification(builder);
        int result;
        this.conn = context.getDataSource().getConnection();
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.TABLE_A");
        assertEquals(4, result);
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.VIEW1");
        assertEquals(4, result);
    }

    @Test
    public void testUnitTestDeployLimitTablesAndViews() throws Exception {
        Set<String> tables = new HashSet<String>();
        Set<String> views = new HashSet<String>();

        tables.add("TABLE_A");
        tables.add("TABLE_B");
        views.add("VIEW1");
        views.add("VIEW2");
        views.add("VIEW3");

        DbDeployerAppContext context = new UnitTestDbBuilder()
                .setReferenceEnvName("test")
                .setDbServer("limitTablesViews")
                .setSourcePath("platforms/h2/step1")
                .setDbPlatform(new H2DbPlatform())
                .setTables(tables)
                .setViews(views)
                .buildContext();

        context.setupEnvInfra();
        context.cleanEnvironment();
        context.deploy();

        this.setupVerification(context);
        int result;
        this.conn = context.getDataSource().getConnection();
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.TABLE_A");
        assertEquals(4, result);
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.VIEW1");
        assertEquals(4, result);

        DbMetadataManager dbMetadataManager = new H2DbPlatform().getDbMetadataManager();
        dbMetadataManager.setDataSource(context.getDataSource());

        assertNotNull("Should create tables that were defined in tables/views params", dbMetadataManager.getTableInfo("bogusSchema", "TABLE_B"));
        assertNotNull("Should create tables that were defined in tables/views params", dbMetadataManager.getTableInfo("bogusSchema", "VIEW2"));

        assertNull("Should not create tables that were defined in tables/views params", dbMetadataManager.getTableInfo("bogusSchema", "TABLE_C"));
        assertNull("Should not create tables that were defined in tables/views params", dbMetadataManager.getTableInfo("bogusSchema", "VIEW4"));
    }

    @Test
    public void testCrossDbJoin() throws Exception {
        Set<String> tables = new HashSet<String>();
        Set<String> views = new HashSet<String>();

        tables.add("TABLE_C");
        tables.add("PRODUCT");

        // run it twice to ensure that we can drop the schema
        DbDeployerAppContext dbDeployerAppContext = new UnitTestDbBuilder()
                .setReferenceEnvName("test")
                .setDbServer("crossDbJoin")
                .setSourcePath("platforms/h2/step1")
                .setDbPlatform(new H2DbPlatform())
                .setTables(tables)
                .setViews(views)
                .buildContext();
        dbDeployerAppContext.setupAndCleanAndDeploy().getEnvironment();

        this.setupVerification(dbDeployerAppContext);
        int result;
        this.conn = dbDeployerAppContext.getDataSource().getConnection();
        result = this.jdbc
                .queryForInt(conn, "select count(p.*) from bogusSchema.TABLE_C a, SCHEMA2.PRODUCT p where a.PRODUCT_ID = p.PRODUCT_ID ");
        assertEquals(3, result);
    }

    @Test
    public void testNullAndSpace() throws Exception {
        Set<String> tables = new HashSet<String>();
        Set<String> views = new HashSet<String>();

        tables.add("TABLE_C");

        DbDeployerAppContext dbDeployerAppContext = new UnitTestDbBuilder()
                .setReferenceEnvName("test")
                .setDbServer("nullAndSpace")
                .setSourcePath("./src/test/resources/platforms/h2/step1")
                .setDbPlatform(new H2DbPlatform())
                .setTables(tables)
                .setViews(views)
                .buildContext();
        dbDeployerAppContext.setupAndCleanAndDeploy().getEnvironment();

        this.setupVerification(dbDeployerAppContext);
        String stringField;
        this.conn = dbDeployerAppContext.getDataSource().getConnection();
        List<Map<String, Object>> results = this.jdbc
                .queryForList(conn, "select STRING_FIELD from bogusSchema.TABLE_C c  where c.STRING_FIELD = ''");
        // List<Map<String, Object>> results = jdbc.queryForList("select * from TABLE_C c ");
        for (Map<String, Object> result : results) {
            System.out.println(result.get("STRING_FIELD") + ":" + result.get("C_ID"));
        }
        stringField = (String) results.get(0).get("STRING_FIELD");
        assertEquals(stringField, "");

        results = this.jdbc.queryForList(conn, "select STRING_FIELD from bogusSchema.TABLE_C c  where c.STRING_FIELD is null");
        stringField = (String) results.get(0).get("STRING_FIELD");
        assertNull(stringField);

        // test for ints - ID 6 has a value, ID 7 is null
        results = this.jdbc.queryForList(conn, "select PRODUCT_ID from bogusSchema.TABLE_C c  where c.C_ID in (6)");
        assertEquals(60, results.get(0).get("PRODUCT_ID"));
        results = this.jdbc.queryForList(conn, "select PRODUCT_ID from bogusSchema.TABLE_C c  where c.C_ID in (7)");
        assertNull(results.get(0).get("PRODUCT_ID"));
    }

    @Test
    public void testUnitTestWithFile() throws Exception {
        DataSource ds = JdbcDataSourceFactory.createFromJdbcUrl(org.h2.Driver.class, H2JdbcDataSourceFactory.getUrl("fileTest", true), new Credential("sa", ""));
        this.jdbc = new JdbcHelper();

        // run it twice to ensure that we can drop the schema
        DbDeployerAppContext builder = new UnitTestDbBuilder()
                .setEnvName("unittest-file")
                .setDbServer("fileTest")
                .setSourcePath("platforms/h2/step1")
                .setDbPlatform(new H2DbPlatform())
                .setPersistToFile(true)
                .buildContext();

        builder.setupEnvInfra();
        builder.setupEnvInfra();
        builder.setupEnvInfra();
        builder.cleanAndDeploy();
        DbDeployerAppContext dbDeployerAppContext = builder.cleanAndDeploy();
        int result;
        this.conn = dbDeployerAppContext.getDataSource().getConnection();
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.TABLE_A");
        assertEquals(4, result);
        result = this.jdbc.queryForInt(conn, "select count(*) from bogusSchema.VIEW1");
        assertEquals(4, result);
    }
}
