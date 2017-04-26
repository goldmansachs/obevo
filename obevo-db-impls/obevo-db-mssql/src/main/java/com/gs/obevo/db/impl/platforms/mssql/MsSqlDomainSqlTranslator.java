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
package com.gs.obevo.db.impl.platforms.mssql;

import java.util.Arrays;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.impl.platforms.sqltranslator.UnparsedSqlTranslator;
import com.gs.obevo.db.sqlparser.tokenparser.SqlToken;
import com.gs.obevo.db.sqlparser.tokenparser.SqlTokenParser;
import com.gs.obevo.db.sqlparser.tokenparser.SqlTokenType;
import com.gs.obevo.impl.text.CommentRemover;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;

public abstract class MsSqlDomainSqlTranslator implements UnparsedSqlTranslator {
    @Override
    public String handleRawFullSql(String sql, Change change) {
        sql = CommentRemover.removeComments(sql, "sybase sp_addtype conversion").trim();
        if (sql.startsWith("sp_addtype")) {
            // all the params are in string literals, e.g. 'param'. Let's extract it out
            MutableList<SqlToken> allParts = new SqlTokenParser().parseTokens(sql);
            MutableList<SqlToken> parts = allParts.select(Predicates.attributeEqual(SqlToken.TO_TOKEN_TYPE,
                    SqlTokenType.STRING));

            String domainName = null;
            String domainType = null;
            String domainProps = null;
            for (int i = 0; i < parts.size(); i++) {
                String part = this.stripAddTypeParam(parts.get(i).getText().trim());
                if (i == 0) {
                    domainName = part;
                } else if (i == 1) {
                    domainType = part;
                } else if (i == 2) {
                    domainProps = part;
                } else {
                    throw new IllegalArgumentException("Not expecting more than 3 args here, but got: " +
                            Arrays.asList(parts));
                }
            }

            return this.createDomainSql(domainName, domainType, domainProps);
        } else {
            return sql;
        }
    }

    protected abstract String createDomainSql(String domainName, String domainType, String domainProps);

    private String stripAddTypeParam(String param) {
        int firstQuoteIndex = param.indexOf('\'');
        int lastQuoteIndex = param.lastIndexOf('\'');
        return param.substring(firstQuoteIndex + 1, lastQuoteIndex);
    }
}
