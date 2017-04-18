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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import java.sql.Connection;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.impl.platforms.AbstractSqlExecutor;

/**
 * See <class>IqDataSource</class> javadoc for why this class is separate from the other SqlExecutor classes
 * For that same reason, we must mark this as non-transactional
 */
public class IqSqlExecutor extends AbstractSqlExecutor {
    private final IqDataSource iqDs;

    public IqSqlExecutor(IqDataSource iqDs) {
        super(iqDs);
        this.iqDs = iqDs;
    }

    @Override
    public void setDataSourceSchema(Connection conn, PhysicalSchema schema) {
        iqDs.setCurrentSchema(schema);
    }
}
