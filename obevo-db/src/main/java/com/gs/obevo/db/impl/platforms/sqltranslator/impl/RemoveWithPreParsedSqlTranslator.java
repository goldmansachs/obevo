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
package com.gs.obevo.db.impl.platforms.sqltranslator.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.db.impl.platforms.sqltranslator.PreParsedSqlTranslator;

public class RemoveWithPreParsedSqlTranslator implements PreParsedSqlTranslator {
    /**
     * Only match the WITH if it starts the line
     */
    private final Pattern withPattern = Pattern.compile("(?i)^\\s+with.*$", Pattern.MULTILINE);

    @Override
    public String preprocessSql(String sql) {
        Matcher withMatcher = this.withPattern.matcher(sql);
        if (withMatcher.find()) {
            sql = withMatcher.replaceAll("");
        }

        return sql;
    }
}
