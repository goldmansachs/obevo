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
package com.gs.obevocomparer.input.field;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class DateFieldTest {

    private final DateField field = new DateField("Date Str", "New Date", "MM-dd-yyyy");

    @Test
    public void testGetValue() throws Exception {
        Assert.assertEquals("not a date", this.field.getValue("not a date"));
        org.junit.Assert.assertEquals(Date.class, this.field.getValue("05-12-2010").getClass());
        org.junit.Assert.assertEquals(new SimpleDateFormat("MM-dd-yyyy").parse("05-12-2010"), this.field.getValue("05-12-2010"));
    }
}
