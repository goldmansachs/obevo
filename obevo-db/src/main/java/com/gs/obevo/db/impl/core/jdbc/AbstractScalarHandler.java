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
package com.gs.obevo.db.impl.core.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Alternate implementation of {@link org.apache.commons.dbutils.handlers.ScalarHandler} from apache commons-dbutils,
 * but forced a return to Integer as some of the existing obevo calls expected a Long, and I wanted to minimize
 * refactoring. Previously was using SingleColumnRowMapper in Spring, which let you define the type. This could use some
 * refactoring.
 *
 * {@code ResultSetHandler} implementation that converts one
 * {@code ResultSet} column into an Object. This class is thread safe.
 *
 * @param <T> The type of the scalar
 * @see ResultSetHandler
 */
abstract class AbstractScalarHandler<T> implements ResultSetHandler<T> {

    /**
     * The column number to retrieve.
     */
    private final int columnIndex;

    /**
     * The column name to retrieve.  Either columnName or columnIndex
     * will be used but never both.
     */
    private final String columnName;

    /**
     * Creates a new instance of ScalarHandler.  The first column will
     * be returned from <code>handle()</code>.
     */
    public AbstractScalarHandler() {
        this(1, null);
    }

    /**
     * Creates a new instance of ScalarHandler.
     *
     * @param columnIndex The index of the column to retrieve from the
     *                    <code>ResultSet</code>.
     */
    public AbstractScalarHandler(int columnIndex) {
        this(columnIndex, null);
    }

    /**
     * Creates a new instance of ScalarHandler.
     *
     * @param columnName The name of the column to retrieve from the
     *                   <code>ResultSet</code>.
     */
    public AbstractScalarHandler(String columnName) {
        this(1, columnName);
    }

    /**
     * Helper constructor
     *
     * @param columnIndex The index of the column to retrieve from the
     *                    <code>ResultSet</code>.
     * @param columnName  The name of the column to retrieve from the
     *                    <code>ResultSet</code>.
     */
    private AbstractScalarHandler(int columnIndex, String columnName) {
        this.columnIndex = columnIndex;
        this.columnName = columnName;
    }

    /**
     * Returns one <code>ResultSet</code> column as an object via the
     * <code>ResultSet.getObject()</code> method that performs type
     * conversions.
     *
     * @param rs <code>ResultSet</code> to process.
     * @return The column or <code>null</code> if there are no rows in
     * the <code>ResultSet</code>.
     * @throws SQLException       if a database access error occurs
     * @throws ClassCastException if the class datatype does not match the column type
     * @see ResultSetHandler#handle(ResultSet)
     */
    // We assume that the user has picked the correct type to match the column
    // so getObject will return the appropriate type and the cast will succeed.
    @SuppressWarnings("unchecked")
    @Override
    public T handle(ResultSet rs) throws SQLException {

        if (rs.next()) {
            if (this.columnName == null) {
                return getValue(rs, this.columnIndex);
            } else {
                return getValue(rs, this.columnName);
            }
        } else {
            return null;
        }
    }

    protected abstract T getValue(ResultSet rs, int columnIndex) throws SQLException;

    protected abstract T getValue(ResultSet rs, String columnName) throws SQLException;
}
