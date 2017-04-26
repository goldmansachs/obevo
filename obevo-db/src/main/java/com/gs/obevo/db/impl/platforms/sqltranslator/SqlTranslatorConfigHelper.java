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
package com.gs.obevo.db.impl.platforms.sqltranslator;

import com.gs.obevo.db.impl.platforms.sqltranslator.impl.DefaultSqlTranslatorNameMapper;
import com.gs.obevo.db.impl.platforms.sqltranslator.impl.DefaultUnparsedSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.impl.ForeignKeyNameRemovalSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.impl.RemoveWithPreParsedSqlTranslator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

/**
 * config to help new clients take in the defaults recommended here
 */
public class SqlTranslatorConfigHelper {
    public static SqlTranslatorConfigHelper createInMemoryDefault() {
        SqlTranslatorConfigHelper configHelper = new SqlTranslatorConfigHelper();
        configHelper.setNameMapper(new DefaultSqlTranslatorNameMapper());
        configHelper.setColumnSqlTranslators(FastList.<ColumnSqlTranslator>newListWith());
        configHelper.setPostColumnSqlTranslators(FastList.<PostColumnSqlTranslator>newListWith());
        configHelper.setPostParsedSqlTranslators(FastList.<PostParsedSqlTranslator>newListWith(new ForeignKeyNameRemovalSqlTranslator()));
        configHelper.setPreParsedSqlTranslators(FastList.<PreParsedSqlTranslator>newListWith(new RemoveWithPreParsedSqlTranslator()));
        configHelper.setUnparsedSqlTranslators(FastList.<UnparsedSqlTranslator>newListWith(new DefaultUnparsedSqlTranslator()));
        return configHelper;
    }

    private SqlTranslatorNameMapper nameMapper;
    private MutableList<ColumnSqlTranslator> columnSqlTranslators;
    private MutableList<PostColumnSqlTranslator> postColumnSqlTranslators;
    private MutableList<PostParsedSqlTranslator> postParsedSqlTranslators;
    private MutableList<PreParsedSqlTranslator> preParsedSqlTranslators;
    private MutableList<UnparsedSqlTranslator> unparsedSqlTranslators;

    public SqlTranslatorNameMapper getNameMapper() {
        return this.nameMapper;
    }

    public void setNameMapper(SqlTranslatorNameMapper nameMapper) {
        this.nameMapper = nameMapper;
    }

    public MutableList<ColumnSqlTranslator> getColumnSqlTranslators() {
        return columnSqlTranslators;
    }

    public void setColumnSqlTranslators(MutableList<ColumnSqlTranslator> columnSqlTranslators) {
        this.columnSqlTranslators = columnSqlTranslators;
    }

    public MutableList<PostColumnSqlTranslator> getPostColumnSqlTranslators() {
        return this.postColumnSqlTranslators;
    }

    public void setPostColumnSqlTranslators(MutableList<PostColumnSqlTranslator> postColumnSqlTranslators) {
        this.postColumnSqlTranslators = postColumnSqlTranslators;
    }

    public MutableList<PostParsedSqlTranslator> getPostParsedSqlTranslators() {
        return this.postParsedSqlTranslators;
    }

    public void setPostParsedSqlTranslators(MutableList<PostParsedSqlTranslator> postParsedSqlTranslators) {
        this.postParsedSqlTranslators = postParsedSqlTranslators;
    }

    public MutableList<PreParsedSqlTranslator> getPreParsedSqlTranslators() {
        return this.preParsedSqlTranslators;
    }

    public void setPreParsedSqlTranslators(MutableList<PreParsedSqlTranslator> preParsedSqlTranslators) {
        this.preParsedSqlTranslators = preParsedSqlTranslators;
    }

    public MutableList<UnparsedSqlTranslator> getUnparsedSqlTranslators() {
        return this.unparsedSqlTranslators;
    }

    public void setUnparsedSqlTranslators(MutableList<UnparsedSqlTranslator> unparsedSqlTranslators) {
        this.unparsedSqlTranslators = unparsedSqlTranslators;
    }
}
