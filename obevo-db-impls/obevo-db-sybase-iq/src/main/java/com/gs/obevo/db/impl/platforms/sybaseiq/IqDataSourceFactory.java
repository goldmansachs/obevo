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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import java.sql.Driver;

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.inputreader.Credential;

/**
 * Factory interface for creating IQ DataSources. The data source can vary based on the connection type (jconnect vs. odbc).
 *
 * For history on the drivers to use, see here: http://scn.sap.com/community/sql-anywhere/blog/2014/05/02/connecting-to-sql-anywhere-using-jdbc
 */
public interface IqDataSourceFactory {
    /**
     * Indicates if this IqDataSourceFactory can work w/ the given driver.
     */
    boolean isDriverAccepted(Class<? extends Driver> driverClass);

    /**
     * Creates a JDBC DataSource from the given parameters.
     */
    DataSource createDataSource(DbEnvironment env, Credential credential, String schema, int numThreads);

    boolean isIqClientLoadSupported();
}
