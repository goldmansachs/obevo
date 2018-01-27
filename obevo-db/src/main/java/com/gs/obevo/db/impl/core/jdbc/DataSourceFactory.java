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

import javax.sql.DataSource;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.inputreader.Credential;

/**
 * Factory for creating {@link DataSource} instances for connecting to the given {@link DbEnvironment}. The factory
 * may also have to decide on which driver to use for the connection, assuming it is not already configured in the
 * DbEnvironment.
 */
public interface DataSourceFactory {
    /**
     * Creates the data source against the given environment with the provided credential and the ability to handle
     * multiple connections if needed for multi-threaded execution. It does not have to be numConnections == numThreads,
     * but whatever is reason able to get the work done.
     */
    DataSource createDataSource(DbEnvironment env, Credential credential, int numThreads);
}
