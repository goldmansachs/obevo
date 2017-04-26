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
package com.gs.obevocomparer.input.db;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.util.TestUtil;
import org.h2.tools.RunScript;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryDataSourceTest {

    private static final String DB_URL = "jdbc:h2:mem:test";

    /* Create a permanent connection to survive the lifecycle of this JUnit test.
     * Without an active connection, H2 deletes an in-memory database automatically
     * so this is required for the test to run properly */
    private static Connection activeConn;

    @BeforeClass
    public static void loadDriverTest() throws Exception {
        Class.forName("org.h2.Driver");
        activeConn = getConnection();

        Connection conn = getConnection();
        RunScript.execute(conn, new FileReader("src/test/resources/testdb.ddl"));
        conn.close();
    }

    @AfterClass
    public static void tearDown() throws SQLException, IOException {
        activeConn.close();
    }

    @Test
    public void queryDataSourceTest() throws Exception {

        QueryDataSource dataSource = new QueryDataSource("test", DB_URL, "test", "",
                "select * from Test");

        Assert.assertFalse(dataSource.getConnection().isClosed());

        List<CatoDataObject> data = TestUtil.getData(dataSource);

        Assert.assertTrue(dataSource.getConnection().isClosed());
        Assert.assertEquals(4, data.size());

        CatoDataObject obj = data.get(0);
        Assert.assertEquals(5, obj.getFields().size());
        Assert.assertEquals(1, obj.getValue("KEY"));
        Assert.assertEquals("abc", obj.getValue("STR_VAL"));
        Assert.assertEquals("def", obj.getValue("STR_VAL2"));
        Assert.assertEquals(5.25, ((BigDecimal) obj.getValue("DOUBLE_VAL")).doubleValue(), 0.00001);
        Assert.assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2010-05-12"), obj.getValue("DATE_VAL"));

        // test query a second time on same table
        Assert.assertEquals(4, TestUtil.getData(
                new QueryDataSource("test", DB_URL, "test", "", "select * from Test")).size());
    }

    @Test
    public void testWithColumnRename() throws Exception {

        QueryDataSource dataSource = new QueryDataSource("test", DB_URL, "test", "",
                "select STR_VAL as RENAME1, DOUBLE_VAL as RENAME2 from Test where KEY = 1");

        List<CatoDataObject> data = TestUtil.getData(dataSource);

        Assert.assertEquals(1, data.size());

        Assert.assertEquals("abc", data.get(0).getValue("RENAME1"));
        Assert.assertEquals(5.25, ((BigDecimal) data.get(0).getValue("RENAME2")).doubleValue(), 0.001);
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, "test", "");
    }
}
