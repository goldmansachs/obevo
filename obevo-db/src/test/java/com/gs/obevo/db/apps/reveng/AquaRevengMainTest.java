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
package com.gs.obevo.db.apps.reveng;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AquaRevengMainTest {

    @Test
    public void testExtractName() {
        assertEquals("TABLE", AquaRevengMain.extractName("TABLE", null));
        assertEquals("TABLE", AquaRevengMain.extractName("TABLE", "abca"));
        assertEquals("TABLE", AquaRevengMain.extractName("TABLE", "abc{}a"));
        assertEquals("TABLE", AquaRevengMain.extractName("abcTABLEa", "abc{}a"));
        assertEquals("TABLE", AquaRevengMain.extractName("TABLEaaa", "{}aaa"));
        assertEquals("TABLE", AquaRevengMain.extractName("aaaTABLE", "aaa{}"));
    }
}
