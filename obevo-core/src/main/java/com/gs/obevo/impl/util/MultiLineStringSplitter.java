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
package com.gs.obevo.impl.util;

import java.util.regex.Pattern;

import com.gs.obevo.db.sqlparser.tokenparser.SqlToken;
import com.gs.obevo.db.sqlparser.tokenparser.SqlTokenParser;
import com.gs.obevo.db.sqlparser.tokenparser.SqlTokenType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class MultiLineStringSplitter implements Function<String, MutableList<String>> {
    public static MultiLineStringSplitter createSplitterOnSpaceAndLine(String token) {
        return new MultiLineStringSplitter("[^\\S\\n]*" + token + "[^\\S\\n]*");
    }

    private final String splitToken;
    private final boolean splitOnWholeLine;

    public MultiLineStringSplitter(String splitToken) {
        this(splitToken, true);
    }

    public MultiLineStringSplitter(String splitToken, boolean splitOnWholeLine) {
        this.splitToken = splitToken;
        this.splitOnWholeLine = splitOnWholeLine;
    }

    private final SqlTokenParser tokenParser = new SqlTokenParser();

    private MutableList<SqlToken> collapseWhiteSpaceAndTokens(MutableList<SqlToken> sqlTokens) {
        MutableList<SqlToken> collapsedTokens = Lists.mutable.empty();

        String curText = "";
        boolean inText = false;
        for (SqlToken sqlToken : sqlTokens) {
            switch (sqlToken.getTokenType()) {
            case STRING:
            case COMMENT:
                if (inText) {
                    collapsedTokens.add(new SqlToken(SqlTokenType.TOKEN, curText));
                    curText = "";
                }
                inText = false;
                collapsedTokens.add(sqlToken);
                break;
            case TOKEN:
            case WHITESPACE:
                inText = true;
                curText += sqlToken.getText();
                break;
            default:
                throw new IllegalArgumentException("Not expecting this enum type: " + sqlToken.getTokenType());
            }
        }
        if (inText) {
            collapsedTokens.add(new SqlToken(SqlTokenType.TOKEN, curText));
        }

        return collapsedTokens;
    }

    @Override
    public MutableList<String> valueOf(String inputString) {
        inputString += "\n";  // add sentinel to facilitate line split

        MutableList<SqlToken> sqlTokens = this.tokenParser.parseTokens(inputString);
        sqlTokens = this.collapseWhiteSpaceAndTokens(sqlTokens);

        MutableList<String> finalSplitStrings = Lists.mutable.empty();
        String currentSql = "";

        for (SqlToken sqlToken : sqlTokens) {
            if (sqlToken.getTokenType() == SqlTokenType.COMMENT || sqlToken.getTokenType() == SqlTokenType.STRING) {
                currentSql += sqlToken.getText();
            } else {
                String pattern = splitOnWholeLine ? "(?i)^" + this.splitToken + "$" : this.splitToken;
                MutableList<String> splitStrings =
                        Lists.mutable.with(Pattern.compile(pattern, Pattern.MULTILINE).split(sqlToken.getText()));
                if (splitStrings.isEmpty()) {
                    // means that we exactly match
                    finalSplitStrings.add(currentSql);
                    currentSql = "";
                } else if (splitStrings.size() == 1) {
                    currentSql += sqlToken.getText();
                } else {
                    splitStrings.set(0, currentSql + splitStrings.get(0));

                    if (splitOnWholeLine) {
                        if (splitStrings.size() > 1) {
                            splitStrings.set(0, StringUtils.chomp(splitStrings.get(0)));
                            for (int i = 1; i < splitStrings.size(); i++) {
                                String newSql = splitStrings.get(i);
                                if (newSql.startsWith("\n")) {
                                    newSql = newSql.replaceFirst("^\n", "");
                                } else if (newSql.startsWith("\r\n")) {
                                    newSql = newSql.replaceFirst("^\r\n", "");
                                }

                                // Chomping the end of each sql due to the split of the GO statement
                                if (i < splitStrings.size() - 1) {
                                    newSql = StringUtils.chomp(newSql);
                                }
                                splitStrings.set(i, newSql);
                            }
                        }
                    }

                    finalSplitStrings.addAll(splitStrings.subList(0, splitStrings.size() - 1));
                    currentSql = splitStrings.getLast();
                }
            }
        }

        if (!currentSql.isEmpty()) {
            finalSplitStrings.add(currentSql);
        }

        // accounting for the sentinel
        if (finalSplitStrings.getLast().isEmpty()) {
            finalSplitStrings.remove(finalSplitStrings.size() - 1);
        } else {
            finalSplitStrings.set(finalSplitStrings.size() - 1, StringUtils.chomp(finalSplitStrings.getLast()));
        }

        return finalSplitStrings;
    }
}
