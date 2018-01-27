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

import java.sql.Timestamp;
import java.util.Date;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class AbstractDerivedFieldTest {
    private static final AbstractDerivedField<String> field1 =
            new AbstractDerivedField<String>(TestUtil.KEY_FIELDS.get(0), "New Test", String.class) {
                protected Object getValue(String sourceValue) {
                    return sourceValue.concat(" decorated");
                }
            };

    private static final AbstractDerivedField<Date> field2 =
            new AbstractDerivedField<Date>(TestUtil.ATTR_FIELDS.get(0), "New Test 2", Date.class) {
                protected Object getValue(Date sourceValue) {
                    return sourceValue.toString().concat(" decorated");
                }
            };

    @Test
    public void testAbstractDerivedField() {
        Assert.assertEquals("New Test", field1.getName());

        CatoDataObject obj1 = TestUtil.createDataObject("A", "B", 1, 2, 3);
        Assert.assertEquals("A decorated", field1.getValue(obj1));

        CatoDataObject obj2 = TestUtil.createDataObject(null, "B", 1, 2, 3);
        assertNull(field1.getValue(obj2));

        CatoDataObject obj3 = TestUtil.createDataObject("A", "B", 1, 2, 3);
        org.junit.Assert.assertEquals(1, field2.getValue(obj3));

        CatoDataObject obj4 = TestUtil.createDataObject("A", "B", new Date(), 2, 3);
        org.junit.Assert.assertEquals(String.class, field2.getValue(obj4).getClass());

        CatoDataObject obj5 = TestUtil.createDataObject("A", "B", new Timestamp(1000), 2, 3);
        org.junit.Assert.assertEquals(String.class, field2.getValue(obj5).getClass());

        CatoDataObject obj6 = TestUtil.createDataObject("A", "B", true, 2, 3);
        org.junit.Assert.assertEquals(true, field2.getValue(obj6));

        CatoDataObject obj7 = TestUtil.createDataObject("A", "B", new Object(), 2, 3);
        org.junit.Assert.assertEquals(Object.class, field2.getValue(obj7).getClass());
    }
}
