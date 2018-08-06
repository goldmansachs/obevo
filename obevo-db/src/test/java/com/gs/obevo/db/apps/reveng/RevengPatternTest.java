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
package com.gs.obevo.db.apps.reveng;

import com.gs.obevo.db.apps.reveng.AbstractDdlReveng.NamePatternType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng.RevengPattern;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng.RevengPatternOutput;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class RevengPatternTest {
    private static final String NA = "n/a";

    private final RevengPattern subschemaPattern = new RevengPattern(NA, NamePatternType.THREE, "pat " + AbstractDdlReveng.getCatalogSchemaObjectPattern("", ""), 1, null, NA);
    private final RevengPattern schemaPattern = new RevengPattern(NA, NamePatternType.TWO, "pat " + AbstractDdlReveng.getSchemaObjectPattern("", ""), 1, null, NA);

    @Test
    public void testFullSubschema() {
        RevengPatternOutput output = subschemaPattern.evaluate("pat A.B.C");
        assertThat(output.getSchema(), equalTo("A"));
        assertThat(output.getSubSchema(), equalTo("B"));
        assertThat(output.getPrimaryName(), equalTo("C"));
    }

    @Test
    public void testPartialSubschema() {
        RevengPatternOutput output = subschemaPattern.evaluate("pat B.C");
        assertThat(output.getSchema(), equalTo(null));
        assertThat(output.getSubSchema(), equalTo("B"));
        assertThat(output.getPrimaryName(), equalTo("C"));
    }

    @Test
    public void testBlankSubschema() {
        RevengPatternOutput output = subschemaPattern.evaluate("pat C");
        assertThat(output.getSchema(), equalTo(null));
        assertThat(output.getSubSchema(), equalTo(null));
        assertThat(output.getPrimaryName(), equalTo("C"));
    }

    @Test
    public void testFullSchema() {
        RevengPatternOutput output = schemaPattern.evaluate("pat A.B");
        assertThat(output.getSchema(), equalTo("A"));
        assertThat(output.getSubSchema(), equalTo(null));
        assertThat(output.getPrimaryName(), equalTo("B"));
    }

    @Test
    public void testBlankSchema() {
        RevengPatternOutput output = schemaPattern.evaluate("pat B");
        assertThat(output.getSchema(), equalTo(null));
        assertThat(output.getSubSchema(), equalTo(null));
        assertThat(output.getPrimaryName(), equalTo("B"));
    }
}
