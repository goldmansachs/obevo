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
package com.gs.obevo.db.impl.platforms.sybaseiq.iqload;

import java.io.File;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.sql.DataSource;

import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.sybaseiq.SybaseIqParamReader;
import org.apache.commons.dbutils.DbUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@Ignore("Cannot yet run this test on remote build server due to no guarantee of ODBC client")
@RunWith(Parameterized.class)
public class IqLoadFileCreatorTest {

    @Parameterized.Parameters
    public static Iterable<Object[]> params() {
        return SybaseIqParamReader.getParamReader().getJdbcDsAndSchemaParams();
    }

    private static final String tableName = "IQ_LOAD_TEST";
    private final DataSource dataSource;
    private final JdbcHelper jdbcTemplate;
    private final String schema;
    private final IqLoadMode iqLoadMode;
    private final File iqLoadRoot;
    private final boolean grantEnabled;
    private Connection conn;

    public IqLoadFileCreatorTest(DataSource dataSource, String schemaName) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcHelper();
        this.schema = schemaName;
        this.iqLoadRoot = new File("./target");
        this.iqLoadMode = IqLoadMode.IQ_CLIENT_WINDOWS;
        this.grantEnabled = false;
    }

    @After
    public void teardown() {
        DbUtils.closeQuietly(conn);
    }

    @Test
    public void testLoadWithoutDefault() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP TABLE IF EXISTS " + tableName + ";");
        sb.append("CREATE TABLE " + tableName + " (");
        sb.append("intfield integer null,");
        sb.append("stringfield varchar(200) null,");
        sb.append("cm_id bigint not null,");
        sb.append("datefieldjoda date not null,");
        sb.append("datefieldjdk date not null,");
        sb.append("timestampfieldjoda timestamp not null,");
        sb.append("timestampfieldjdk timestamp not null,");
        sb.append("PRIMARY KEY (CM_ID),");
        sb.append(");");
        if (grantEnabled) {
            sb.append("GRANT SELECT ON " + tableName + " TO dbdeploy_ro;");
        }

        MutableList<FieldToColumnMapping> mappings = Lists.mutable.empty();
        mappings.add(new FieldToColumnMapping("myCmId", "CM_ID"));
        mappings.add(new FieldToColumnMapping("myString", "stringfield"));
        mappings.add(new FieldToColumnMapping("myInt", "intfield"));
        mappings.add(new FieldToColumnMapping("dateFieldJoda", "datefieldjoda"));
        mappings.add(new FieldToColumnMapping("timestampFieldJoda", "timestampfieldjoda"));
        mappings.add(new FieldToColumnMapping("dateFieldJdk", "datefieldjdk"));
        mappings.add(new FieldToColumnMapping("timestampFieldJdk", "timestampfieldjdk"));

        this.testLoad(sb.toString(), mappings, "\n", "~"); // single-char
        this.testLoad(sb.toString(), mappings, "\n", "!~!~"); // multi-char
        this.testLoad(sb.toString(), mappings, "####", "~"); // single-char,
        // non-line-break del
        this.testLoad(sb.toString(), mappings, "####", "!~!~"); // multi-char,
        // non-line-break del
    }

    @Test
    public void testLoadWithDefault() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("DROP TABLE IF EXISTS " + tableName + ";");
        sb.append("CREATE TABLE " + tableName + " (");
        sb.append("intfield integer null,");
        sb.append("default_value_col timestamp not null,");
        sb.append("stringfield varchar(200) null,");
        sb.append("cm_id bigint not null,");
        sb.append("datefieldjoda date not null,");
        sb.append("datefieldjdk date not null,");
        sb.append("timestampfieldjoda timestamp not null,");
        sb.append("timestampfieldjdk timestamp not null,");
        sb.append("PRIMARY KEY (CM_ID),");
        sb.append(");");
        if (grantEnabled) {
            sb.append("GRANT SELECT ON " + tableName + " TO dbdeploy_ro;");
        }

        MutableList<FieldToColumnMapping> mappings = Lists.mutable.empty();
        mappings.add(new FieldToColumnMapping("myCmId", "CM_ID"));
        mappings.add(new FieldToColumnMapping("myString", "stringfield"));
        mappings.add(new FieldToColumnMapping("myInt", "intfield"));
        mappings.add(new FieldToColumnMapping("dateFieldJoda", "datefieldjoda"));
        mappings.add(FieldToColumnMapping.createWithDefaultValue("default_value_col", new LocalDateTime(
                "2011-12-01T05:00:00")));
        mappings.add(new FieldToColumnMapping("timestampFieldJoda", "timestampfieldjoda"));
        mappings.add(new FieldToColumnMapping("dateFieldJdk", "datefieldjdk"));
        mappings.add(new FieldToColumnMapping("timestampFieldJdk", "timestampfieldjdk"));

        this.testLoad(sb.toString(), mappings, "\n", "~"); // single-char
        this.testLoad(sb.toString(), mappings, "\n", "!~!~"); // multi-char
        this.testLoad(sb.toString(), mappings, "####", "~"); // single-char,
        // non-line-break del
        this.testLoad(sb.toString(), mappings, "####", "!~!~"); // multi-char,
        // non-line-break del
    }

    private void testLoad(String tableCreationSql, MutableList<FieldToColumnMapping> mappings, String rowDel,
            String colDel) throws Exception {
        conn = dataSource.getConnection();
        this.jdbcTemplate.update(conn, tableCreationSql);

        // setup the data
        MutableList<TestBean> beans = Lists.mutable.empty();
        LocalDateTime timestampJodaVal = new LocalDateTime("2011-01-01T11:11:00");
        LocalDate dateJodaVal = new LocalDate("2011-02-02");
        Timestamp timestampJdkVal = new Timestamp(new GregorianCalendar(2011, 0, 1, 11, 11, 0).getTimeInMillis());
        Date dateJdkVal = new GregorianCalendar(2011, 1, 2).getTime();

        int rowCount = 13;
        for (int i = 0; i < rowCount; i++) {
            beans.add(new TestBean(1, "2", i, dateJdkVal, timestampJdkVal, dateJodaVal, timestampJodaVal));
        }

        // setup IQ loader
        IqLoadFileCreator loadFileCreator = new IqLoadFileCreator(tableName, mappings, this.iqLoadRoot, "loadFile",
                this.iqLoadMode, new BeanDataExtractor());
        loadFileCreator.setRowDel(rowDel);
        loadFileCreator.setColDel(colDel);
        loadFileCreator.openFile();
        System.out.println("Writing the file");
        loadFileCreator.writeToFile(beans);
        loadFileCreator.closeFile();

        System.out.println("Executing the SQL");

        String mysql = loadFileCreator.getIdLoadCommand(this.schema);
        int rowsInserted = this.jdbcTemplate.update(conn, mysql);

        assertEquals(rowCount, rowsInserted);
        // TODO would like a better insertion - possibly on the data too
    }

    static class TestBean {
        private final Integer myInt;
        private final String myString;
        private final long myCmId;
        private final Date dateFieldJdk;
        private final Timestamp timestampFieldJdk;
        private final LocalDate dateFieldJoda;
        private final LocalDateTime timestampFieldJoda;

        TestBean(Integer myInt, String myString, long myCmId, Date dateFieldJdk, Timestamp timestampFieldJdk,
                LocalDate dateFieldJoda, LocalDateTime timestampFieldJoda) {
            this.myInt = myInt;
            this.myString = myString;
            this.myCmId = myCmId;
            this.dateFieldJdk = dateFieldJdk;
            this.timestampFieldJdk = timestampFieldJdk;
            this.dateFieldJoda = dateFieldJoda;
            this.timestampFieldJoda = timestampFieldJoda;
        }

        public Integer getMyInt() {
            return this.myInt;
        }

        public String getMyString() {
            return this.myString;
        }

        public long getMyCmId() {
            return this.myCmId;
        }

        public Date getDateFieldJdk() {
            return this.dateFieldJdk;
        }

        public Timestamp getTimestampFieldJdk() {
            return this.timestampFieldJdk;
        }

        public LocalDate getDateFieldJoda() {
            return this.dateFieldJoda;
        }

        public LocalDateTime getTimestampFieldJoda() {
            return this.timestampFieldJoda;
        }
    }
}