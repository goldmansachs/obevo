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

public class AliasFieldTest {

    final AliasField field = new AliasField("Source Field", "New Field");

    @Test
    public void testGetValue() throws Exception {

        Assert.assertEquals("New Field", this.field.getName());

        Assert.assertEquals("abc", this.field.getValue("abc"));
        org.junit.Assert.assertEquals(123, this.field.getValue(123));
        org.junit.Assert.assertEquals(new Integer(100), new Integer(100));
    }
}
