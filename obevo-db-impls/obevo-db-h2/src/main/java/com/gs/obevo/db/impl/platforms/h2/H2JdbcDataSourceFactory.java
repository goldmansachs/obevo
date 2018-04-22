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
package com.gs.obevo.db.impl.platforms.h2;

import java.io.File;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2JdbcDataSourceFactory extends JdbcDataSourceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(H2JdbcDataSourceFactory.class);

    // We'd like to reuse the same DB file for consistency if we go w/ the same environment
    private static final ConcurrentMutableMap<String, String> dbNameToFileUrlMap = new ConcurrentHashMap<String, String>();

    @Override
    protected String createUrl(DbEnvironment env) {
        if (env.getDbServer() == null) {
            throw new IllegalArgumentException("Neither server name nor raw URL was defined for this environment"
                    + env.getName());
        } else {
            String url = getUrl(env.getDbServer(), env.isPersistToFile());
            env.setJdbcUrl(url);  // set the url here for usage by client libraries as well
            return url;
        }
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
        Validate.notNull(dbName, "dbName parameter must be specified");
        String url;
        if (persistToFile) {
            // In order to allow access to the db from multiple processes, the FILE_LOCK=NO setting is needed.
            // This however, does not prevent concurrency issues from multiple writers.
            // Other options found in the docs that we are not using: MVCC=TRUE;FILE_LOCK=SERIALIZED
            String dbFileName = new File(SystemUtils.getJavaIoTmpDir(), dbName).getAbsolutePath();
            LOG.info("writing db to file:{}", dbFileName);
            url = String.format("jdbc:h2:file:%s;FILE_LOCK=NO", dbFileName);
        } else {
            // use DB_CLOSE_DELAY=-1 to keep the DB open for the duration of the JVM
            url = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1", dbName);
        }
        LOG.info("setting up in memory db: {}", url);
        return url;
    }
}
