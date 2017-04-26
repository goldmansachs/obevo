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
package com.gs.obevo.util.hash;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OldWhitespaceAgnosticDbChangeHashStrategyTest {
    private final OldWhitespaceAgnosticDbChangeHashStrategy hashStrategy = new OldWhitespaceAgnosticDbChangeHashStrategy();

    @Test
    public void test() {
        this.testString("", "d41d8cd98f00b204e9800998ecf8427e", "\n      \n     \t        \t\t\t\r\r\n\n");
        this.testString("", "d41d8cd98f00b204e9800998ecf8427e", "\t\t         \t\n        \t   \t\t     \r\r\n\n \n  ");
        this.testString("abc def ghi j k l", "ffe80aa68532a9cc91bcd28e5e3e512e",
                "\n  abc    \n     \t    def    \tghi\t\t\rj\rk\nl\n");
        this.testString("abc def ghi j k l", "ffe80aa68532a9cc91bcd28e5e3e512e",
                "          \n  abc    \n     \t     \n\n\r  def    \tghi\t\t\rj\r    k    \nl\n");
    }

    private void testString(String expectedConversion, String expectedHash, String inputString) {
        assertEquals(expectedConversion, this.hashStrategy.normalizeFileContentsForHashing(inputString));
        assertEquals(expectedHash, this.hashStrategy.hashContent(inputString));
    }
}
