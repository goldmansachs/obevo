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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handler for long/bigint.
 */
class ScalarLongHandler extends AbstractScalarHandler<Long> {
    @Override
    protected Long getValue(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getLong(columnIndex);
    }

    @Override
    protected Long getValue(ResultSet rs, String columnName) throws SQLException {
        return rs.getLong(columnName);
    }
}
