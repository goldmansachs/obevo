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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.impl.core.util.RegexpPatterns;
import com.gs.obevo.db.impl.platforms.sqltranslator.PostParsedSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.UnparsedSqlTranslator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public final class AseToInMemorySqlTranslator implements PostParsedSqlTranslator, UnparsedSqlTranslator {

    // ASE allows a wide range of formats, so we parse for them
    public static final ImmutableList<String> ACCEPTED_DATE_FORMATS = Lists.immutable.with(
            "dd MMM yyyy HH:mm:ss",
            "MMM dd yyyy",
            "yyyyMMdd",
            "dd/MM/yyyy"
    );

    @Override
    public String handleAnySqlPostTranslation(String string, Change change) {
        string = string.replaceAll("(?i)getdate\\(\\)", "CURRENT_DATE");

        // keeping for backwards-compatibility
        string = string.replaceAll("(?i)dbo\\.", "");

        // only for Sybase ASE - the "modify" keyword should change to "alter column"
        Matcher modifyMatcher = RegexpPatterns.modifyTablePattern.matcher(string);
        if (modifyMatcher.find()) {
            string = modifyMatcher.replaceFirst("ALTER TABLE " + modifyMatcher.group(1) + " ALTER COLUMN");
        }

        return string;
    }

    @Override
    public String handleRawFullSql(String string, Change change) {
        string = string.replaceAll("(?i).*sp_bindefault.+", "");
        string = string.replaceAll("(?i).*sp_bindrule.+", "");

        return string;
    }
}
