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
package com.gs.obevo.db.impl.platforms.sqltranslator.impl;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class DefaultUnparsedSqlTranslatorTest {
    private final DefaultUnparsedSqlTranslator translator = new DefaultUnparsedSqlTranslator();

    @Test
    public void testRemoveGrantGrant() {
        assertThat(this.translator.handleRawFullSql("GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE_C TO GROUP GROUP1", null),
                Matchers.equalToIgnoringWhiteSpace(""));
    }

    @Test
    public void testCommentGrantFalsePositive() {
        assertThat(this.translator.handleRawFullSql("blah blah 'SILVER GRANT' blah hello world", null),
                Matchers.equalToIgnoringWhiteSpace("blah blah 'SILVER GRANT' blah hello world"));
    }
}