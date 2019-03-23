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
package com.gs.obevo.db.impl.platforms.postgresql;

import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.DefaultDbTranslationDialect;
import com.gs.obevo.db.impl.platforms.sqltranslator.InMemoryTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.PostColumnSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.PreParsedSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.SqlTranslatorConfigHelper;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTable;
import com.gs.obevo.db.sqlparser.syntaxparser.CreateTableColumn;
import com.gs.obevo.impl.PrepareDbChange;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

public class PostgreSqlToHsqlTranslationDialect extends DefaultDbTranslationDialect {
    private final PostColumnSqlTranslator replaceNextvalWithIdentity = new PostColumnSqlTranslator() {
        private final Pattern defaultPattern = Pattern.compile("(?i)default\\s+nextval.*", Pattern.DOTALL);

        @Override
        public String handlePostColumnText(String postColumnText, CreateTableColumn column, CreateTable table) {
            Matcher defaultMatcher = defaultPattern.matcher(postColumnText);
            if (defaultMatcher.find()) {
                postColumnText = defaultMatcher.replaceFirst("IDENTITY");
            }

            return postColumnText;
        }
    };

    private final PreParsedSqlTranslator substituteCreateOrReplace = new PreParsedSqlTranslator() {
        Pattern pattern = Pattern.compile("(?i)^\\s*create\\s+or\\s+replace\\s+", Pattern.DOTALL);
        @Override
        public String preprocessSql(String sql) {
            Matcher matcher = pattern.matcher(sql);
            if (matcher.find()) {
                sql = matcher.replaceFirst("create ");
            }
            return sql;
        }
    };

    @Override
    public ImmutableList<String> getInitSqls() {
        return Lists.immutable.with(
                "SET DATABASE SQL SYNTAX PGS TRUE"
                , "SET DATABASE TRANSACTION CONTROL MVCC"
        );
    }

    @Override
    public ImmutableList<PrepareDbChange> getAdditionalTranslators() {
        SqlTranslatorConfigHelper configHelper = SqlTranslatorConfigHelper.createInMemoryDefault();

        configHelper.getPreParsedSqlTranslators()
                .with(substituteCreateOrReplace);
        configHelper.getPostColumnSqlTranslators()
                .with(replaceNextvalWithIdentity);
        return Lists.immutable.<PrepareDbChange>with(new InMemoryTranslator(configHelper));
    }

    @Override
    public void initSchema(JdbcHelper jdbc, Connection conn) {
        updateAndIgnoreException(conn, jdbc, "create type int4 as integer");
    }

    @Override
    public ImmutableSet<String> getDisabledChangeTypeNames() {
        return Sets.immutable.of(ChangeType.FUNCTION_STR, ChangeType.SP_STR);
    }
}
