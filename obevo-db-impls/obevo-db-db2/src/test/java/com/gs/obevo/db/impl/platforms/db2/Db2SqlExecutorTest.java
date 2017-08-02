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
package com.gs.obevo.db.impl.platforms.db2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.ResultSetHandler;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Db2SqlExecutorTest {
    private Db2SqlExecutor executor;

    @Before
    public void setup() {
        this.executor = new Db2SqlExecutor(mock(DataSource.class), mock(DbEnvironment.class));
    }

    @Test
    public void testSetPathSqlCreation() {
        // In the test setup, we duplicate sB across the JDBC "set path" return result and the schemas defined in the config to ensure that we drop the dupes in the final output
        JdbcHelper jdbc = mock(JdbcHelper.class);
        Connection conn = mock(Connection.class);
        when(jdbc.query(Matchers.any(Connection.class), Matchers.anyString(), Matchers.<ResultSetHandler<Object>>any())).thenReturn("s3,\"s1\",\"sB\",s2");

        // Use LinkedHashSet just to make the test setup easy
        LinkedHashSet<PhysicalSchema> schemas = new LinkedHashSet<PhysicalSchema>(Arrays.asList(new PhysicalSchema("sA"), new PhysicalSchema("sB"), new PhysicalSchema("sC")));

        assertEquals("set path s3,s1,sB,s2,sA,sC", Db2SqlExecutor.getCurrentPathSql(conn, jdbc, SetAdapter.adapt(schemas).toImmutable()));
    }

    @Test
    public void testFindTableNameFromSQLException20054WithSpace() {
        SQLException diagnosable = mock(SQLException.class);
        when(diagnosable.getMessage()).thenReturn("DB2 SQL error: SQLCODE: -20054, SQLSTATE: 55019, SQLERRMC: _MY_SCHEMA.MYTAB1_;23");
        assertEquals(Tuples.pair(new PhysicalSchema("_MY_SCHEMA"), "MYTAB1_"),
                this.executor.findTableNameFromException(diagnosable, -20054));
        verify(diagnosable, times(1)).getMessage();
    }

    @Test
    public void testFindTableNameFromSQLException20054() {
        SQLException diagnosable = mock(SQLException.class);
        when(diagnosable.getMessage()).thenReturn("DB2 SQL error: SQLCODE: -20054, SQLSTATE: 55019, SQLERRMC=MY_SCHEMA.MYTAB1_;23");
        assertEquals(Tuples.pair(new PhysicalSchema("MY_SCHEMA"), "MYTAB1_"),
                this.executor.findTableNameFromException(diagnosable, -20054));
        verify(diagnosable, times(1)).getMessage();
    }

    @Test
    public void testFindTableNameFromSQLException668WithSpace() {
        SQLException diagnosable = mock(SQLException.class);
        // Note: We will be using SqlErrmc code alone inside the findTableFromSQLException method. Hence we are mocking
        // only the getSqlErrmc method
        when(diagnosable.getMessage()).thenReturn("DB2 SQL error: SQLCODE: -668, SQLSTATE: 57016, SQLERRMC: 7;7MY_SCHEMA.MYTAB2_");
        assertEquals(Tuples.pair(new PhysicalSchema("7MY_SCHEMA"), "MYTAB2_"),
                this.executor.findTableNameFromException(diagnosable, -668));

        verify(diagnosable, times(1)).getMessage();
    }

    @Test
    public void testFindTableNameFromSQLException668() {
        SQLException diagnosable = mock(SQLException.class);
        // Note: We will be using SqlErrmc code alone inside the findTableFromSQLException method. Hence we are mocking
        // only the getSqlErrmc method
        when(diagnosable.getMessage()).thenReturn("random prefix - DB2 SQL Error: SQLCODE=-668, SQLSTATE=57016, SQLERRMC=7;MY_SCHEMA.MYTAB3, DRIVER=3.59.81");
        assertEquals(Tuples.pair(new PhysicalSchema("MY_SCHEMA"), "MYTAB3"),
                this.executor.findTableNameFromException(diagnosable, -668));

        verify(diagnosable, times(1)).getMessage();
    }
}
