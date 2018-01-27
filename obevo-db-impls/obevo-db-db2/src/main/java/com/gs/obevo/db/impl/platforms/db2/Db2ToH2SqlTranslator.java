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
package com.gs.obevo.db.impl.platforms.db2;

import java.util.regex.Matcher;

import com.gs.obevo.db.impl.platforms.sqltranslator.PostColumnSqlTranslator;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTable;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTableColumn;

public class Db2ToH2SqlTranslator implements PostColumnSqlTranslator {
    @Override
    public String handlePostColumnText(String sql, CreateTableColumn column, CreateTable table) {
        Matcher identityMatcher = Db2ToInMemorySqlTranslator.identityPattern.matcher(sql);
        if (identityMatcher.find()) {
            return identityMatcher.replaceFirst("auto_increment ");
        } else {
            return sql;
        }
    }
}
