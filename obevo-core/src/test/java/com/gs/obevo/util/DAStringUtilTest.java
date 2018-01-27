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
package com.gs.obevo.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DAStringUtilTest {

    @Test
    public void testNormalizeWhiteSpaceFromString() throws Exception {
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromString(null));
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromString("    \r\n       "));
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromString("     \r      "));
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromString(""));
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromString("           "));
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromString("     \n      "));
        assertEquals("a a", DAStringUtil.normalizeWhiteSpaceFromString("   a  \n  \n  a    "));
        assertEquals("a a", DAStringUtil.normalizeWhiteSpaceFromString(" \t  a  \n  \n \t a   \t "));
    }

    @Test
    public void testNormalizeWhiteSpaceFromStringOld() {
        assertEquals(null, DAStringUtil.normalizeWhiteSpaceFromStringOld(null));
        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld("    \r\n       "));
        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld("     \r      "));
        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld(""));
        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld("           "));
        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld("     \n      "));
        assertEquals("a a", DAStringUtil.normalizeWhiteSpaceFromStringOld("   a  \n  \n  a    "));
        assertEquals("a a", DAStringUtil.normalizeWhiteSpaceFromStringOld(" \t  a  \n  \n \t a   \t "));

        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld("\n      \n     \t        \t\t\t\r\r\n\n"));
        assertEquals("", DAStringUtil.normalizeWhiteSpaceFromStringOld("\t\t         \t\n        \t   \t\t     \r\r\n\n \n  "));
        assertEquals("abc def ghi j k l", DAStringUtil.normalizeWhiteSpaceFromStringOld("\n  abc    \n     \t    def    \tghi\t\t\rj\rk\nl\n"));
        assertEquals("abc def ghi j k l", DAStringUtil.normalizeWhiteSpaceFromStringOld("          \n  abc    \n     \t     \n\n\r  def    \tghi\t\t\rj\r    k    \nl\n"));
    }
}
