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
package com.gs.obevocomparer.input.field;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class DoubleFieldTest {

    @Test
    public void constructorTest() {
        new DoubleField("Field");
        new DoubleField("Field 1", "Field 2");
        new DoubleField("Field", true);
        new DoubleField("Field 1", "Field 2", true);
    }

    @Test
    public void doubleFieldTest() {

        DoubleField field1 = new DoubleField("Field", true);

        Assert.assertEquals("Field", field1.getName());
        assertNull(field1.getValue(""));
        assertNull(field1.getValue("   	"));

        Assert.assertEquals(5.0, field1.getValue("5"));
        Assert.assertEquals(3.14, field1.getValue("3.14"));
        Assert.assertEquals(500.0, field1.getValue("5E2"));

        try {
            field1.getValue("abc");
            Assert.fail();
        } catch (Exception ex) {
        }

        DoubleField field2 = new DoubleField("Field", false);

        try {
            field2.getValue("   ");
            org.junit.Assert.fail();
        } catch (Exception ex) {
        }
    }
}
