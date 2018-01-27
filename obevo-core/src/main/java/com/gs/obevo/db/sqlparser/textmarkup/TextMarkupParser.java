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
package com.gs.obevo.db.sqlparser.textmarkup;

import java.io.StringReader;
import java.util.List;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Utility to split the metadata line into tokens.
 */
public class TextMarkupParser {
    public static List<Token> parseTokens(String line) {
        TextMarkupLineSyntaxParser parser = new TextMarkupLineSyntaxParser(new StringReader(line));

        MutableList<Token> tokens = Lists.mutable.empty();
        for (Token token = parser.getNextToken(); token.kind != TextMarkupLineSyntaxParserConstants.EOF; token = parser.getNextToken()) {
            tokens.add(token);
        }
        return tokens;
    }
}
