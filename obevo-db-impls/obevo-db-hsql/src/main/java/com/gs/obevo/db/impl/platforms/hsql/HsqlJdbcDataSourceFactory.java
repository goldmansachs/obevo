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
package com.gs.obevo.db.impl.platforms.hsql;

import java.io.File;
import java.io.IOException;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

public class HsqlJdbcDataSourceFactory extends JdbcDataSourceFactory {
    private static final String HSQL_FILE_PLACEHOLDER = ".zzz";

    // related to DEPLOYANY-201 - want to reuse the same DB file for consistency if we go w/ the same environment
    private static final ConcurrentMutableMap<String, String> dbNameToFileUrlMap = new ConcurrentHashMap<String, String>();

    @Override
    protected String createUrl(DbEnvironment env) {
        if (env.getDbServer() == null) {
            throw new IllegalArgumentException("Neither server name nor raw URL was defined for this environment"
                    + env.getName());
        } else {
            return getUrl(env.getDbServer(), env.isPersistToFile());
        }
    }

    public static String getHsqlUrl(String dbName) {
        return getUrl(dbName, false);
    }

    public static String getUrl(final String dbName, final boolean persistToFile) {
        String key = dbName + ":" + persistToFile;
        return dbNameToFileUrlMap.getIfAbsentPut(key, new Function0<String>() {
            @Override
            public String value() {
                return getUrlUncached(dbName, persistToFile);
            }
        });
    }

    private static String getUrlUncached(String dbName, boolean persistToFile) {
        if (persistToFile) {
            try {
                File tmpPlaceholder = File.createTempFile("hsqldb" + dbName, HSQL_FILE_PLACEHOLDER);
                File tmpDbFile = new File(tmpPlaceholder.getParentFile(), tmpPlaceholder.getName().substring(0,
                        tmpPlaceholder.getName().length() - HSQL_FILE_PLACEHOLDER.length()));

                return String.format("jdbc:hsqldb:file:%1$s", tmpDbFile.getAbsolutePath());
            } catch (IOException exc) {
                throw new DeployerRuntimeException(exc);
            }
        } else {
            return String.format("jdbc:hsqldb:mem:%1$s", dbName);
        }
    }
}
