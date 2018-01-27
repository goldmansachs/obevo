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
package com.gs.obevo.db.impl.platforms.mssql;

import java.sql.Connection;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.DefaultDbTranslationDialect;
import com.gs.obevo.db.impl.platforms.sqltranslator.InMemoryTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.SqlTranslatorConfigHelper;
import com.gs.obevo.db.impl.platforms.sqltranslator.impl.DateFormatterPostParsedSqlTranslator;
import com.gs.obevo.impl.PrepareDbChange;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

public class MsSqlToH2TranslationDialect extends DefaultDbTranslationDialect {
    @Override
    public ImmutableList<String> getInitSqls() {
        return Lists.immutable.with("SET MODE MySQL");
    }

    @Override
    public ImmutableList<PrepareDbChange> getAdditionalTranslators() {
        SqlTranslatorConfigHelper configHelper = SqlTranslatorConfigHelper.createInMemoryDefault();
        configHelper.setNameMapper(new MsSqlSqlTranslatorNameMapper());
        configHelper.getPostColumnSqlTranslators()
                .with(new MsSqlToH2SqlTranslator());
        configHelper.getPostParsedSqlTranslators()
                .with(new MsSqlToInMemorySqlTranslator())
                .with(new DateFormatterPostParsedSqlTranslator(MsSqlToInMemorySqlTranslator.ACCEPTED_DATE_FORMATS))
                .with(new MsSqlToH2SqlTranslator());

        configHelper.getUnparsedSqlTranslators()
                .with(new MsSqlToInMemorySqlTranslator())
                .with(new MsSqlToH2DomainSqlTranslator());

        return Lists.immutable.<PrepareDbChange>with(new InMemoryTranslator(configHelper));
    }

    @Override
    public void initSchema(JdbcHelper jdbc, Connection conn) {
        this.updateAndIgnoreException(conn, jdbc, "create domain TEXT as LONGVARCHAR");
        this.updateAndIgnoreException(conn, jdbc, "create domain SMALLDATETIME as DATETIME");
        this.updateAndIgnoreException(conn, jdbc, "create domain XML as BLOB");
        this.updateAndIgnoreException(conn, jdbc, "create domain MONEY as NUMERIC(30,2)");
    }

    @Override
    public ImmutableSet<String> getDisabledChangeTypeNames() {
        return Sets.immutable.with(
                ChangeType.DEFAULT_STR,
                ChangeType.FUNCTION_STR,
                ChangeType.RULE_STR,
                ChangeType.SP_STR,
                ChangeType.TRIGGER_STR,
                ChangeType.TRIGGER_INCREMENTAL_OLD_STR
        );
    }
}
