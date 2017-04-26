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

import java.sql.Connection;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.core.reader.PrepareDbChange;
import com.gs.obevo.db.impl.platforms.DefaultDbTranslationDialect;
import com.gs.obevo.db.impl.platforms.sqltranslator.InMemoryTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.SqlTranslatorConfigHelper;
import com.gs.obevo.db.impl.platforms.sqltranslator.impl.DateFormatterPostParsedSqlTranslator;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

public class Db2ToHsqlTranslationDialect extends DefaultDbTranslationDialect {
    @Override
    public ImmutableList<String> getInitSqls() {
        return Lists.immutable.with("SET DATABASE SQL SYNTAX DB2 TRUE");
    }

    @Override
    public ImmutableList<PrepareDbChange> getAdditionalTranslators() {
        SqlTranslatorConfigHelper configHelper = SqlTranslatorConfigHelper.createInMemoryDefault();
        configHelper.setNameMapper(new Db2SqlTranslatorNameMapper());
        configHelper.getPostColumnSqlTranslators()
                .with(new Db2ToInMemorySqlTranslator())
                .with(new Db2ToHsqlSqlTranslator());
        configHelper.getPostParsedSqlTranslators()
                .with(new Db2ToInMemorySqlTranslator())
                .with(new DateFormatterPostParsedSqlTranslator(Db2ToInMemorySqlTranslator.ACCEPTED_DATE_FORMATS));
        configHelper.getUnparsedSqlTranslators()
                .with(new Db2ToInMemorySqlTranslator());

        return Lists.immutable.<PrepareDbChange>with(new InMemoryTranslator(configHelper));
    }

    @Override
    public ImmutableSet<String> getDisabledChangeTypeNames() {
        return Sets.immutable.with(
                ChangeType.DEFAULT_STR,
                ChangeType.FUNCTION_STR,
                ChangeType.RULE_STR,
                ChangeType.SEQUENCE_STR,
                ChangeType.SP_STR,
                ChangeType.TRIGGER_STR,
                ChangeType.TRIGGER_INCREMENTAL_OLD_STR
        );
    }

    @Override
    public void initSchema(JdbcHelper jdbc, Connection conn) {
        updateAndIgnoreException(conn, jdbc, "create type vargraphic as LONGVARBINARY");
    }
}
