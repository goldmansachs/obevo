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
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.jdbc.DefaultJdbcHandler;
import com.gs.obevo.db.impl.core.jdbc.JdbcHandler;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.AbstractSqlExecutor;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Db2SqlExecutor extends AbstractSqlExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(Db2SqlExecutor.class);

    private final DbEnvironment env;
    private final boolean autoReorgEnabled;
    private String currentPathSql;
    private final Object currentPathSqlLock = new Object();

    public Db2SqlExecutor(DataSource ds, DbEnvironment env) {
        super(ds);
        this.env = env;
        this.autoReorgEnabled = env.isAutoReorgEnabled();
    }

    private String getCurrentPathSql(Connection conn) {
        synchronized (currentPathSqlLock) {
            if (this.currentPathSql == null) {
                this.currentPathSql = getCurrentPathSql(conn, this.getJdbcTemplate(), this.env.getPhysicalSchemas());
            }
        }

        return this.currentPathSql;
    }

    /**
     * Package-private for unit testing only.
     */
    static String getCurrentPathSql(Connection conn, JdbcHelper jdbc, ImmutableSet<PhysicalSchema> physicalSchemas) {
        String path = jdbc.query(conn, "select current path from sysibm.sysdummy1", new ScalarHandler<String>());

        MutableList<String> currentSchemaPathList = Lists.mutable.of(path.split(",")).collect(new Function<String, String>() {
            @Override
            public String valueOf(String object) {
                if (object.startsWith("\"") && object.endsWith("\"")) {
                    return object.substring(1, object.length() - 1);
                } else {
                    return object;
                }
            }
        });

        // Rules on constructing this "set path" command:
        // 1) The existing default path must come first (respecting the existing connection), followed by the
        // schemas in our environment. The default path must take precedence.
        // 2) We cannot have duplicate schemas listed in the "set path" call; i.e. in case the schemas in our
        // environment config are already in the default schema.
        //
        // Given these two requirements, we use a LinkedHashSet
        LinkedHashSet<String> currentSchemaPaths = new LinkedHashSet(currentSchemaPathList);
        currentSchemaPaths.addAll(physicalSchemas.collect(PhysicalSchema.TO_PHYSICAL_NAME).castToSet());

        // This is needed to work w/ stored procedures
        // Ideally, we'd use "set current path current path, " + physicalSchemaList
        // However, we can't set this multiple times in a connection, as we can't have dupes in "current path"
        // Ideally, we could use BasicDataSource.initConnectionSqls, but this does not interoperate w/ the LDAP
        // datasource for JNDI-JDBC
        return "set path " + CollectionAdapter.adapt(currentSchemaPaths).makeString(",");
    }

    @Override
    public void setDataSourceSchema(Connection conn, PhysicalSchema schema) {
        JdbcHelper jdbc = this.getJdbcTemplate();
        jdbc.update(conn, this.getCurrentPathSql(conn));
        jdbc.update(conn, "SET SCHEMA " + schema.getPhysicalName());
    }

    @Override
    protected JdbcHandler getJdbcHandler() {
        return new Db2JdbcHandler();
    }

    private class Db2JdbcHandler extends DefaultJdbcHandler {
        @Override
        public boolean handleException(JdbcHelper jdbcHelper, Connection conn, int retryCount, DataAccessException e) {
            if (retryCount > 0) {
                return false;
            }

            if (e.getRootCause() instanceof SQLException) {
                SQLException cause = (SQLException) e.getRootCause();
                // Do not throw back the exception if error code = -20054
                MutableList<Integer> REORG_ERROR_CODES =
                        Lists.mutable.with(-20054, -668);
                SQLException next = cause.getNextException();
                if (next != null) {
                    cause = next;
                }
                if (REORG_ERROR_CODES.contains(cause.getErrorCode())) {
                    LOG.info("encountered " + cause.getErrorCode()
                            + " exception. Reorg needed");
                    Pair<PhysicalSchema, String> tableId =
                            Db2SqlExecutor.this.findTableNameFromException(cause, cause.getErrorCode());
                    LOG.debug("Parsed table name as [" + tableId + "] from SQLException");
                    if (Db2SqlExecutor.this.autoReorgEnabled) {
                        executeReorg(jdbcHelper, conn, tableId.getOne(), tableId.getTwo());
                    } else {
                        LOG.info(String
                                .format(
                                        "Reorg detected for table %1$s.%2$s, but auto-reorg is disabled. Hence, we will proceed as is without the reorg. Be warned that subsequent actions on this table may cause an error",
                                        tableId.getOne().getPhysicalName(), tableId.getTwo()));
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public static void executeReorg(JdbcHelper jdbc, Connection conn, PhysicalSchema schema, String table) {
        jdbc.update(conn, "call sysproc.admin_cmd ('reorg table " + schema + "." + table + "')");
    }

    private static final Pattern PATTERN_20054 = Pattern.compile("SQLERRMC: (\\w+)\\.(\\w+);");
    private static final Pattern PATTERN_668 = Pattern.compile("SQLERRMC: 7;(\\w+)\\.(\\w+)");
    /**
     * Finds the table name from SQL Exception. Based on the documentation as defined at
     * http://publib.boulder.ibm.com/infocenter/db2luw/v8/index.jsp?topic=/com.ibm.db2.udb.doc/ad/tjvjcerr.htm
     *
     * @param exception An instance of SQL Exception
     *                  TODO handle the hardcoding of these errorCodes a little bit better
     * @return The table name from SQL Exception
     */
    Pair<PhysicalSchema, String> findTableNameFromException(Exception exception, int errorCode) {
        String sqlErrorMC = exception.getMessage();

        Matcher matcher;
        switch (errorCode) {
        case -20054:
            matcher = PATTERN_20054.matcher(sqlErrorMC);
            break;
        case -668:
            matcher = PATTERN_668.matcher(sqlErrorMC);
            break;
        default:
            throw new IllegalArgumentException("Unhandled error code for reorg message parsing: " + errorCode);
        }

        String schemaName;
        String tableName;
        if (matcher.find()) {
            schemaName = matcher.group(1);
            tableName = matcher.group(2);
        } else {
            throw new IllegalArgumentException("Unhandled error code for reorg message parsing: " + errorCode);
        }
        return Tuples.pair(new PhysicalSchema(schemaName), tableName);
    }
}
