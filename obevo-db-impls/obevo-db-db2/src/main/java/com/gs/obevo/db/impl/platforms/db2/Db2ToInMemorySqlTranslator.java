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
package com.gs.obevo.db.impl.platforms.db2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.impl.platforms.sqltranslator.PostColumnSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.PostParsedSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.UnparsedSqlTranslator;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTable;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTableColumn;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public class Db2ToInMemorySqlTranslator implements PostColumnSqlTranslator, PostParsedSqlTranslator, UnparsedSqlTranslator {
    private final Pattern defaultPattern = Pattern.compile("(?i)((?:not\\s+)?null)\\s+default\\s+(.*)");

    // SHANT add test cases for this in the integration test
    // these are allowable by db2 (i.e. to use dots instead of colons), but HSQL does not
    public static final ImmutableList<String> ACCEPTED_DATE_FORMATS = Lists.immutable.with(
            "yyyy-MM-dd-HH.mm.ss.SSS",
            "yyyy-MM-dd HH.mm.ss.SSS"
    );

    static final Pattern identityPattern =
            Pattern.compile("(?i)\\bgenerated\\s+(.*)as\\s+identity\\s*(\\(.*\\))?");

    protected final Pattern loggedPattern =
            Pattern.compile("(?i)(not\\s+)?\\blogged\\b");
    protected final Pattern compactPattern =
            Pattern.compile("(?i)(not\\s+)?\\bcompact\\b");

    @Override
    public String handlePostColumnText(String postColumnText, CreateTableColumn column, CreateTable table) {
        // default clause seems to require a reversal in HSQL - only for DB2?
        Matcher defaultMatcher = this.defaultPattern.matcher(postColumnText);
        while (defaultMatcher.find()) {
            String nullClause = defaultMatcher.group(1);
            String defaultClause = defaultMatcher.group(2);
            postColumnText = defaultMatcher.replaceFirst("DEFAULT " + defaultClause + " " + nullClause);
            defaultMatcher = this.defaultPattern.matcher(postColumnText);
        }

        Matcher loggedMatcher = this.loggedPattern.matcher(postColumnText);
        if (loggedMatcher.find()) {
            postColumnText = loggedMatcher.replaceAll(" ");
        }
        Matcher compactMatcher = this.compactPattern.matcher(postColumnText);
        if (compactMatcher.find()) {
            postColumnText = compactMatcher.replaceAll(" ");
        }

        return postColumnText;
    }

    @Override
    public String handleAnySqlPostTranslation(String string, Change change) {
        return string.replaceAll("(?i)current\\s+timestamp", "current_timestamp");
    }

    @Override
    public String handleRawFullSql(String string, Change change) {
        // filter out specific db2 system calls like reorg
        return string.replaceAll("(?i)CALL\\s+SYSPROC.*", "");
    }
}
