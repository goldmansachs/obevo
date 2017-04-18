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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SqlTokenParserTest {
    @Test
    public void testAll() throws Exception {
        this.test("abc abcd \n /*fd\na*/sv  fd*/ \n 1234 //" +
                " \n abcdef/*dafdsfa*/abc" +
                "fdsa " +
                "\n ghi /fdaf", Arrays.asList(
                new SqlToken(SqlTokenType.TOKEN, "abc")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.TOKEN, "abcd")
                , new SqlToken(SqlTokenType.WHITESPACE, " \n ")
                , new SqlToken(SqlTokenType.COMMENT, "/*fd\na*/")
                , new SqlToken(SqlTokenType.TOKEN, "sv")
                , new SqlToken(SqlTokenType.WHITESPACE, "  ")
                , new SqlToken(SqlTokenType.TOKEN, "fd*/")
                , new SqlToken(SqlTokenType.WHITESPACE, " \n ")
                , new SqlToken(SqlTokenType.TOKEN, "1234")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.COMMENT, "// \n")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.TOKEN, "abcdef")
                , new SqlToken(SqlTokenType.COMMENT, "/*dafdsfa*/")
                , new SqlToken(SqlTokenType.TOKEN, "abcfdsa")
                , new SqlToken(SqlTokenType.WHITESPACE, " \n ")
                , new SqlToken(SqlTokenType.TOKEN, "ghi")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.TOKEN, "/fdaf")

        ));
    }

    @Test
    public void testWithSingleQuoteString() throws Exception {
        this.test("abc '  /*     '   */", Arrays.asList(
                new SqlToken(SqlTokenType.TOKEN, "abc")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.STRING, "'  /*     '")
                , new SqlToken(SqlTokenType.WHITESPACE, "   ")
                , new SqlToken(SqlTokenType.TOKEN, "*/")
        ));
    }

    @Test
    public void testWithDoubleQuoteString() throws Exception {
        this.test("abc \"  /*   '  \" '  '  */", Arrays.asList(
                new SqlToken(SqlTokenType.TOKEN, "abc")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.STRING, "\"  /*   '  \"")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.STRING, "'  '")
                , new SqlToken(SqlTokenType.WHITESPACE, "  ")
                , new SqlToken(SqlTokenType.TOKEN, "*/")
        ));
    }

    @Test
    public void testEscapedQuote() throws Exception {
        this.test("abc '  f ''  f ' abcd", Arrays.asList(
                new SqlToken(SqlTokenType.TOKEN, "abc")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.STRING, "'  f ''  f '")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.TOKEN, "abcd")
        ));
    }

    @Test
    public void testEscapedQuote2() throws Exception {
        this.test("abc '  f ''  '' f ' '  f ''  '' f '", Arrays.asList(
                new SqlToken(SqlTokenType.TOKEN, "abc")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.STRING, "'  f ''  '' f '")
                , new SqlToken(SqlTokenType.WHITESPACE, " ")
                , new SqlToken(SqlTokenType.STRING, "'  f ''  '' f '")
        ));
    }

    @Test
    public void testEndingLineComment() throws Exception {
        this.test("hello  // ended", Arrays.asList(
                new SqlToken(SqlTokenType.TOKEN, "hello")
                , new SqlToken(SqlTokenType.WHITESPACE, "  ")
                , new SqlToken(SqlTokenType.COMMENT, "// ended")
        ));
    }

    private void test(String input, List<SqlToken> expectedTokens) {
        List<SqlToken> actualTokens = new SqlTokenParser().parseTokens(input);

        assertEquals(expectedTokens.size(), actualTokens.size());
        for (int i = 0; i < expectedTokens.size(); i++) {
            SqlToken expectedToken = expectedTokens.get(i);
            SqlToken actualToken = actualTokens.get(i);

            assertEquals("Failed at " + i + "; " + expectedToken, expectedToken.getText(), actualToken.getText());
            assertEquals("Failed at " + i + "; " + expectedToken, expectedToken.getTokenType(), actualToken.getTokenType());
        }
    }
}
