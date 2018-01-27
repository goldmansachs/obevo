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
package com.gs.obevo.db.impl.platforms.sybasease;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.impl.platforms.sqltranslator.UnparsedSqlTranslator;

/**
 * For translating sp_rename.
 */
public class AseRenameTranslator implements UnparsedSqlTranslator {
    private static final Pattern SP_RENAME_PATTERN = Pattern.compile("(?i)\\s*sp_rename\\s+'(\\w+)\\.(\\w+)'\\s*,\\s*'(\\w+)'", Pattern.MULTILINE);

    @Override
    public String handleRawFullSql(String sql, Change change) {
        Matcher matcher = SP_RENAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            // prepend a space in case there is some text that comes up before this
            String replacementSql = " ALTER TABLE " + matcher.group(1) + " ALTER COLUMN " + matcher.group(2) + " RENAME TO " + matcher.group(3);
            return matcher.replaceFirst(replacementSql);
        }

        // otherwise, return the SQL as is
        return sql;
    }
}
