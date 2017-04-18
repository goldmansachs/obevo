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
package com.gs.obevo.db.sqlparser.tokenparser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class SqlTokenParser {

    public MutableList<SqlToken> parseTokens(String str) {
        str += "\n";  // to handle case where last line entry is // ending comment
        MutableList<SqlToken> tokens = Lists.mutable.empty();

        SqlTokenParserImpl parser = new SqlTokenParserImpl(new StringReader(str));

        boolean inToken = false;
        List<String> queuedTokens = new ArrayList<String>();
        for (Token token = parser.getNextToken(); token.kind != SqlTokenParserImplConstants.EOF;
             token = parser.getNextToken()) {

            SqlTokenType tokenType;
            switch (token.kind) {
            case SqlTokenParserImplConstants.OTHER:
                inToken = true;
                queuedTokens.add(token.image);
                break;
            case SqlTokenParserImplConstants.DOUBLE_QUOTE_STRING:
            case SqlTokenParserImplConstants.SINGLE_QUOTE_STRING:
                tokenType = SqlTokenType.STRING;
                this.processToken(inToken, queuedTokens, tokens, tokenType, token);
                inToken = false;
                break;
            case SqlTokenParserImplConstants.WHITESPACE:
                tokenType = SqlTokenType.WHITESPACE;
                this.processToken(inToken, queuedTokens, tokens, tokenType, token);
                inToken = false;
                break;
            case SqlTokenParserImplConstants.SINGLE_LINE_COMMENT1:
            case SqlTokenParserImplConstants.SINGLE_LINE_COMMENT2:
            case SqlTokenParserImplConstants.MULTI_LINE_COMMENT:
                tokenType = SqlTokenType.COMMENT;
                this.processToken(inToken, queuedTokens, tokens, tokenType, token);
                inToken = false;
                break;
            default:
                throw new IllegalArgumentException("Not expecting this token type: " + token.kind);
            }
        }

        if (inToken) {
            String queuedTokenStr = StringUtils.join(queuedTokens, "");
            queuedTokens.clear();
            tokens.add(new SqlToken(SqlTokenType.TOKEN, queuedTokenStr));
        }

        if (!tokens.isEmpty()) {
            int lastIndex = tokens.size() - 1;
            SqlToken lastToken = tokens.get(lastIndex);
            String lastText = lastToken.getText();
            if (!lastText.isEmpty() && lastText.charAt(lastText.length() - 1) == '\n') {
                lastText = lastText.substring(0, lastText.length() - 1);
                if (lastText.isEmpty()) {
                    tokens.remove(lastIndex);
                } else {
                    tokens.set(lastIndex, new SqlToken(lastToken.getTokenType(), lastText));
                }
            }
        }
        return tokens;
    }

    private void processToken(boolean inToken, List<String> queuedTokens, List<SqlToken> tokens, SqlTokenType tokenType,
            Token token) {
        if (inToken) {
            String queuedTokenStr = StringUtils.join(queuedTokens, "");
            queuedTokens.clear();
            tokens.add(new SqlToken(SqlTokenType.TOKEN, queuedTokenStr));
        }

        tokens.add(new SqlToken(tokenType, token.image));
    }
}
