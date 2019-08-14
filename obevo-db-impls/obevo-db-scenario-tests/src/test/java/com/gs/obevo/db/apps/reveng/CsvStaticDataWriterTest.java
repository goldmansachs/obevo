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
package com.gs.obevo.db.apps.reveng;

import java.io.File;
import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.h2.H2JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import junitx.framework.FileAssert;
import org.apache.commons.dbutils.DbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CsvStaticDataWriterTest {
    private final String url = H2JdbcDataSourceFactory.getUrl("csvStaticDataWriterTest", false);
    private final String schema = "STATICTEST";
    private final String table = "TABLE1";
    private DataSource ds;
    private Connection conn;
    private JdbcHelper jdbc;

    @Before
    public void setup() throws Exception {
        this.ds = JdbcDataSourceFactory.createFromJdbcUrl(org.h2.Driver.class, url, new Credential("sa", ""), 1);
        this.conn = ds.getConnection();

        this.jdbc = new JdbcHelper();
        this.jdbc.execute(conn, "DROP SCHEMA IF EXISTS " + schema);
        this.jdbc.execute(conn, "CREATE SCHEMA " + schema);

        this.jdbc.execute(conn, "CREATE TABLE " + schema + "." + table + " (\n" +
                "INT1    INT NULL,\n" +
                "STR1 VARCHAR(30)\tNULL,\n" +
                "DATE1 DATE\tNULL,\n" +
                "TIMESTAMP1 TIMESTAMP\tNULL\n" +
                ")\n");
    }

    @Test
    public void test() {
        this.jdbc.execute(conn, "INSERT INTO " + schema + "." + table + " (INT1, STR1, DATE1, TIMESTAMP1) " +
                "VALUES (1, 'val1', '2017-01-01', '2016-02-02 22:22:22.2')");
        this.jdbc.execute(conn, "INSERT INTO " + schema + "." + table + " (INT1, STR1, DATE1, TIMESTAMP1) " +
                "VALUES (2, null, '2017-02-02', null)");
        this.jdbc.execute(conn, "INSERT INTO " + schema + "." + table + " (INT1, STR1, DATE1, TIMESTAMP1) " +
                "VALUES (null, 'val\\3', null, '2016-03-03 22:22:22.2')");

        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbTypeStr("H2");
        args.setJdbcUrl(url);
        args.setDriverClass(org.h2.Driver.class.getName());
        args.setDbSchema(schema);
        args.setTables(new String[] { table });
        args.setUsername("sa");
        args.setPassword("");
        File outputPath = new File("./target/csvoutput");
        args.setOutputPath(outputPath);
        CsvStaticDataWriter.start(args, new File("./target/csvoutputwork"));

        FileAssert.assertEquals(new File("./src/test/resources/CsvStaticDataWriter/TABLE1.expected.csv"), new File(outputPath, "staticdata/TABLE1.csv"));
    }

    @After
    public void teardown() throws Exception {
        DbUtils.closeQuietly(conn);
    }
}