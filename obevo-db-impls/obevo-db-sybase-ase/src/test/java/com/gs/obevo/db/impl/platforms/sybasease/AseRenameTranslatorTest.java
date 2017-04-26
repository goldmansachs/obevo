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
package com.gs.obevo.db.impl.platforms.sybasease;

import org.junit.Test;

import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;

public class AseRenameTranslatorTest {
    private final AseRenameTranslator translator = new AseRenameTranslator();

    @Test
    public void translateSpRename() throws Exception {
        assertThat(translator.handleRawFullSql("sp_rename 'mytab.mycol', 'mynewcol'", null),
                equalToIgnoringWhiteSpace("ALTER TABLE mytab ALTER COLUMN mycol RENAME TO mynewcol"));
    }

    @Test
    public void translateSpRenameWithOtherText() throws Exception {
        assertThat(translator.handleRawFullSql("[with surrounding]\t\n[text] sp_rename 'mytab.mycol', 'mynewcol' [before\n\tand after]", null),
                equalToIgnoringWhiteSpace("[with surrounding] [text] ALTER TABLE mytab ALTER COLUMN mycol RENAME TO mynewcol [before and after]"));
    }
}