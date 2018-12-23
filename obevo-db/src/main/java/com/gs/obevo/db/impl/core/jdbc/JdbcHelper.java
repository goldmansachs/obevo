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
package com.gs.obevo.db.impl.core.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that keeps only the minimal calls that were in Spring JdbcTemplate and SimpleJdbcTemplate.
 * This lets us switch to using Apache DbUtils without changing too much of the code
 *
 * We don't leverage QueryRunner from dbutils as we had a few problems w/ it for regular statements and batch inserts.
 * We need to use PreparedStatement only for batch inserts and set the params manually (QueryRunner threw exceptions in
 * a few cases, e.g. w/ Sybase Text types). Otherwise, we want to stick w/ Statement (and not PreparedStatement) for
 * everything else as some databases (particularly Sybase ASE) are quite finicky on how PreparedStatement is used
 *
 * Note - all exceptions rethrown here should throw DataAccessException, as this is the class that clients can check
 * for in case they want to handle certain types of exceptions (e.g. DB2 reorg)
 */
public class JdbcHelper {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcHelper.class);

    private final JdbcHandler jdbcHandler;
    private final boolean parameterTypeEnabled;

    public JdbcHelper() {
        this(null, true);
    }

    public JdbcHelper(JdbcHandler jdbcHandler, boolean parameterTypeEnabled) {
        this.jdbcHandler = jdbcHandler != null ? jdbcHandler : new DefaultJdbcHandler();
        this.parameterTypeEnabled = parameterTypeEnabled;
    }

    public void execute(Connection conn, String sql) {
        this.update(conn, sql);
    }

    public int update(Connection conn, String sql) {
        return this.updateInternal(conn, 0, sql, new Object[0]);
    }

    public int update(Connection conn, String sql, Object... args) {
        return this.updateInternal(conn, 0, sql, args);
    }

    private int updateInternal(Connection conn, int retryCount, String sql, Object... args) {
        this.jdbcHandler.preUpdate(conn, this);

        Statement statement = null;
        PreparedStatement ps = null;
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing update on {}: {} with args: {}", displayConnection(conn), sql, args);
            }
            if (args.length == 0) {
                // For args length == 0, we use regular Statements and not PreparedStatements
                // This is because of http://www.selikoff.net/2008/08/04/question-mark-%E2%80%98%E2%80%99-characters-as-text-in-jdbc/
                // In short - question marks are naively interpreted by PreparedStatements as parameters, even if in
                // strings or comments
                // This can affect legacy DDL files that may have such comments sprinkled in. So we go w/ Statements,
                // which is what spring-jdbc did (this product had used spring-jdbc in an early incarnation, which was
                // when we discovered this issue)
                statement = conn.createStatement();
                return statement.executeUpdate(sql);
            } else {
                ps = conn.prepareStatement(sql);
                for (int j = 0; j < args.length; j++) {
                    if (!parameterTypeEnabled || args[j] != null) {
                        ps.setObject(j + 1, args[j]);
                    } else {
                        ps.setNull(j + 1, ps.getParameterMetaData().getParameterType(j + 1));
                    }
                }

                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            DataAccessException dataAccessException = new DataAccessException(e);
            boolean retry = this.jdbcHandler.handleException(this, conn, retryCount, dataAccessException);
            if (retry) {
                return this.updateInternal(conn, retryCount + 1, sql, args);
            } else {
                throw dataAccessException;
            }
        } finally {
            DbUtils.closeQuietly(statement);
            DbUtils.closeQuietly(ps);
        }
    }

    public int[] batchUpdate(Connection conn, String sql, Object[][] argsArray) {
        return batchUpdateInternal(conn, 0, sql, argsArray);
    }

    private int[] batchUpdateInternal(Connection conn, int retryCount, String sql, Object[][] argsArray) {
        PreparedStatement ps = null;
        try {
            this.jdbcHandler.preUpdate(conn, this);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing batch update on {}: {} with args: {}", displayConnection(conn), sql, argsArray);
            }
            ps = conn.prepareStatement(sql);
            for (Object[] args : argsArray) {
                for (int j = 0; j < args.length; j++) {
                    if (!parameterTypeEnabled || args[j] != null) {
                        ps.setObject(j + 1, args[j]);
                    } else {
                        ps.setNull(j + 1, ps.getParameterMetaData().getParameterType(j + 1));
                    }
                }
                ps.addBatch();
            }

            return ps.executeBatch();
        } catch (SQLException e) {
            DataAccessException dataAccessException = new DataAccessException(e);
            boolean retry = this.jdbcHandler.handleException(this, conn, retryCount, dataAccessException);
            if (retry) {
                return this.batchUpdateInternal(conn, retryCount + 1, sql, argsArray);
            } else {
                LOG.error("Error during batch execution; will print out the full batch stack trace: ");
                throw dataAccessException;
            }
        } finally {
            DbUtils.closeQuietly(ps);
        }
    }

    private void logSqlBatchException(SQLException e, int level) {
        LOG.error("Batch stack trace level #{}", level);
        LOG.error("", e);
        if (e != null) {
            this.logSqlBatchException(e.getNextException(), level + 1);
        }
    }

    public <T> T query(Connection conn, String sql, ResultSetHandler<T> resultSetHandler) {
        ResultSet resultSet = null;
        Statement stmt = null;
        try {
            resultSet = queryAndLeaveStatementOpen(conn, sql);
            stmt = resultSet.getStatement();
            return resultSetHandler.handle(resultSet);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            DbUtils.closeQuietly(stmt);
            DbUtils.closeQuietly(resultSet);
        }
    }

    public ResultSet queryAndLeaveStatementOpen(Connection conn, String sql) {
        return queryAndLeaveStatementOpenInternal(conn, 0, sql);
    }

    private ResultSet queryAndLeaveStatementOpenInternal(Connection conn, int retryCount, String sql) {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing query on {}: {}", displayConnection(conn), sql);
            }
            return statement.executeQuery(sql);
        } catch (SQLException e) {
            DbUtils.closeQuietly(statement);
            DataAccessException dataAccessException = new DataAccessException(e);
            boolean retry = this.jdbcHandler.handleException(this, conn, retryCount, dataAccessException);
            if (retry) {
                return this.queryAndLeaveStatementOpenInternal(conn, retryCount + 1, sql);
            } else {
                throw dataAccessException;
            }
        }
    }

    public int queryForInt(Connection conn, String sql) {
        return this.query(conn, sql, new ScalarIntHandler());
    }

    public Long queryForLong(Connection conn, String sql) {
        return this.query(conn, sql, new ScalarLongHandler());
    }

    public MutableList<Map<String, Object>> queryForList(Connection conn, String sql) {
        return ListAdapter.adapt(this.query(conn, sql, new MapListHandler()));
    }

    public MutableMap<String, Object> queryForMap(Connection conn, String sql) {
        return MapAdapter.adapt(this.query(conn, sql, new MapHandler()));
    }

    private String displayConnection(Connection connection) {
        return "connection[" + System.identityHashCode(connection);
    }
}
