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
package com.gs.obevo.db.impl.platforms.postgresql;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;

public class PostgreSqlJdbcDataSourceFactory extends JdbcDataSourceFactory {
    @Override
    protected String createUrl(DbEnvironment env) {
        return env.getJdbcUrl() == null ? String.format("jdbc:postgresql://%1$s:%2$s/%3$s", env.getDbHost(), env.getDbPort(), env.getDbServer()) : env.getJdbcUrl();
    }
}
