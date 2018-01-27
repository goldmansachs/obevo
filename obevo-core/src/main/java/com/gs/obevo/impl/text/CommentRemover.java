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
package com.gs.obevo.impl.text;

import com.gs.obevo.db.sqlparser.tokenparser.SqlToken;
import com.gs.obevo.db.sqlparser.tokenparser.SqlTokenParser;
import com.gs.obevo.db.sqlparser.tokenparser.SqlTokenType;
import com.gs.obevo.db.sqlparser.tokenparser.TokenMgrError;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to remove Java-style comments from the input text (i.e. single-line // or block comments).
 */
public class CommentRemover {
    private static final Logger LOG = LoggerFactory.getLogger(CommentRemover.class);

    /**
     * Removes Java-style comments from the input text (i.e. single-line // or block comments).
     */
    public static String removeComments(String content, String logMessage) {
        try {
            MutableList<SqlToken> sqlTokens = new SqlTokenParser().parseTokens(content);
            MutableList<SqlToken> nonCommentTokens = sqlTokens.reject(Predicates.attributeEqual(SqlToken::getTokenType,
                    SqlTokenType.COMMENT));

            // makeString returns a space due to the current kludge that the SqlTokenParser also stripes newline from
            // line comments
            return nonCommentTokens.collect(SqlToken.TO_TEXT).makeString(" ");
        } catch (TokenMgrError e) {
            // javacc will throw parsing exceptions as a java.lang.Error (!). We will catch this regardless
            LOG.warn("Error in removing comments from [{}] due to a parsing error (possibly in quote parsing or invalid characters); will default to returning the original string", logMessage, e);
            return content;
        } catch (Exception e) {
            // Let's have a catchall for all exceptions to return the original string if comment parsing fails anyway.
            LOG.warn("Error in removing comments from [{}] due to a parsing error (possibly in quote parsing or invalid characters); will default to returning the original string", logMessage, e);
            return content;
        }
    }
}
