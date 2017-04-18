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

import com.gs.obevo.db.impl.platforms.sqltranslator.PostColumnSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.PostParsedSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.PreParsedSqlTranslator;
import com.gs.obevo.db.impl.platforms.sqltranslator.SqlTranslatorNameMapper;
import com.gs.obevo.db.impl.platforms.sqltranslator.UnparsedSqlTranslator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;

public class DefaultSqlTranslatorConfigHelper {
    private SqlTranslatorNameMapper nameMapper = new DefaultSqlTranslatorNameMapper();
    private MutableList<PostColumnSqlTranslator> postColumnSqlTranslators = Lists.mutable.empty();
    private MutableList<PostParsedSqlTranslator> postParsedSqlTranslators = FastList.<PostParsedSqlTranslator>newListWith(new ForeignKeyNameRemovalSqlTranslator());
    private MutableList<PreParsedSqlTranslator> preParsedSqlTranslators = FastList.<PreParsedSqlTranslator>newListWith(new RemoveWithPreParsedSqlTranslator());
    private MutableList<UnparsedSqlTranslator> unparsedSqlTranslators = FastList.<UnparsedSqlTranslator>newListWith(new DefaultUnparsedSqlTranslator());

    public SqlTranslatorNameMapper getNameMapper() {
        return this.nameMapper;
    }

    public void setNameMapper(SqlTranslatorNameMapper nameMapper) {
        this.nameMapper = nameMapper;
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
