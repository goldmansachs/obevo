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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.AbstractCatoDataSource;

public class QueryDataSource extends AbstractCatoDataSource {

    private final String query;
    private final Connection connection;

    private Statement stmt;
    private ResultSet rset;
    private ResultSetMetaData rsetmd;

    public QueryDataSource(String name, String url, String user, String password, String query) throws SQLException {
        this(name, DriverManager.getConnection(url, user, password), query);
    }

    public QueryDataSource(String name, Connection connection, String query) {
        super(name, null);
        this.connection = connection;
        this.query = query;
    }

    protected CatoDataObject nextDataObject() throws Exception {
        if (!this.rset.next()) {
            return null;
        }

        CatoDataObject dataObject = this.createDataObject();

        for (int i = 1; i < this.rsetmd.getColumnCount() + 1; i++) {
            dataObject.setValue(this.rsetmd.getColumnLabel(i), this.rset.getObject(i));
        }

        return dataObject;
    }

    protected void closeSource() throws Exception {
        this.rset.close();
        this.stmt.close();
        this.connection.close();
    }

    protected void openSource() throws Exception {
        this.stmt = this.connection.createStatement();
        this.rset = this.stmt.executeQuery(this.query);
        this.rsetmd = this.rset.getMetaData();
    }

    public Connection getConnection() {
        return this.connection;
    }
}
