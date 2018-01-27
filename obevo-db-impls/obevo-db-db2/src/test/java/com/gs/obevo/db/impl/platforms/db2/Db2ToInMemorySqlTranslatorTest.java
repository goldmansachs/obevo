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
package com.gs.obevo.db.impl.platforms.db2;

import org.junit.Test;

import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class Db2ToInMemorySqlTranslatorTest {
    private final Db2ToInMemorySqlTranslator translator = new Db2ToInMemorySqlTranslator();

    @Test
    public void testremoveProcCalls() {
        assertThat(this.translator.handleRawFullSql(
                " call   sysproc.admin_cmd   (   'reorg table calc_result_future_merlin_detail' )", null),
                equalToIgnoringWhiteSpace(""));
    }

    @Test
    public void testLoggedRemoval() throws Exception {
        assertEquals("  NOT NULL", this.translator.handlePostColumnText("LOGGED NOT NULL", null, null));
        assertEquals("  NOT NULL", this.translator.handlePostColumnText("NOT LOGGED NOT NULL", null, null));
        assertEquals("preText   NOT NULL", this.translator.handlePostColumnText("preText LOGGED NOT NULL", null, null));
        assertEquals("preText   NOT NULL", this.translator.handlePostColumnText("preText NOT LOGGED NOT NULL", null, null));

        // word boundary does not match here, so the text should remain untouched
        assertEquals("preText bLOGGED NOT NULL", this.translator.handlePostColumnText("preText bLOGGED NOT NULL", null, null));
        assertEquals("preText NOT LOGGEDb NOT NULL", this.translator.handlePostColumnText("preText NOT LOGGEDb NOT NULL", null, null));
    }

    @Test
    public void testCompactRemoval() throws Exception {
        assertEquals("  NOT NULL", this.translator.handlePostColumnText("COMPACT NOT NULL", null, null));
        assertEquals("  NOT NULL", this.translator.handlePostColumnText("NOT COMPACT NOT NULL", null, null));
        assertEquals("preText   NOT NULL", this.translator.handlePostColumnText("preText COMPACT NOT NULL", null, null));
        assertEquals("preText   NOT NULL", this.translator.handlePostColumnText("preText NOT COMPACT NOT NULL", null, null));

        // word boundary does not match here, so the text should remain untouched
        assertEquals("preText bCOMPACT NOT NULL", this.translator.handlePostColumnText("preText bCOMPACT NOT NULL", null, null));
        assertEquals("preText NOT COMPACTb NOT NULL", this.translator.handlePostColumnText("preText NOT COMPACTb NOT " +
                "NULL", null, null));
    }
}