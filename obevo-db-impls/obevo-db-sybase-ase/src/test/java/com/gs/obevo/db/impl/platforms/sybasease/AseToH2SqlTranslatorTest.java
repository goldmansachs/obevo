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
package com.gs.obevo.db.impl.platforms.sybasease;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AseToH2SqlTranslatorTest {
    private final AseToH2SqlTranslator translator = new AseToH2SqlTranslator();

    @Test
    public void testPostColumnIdentity() throws Exception {
        assertEquals("AUTO_INCREMENT NOT NULL",
                this.translator.handlePostColumnText("IDENTITY NOT NULL", null, null));
        assertEquals("  AUTO_INCREMENT NOT NULL",
                this.translator.handlePostColumnText("  IDENTITY NOT NULL", null, null));
        assertEquals("sometext  AUTO_INCREMENT NOT NULL",
                this.translator.handlePostColumnText("sometext  IDENTITY NOT NULL", null, null));
    }

    @Test
    public void testNotNullToSetNotNullTranslation() {
        /*transform*/
        assertEquals("Should insert \"SET\" keyword", "ALTER  TABLE  some_Table  ALTER   COLUMN  abcCol   SET NOT   NULL",
                this.translator.handleAnySqlPostTranslation("ALTER  TABLE  some_Table  ALTER   COLUMN  abcCol   NOT   NULL", null));
        assertEquals("Should insert \"SET\" keyword", "ALTER  TABLE  some_Table  ALTER   COLUMN  abcCol SET NULL",
                this.translator.handleAnySqlPostTranslation("ALTER  TABLE  some_Table  ALTER   COLUMN  abcCol NULL", null));
        /*ignore*/
        assertEquals("Should not touch", "ALTER foo some_Table ALTER COLUMN abcCol NOT NULL",
                this.translator.handleAnySqlPostTranslation("ALTER foo some_Table ALTER COLUMN abcCol NOT NULL", null));
        assertEquals("Should not touch", "ALTER TABLE some_Table ALTER FOO abcCol NOT NULL",
                this.translator.handleAnySqlPostTranslation("ALTER TABLE some_Table ALTER FOO abcCol NOT NULL", null));
        assertEquals("Should not touch", "ALTER TABLE some_Table ALTER COLUMN abcCol FoO NULL",
                this.translator.handleAnySqlPostTranslation("ALTER TABLE some_Table ALTER COLUMN abcCol FoO NULL", null));
        assertEquals("Should not touch", "ALTER TABLE some_Table ALTER COLUMN abcCol SET blahh",
                this.translator.handleAnySqlPostTranslation("ALTER TABLE some_Table ALTER COLUMN abcCol SET blahh", null));
    }
}