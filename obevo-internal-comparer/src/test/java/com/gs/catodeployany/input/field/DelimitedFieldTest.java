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
package com.gs.catodeployany.input.field;

import org.junit.Assert;
import org.junit.Test;

public class DelimitedFieldTest {

    final DelimitedField field1 = new DelimitedField("Field", ",");
    final DelimitedField field2 = new DelimitedField("Field", "|");

    @Test
    public void testGetValue() throws Exception {

        Assert.assertEquals("a,b,cde", this.field1.getValue("cde,b,a"));
        org.junit.Assert.assertEquals("a,b,c", this.field1.getValue("    c  ,  b   , a   "));
        org.junit.Assert.assertEquals("abc", this.field1.getValue("abc"));
        org.junit.Assert.assertEquals("", this.field1.getValue(""));

        org.junit.Assert.assertEquals("a|b|c", this.field2.getValue("c|b|a"));
        org.junit.Assert.assertEquals("a|b|c", this.field2.getValue("    c  | b  | a   "));
        org.junit.Assert.assertEquals("a", this.field2.getValue("a"));
    }
}
