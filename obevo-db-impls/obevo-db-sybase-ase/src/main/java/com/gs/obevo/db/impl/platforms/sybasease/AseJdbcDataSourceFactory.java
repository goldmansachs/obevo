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
package com.gs.obevo.db.impl.platforms.sybasease;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import org.apache.commons.lang3.Validate;

public class AseJdbcDataSourceFactory extends JdbcDataSourceFactory {
    @Override
    protected String createUrl(DbEnvironment env) {
        return createAseUrl(
                Validate.notNull(env.getDbHost(), "dbHost property must be specified for this DB environment: " + env),
                env.getDbPort()
        );
    }

    public static String createAseUrl(String host, int port) {
        return String.format("jdbc:sybase:Tds:%1$s:%2$s", host, port);
    }
}
