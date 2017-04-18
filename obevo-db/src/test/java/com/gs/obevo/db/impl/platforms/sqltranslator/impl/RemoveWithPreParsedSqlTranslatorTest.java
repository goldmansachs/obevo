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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoveWithPreParsedSqlTranslatorTest {
    private final RemoveWithPreParsedSqlTranslator translator = new RemoveWithPreParsedSqlTranslator();

    @Test
    public void testPreprocessSql() throws Exception {
        assertEquals(
                "\tPRIMARY KEY (idField)\n" +
                        "\n" +
                        ") LOCK DATAROWS\n",
                this.translator.preprocessSql(
                        "\tPRIMARY KEY (idField)\n" +
                                "\tWITH max_rows_per_page = 0, reservepagegap = 0\n" +
                                ") LOCK DATAROWS\n"
                ));

        assertEquals(
                "\tPRIMARY KEY (idField)\n" +
                        "\tSOMETHINGBEFORE_DONTREMOVE WITH max_rows_per_page = 0, reservepagegap = 0\n" +
                        ") LOCK DATAROWS\n"
                ,
                this.translator.preprocessSql(
                        "\tPRIMARY KEY (idField)\n" +
                                "\tSOMETHINGBEFORE_DONTREMOVE WITH max_rows_per_page = 0, reservepagegap = 0\n" +
                                ") LOCK DATAROWS\n"
                ));
    }
}