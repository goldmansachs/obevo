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
package com.gs.obevo.db.impl.platforms.mssql;

import com.gs.obevo.db.impl.platforms.sqltranslator.SqlTranslatorNameMapper;

public class MsSqlSqlTranslatorNameMapper implements SqlTranslatorNameMapper {
    @Override
    public String remapIndexName(String name, String tableName) {
        // when dropping an index, it must be qualified in ASE. Convert it to the in-mem convention
        String[] nameParts = name.split("\\.");

        String objectName;
        if (nameParts.length == 1) {
            objectName = nameParts[0];
        } else if (nameParts.length == 2) {
            objectName = nameParts[1];
        } else {
            throw new IllegalArgumentException("Only expecting 1 dot for the split here in [" + name + "] found " +
                    nameParts.length);
        }
        return tableName + "_" + objectName;
    }

    @Override
    public String remapConstraintName(String name, String tableName) {
        return tableName + "_" + name;
    }
}
