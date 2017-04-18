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
package com.gs.catodeployany.compare.breaks;

import java.util.HashMap;
import java.util.Map;

import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldBreakTest {

    static FieldBreak br;

    static Map<String, Object> fieldBreaks;
    static CatoDataObject obj;

    @BeforeClass
    public static void setUp() {
        obj = TestUtil.createDataObject(1, 2, "a", "b", "c", "d");

        fieldBreaks = new HashMap<String, Object>();
        for (int i = 0; i < 2; i++) {
            fieldBreaks.put(TestUtil.ATTR_FIELDS.get(i), i);
        }

        br = new FieldBreak(obj, fieldBreaks);
    }

    @Test
    public void testGetFieldBreaks() {
        Assert.assertEquals(fieldBreaks, br.getFieldBreaks());
    }

    @Test
    public void testGetFields() {
        TestUtil.assertEquals(TestUtil.ATTR_FIELDS.subList(0, 2), br.getFields());
    }

    @Test
    public void testGetExpectedValue() {
        Assert.assertEquals(0, br.getExpectedValue(TestUtil.ATTR_FIELDS.get(0)));
        Assert.assertEquals(1, br.getExpectedValue(TestUtil.ATTR_FIELDS.get(1)));
        Assert.assertNull(br.getExpectedValue(TestUtil.ATTR_FIELDS.get(2)));
        Assert.assertNull(br.getExpectedValue(TestUtil.KEY_FIELDS.get(0)));
        Assert.assertNull(br.getExpectedValue(TestUtil.KEY_FIELDS.get(1)));
    }

    @Test
    public void testGetActualValue() {
        for (String field : TestUtil.ALL_FIELDS) {
            Assert.assertEquals(obj.getValue(field), br.getActualValue(field));
        }
    }

    @Test
    public void setExcludedTest() {
        Assert.assertFalse(br.isExcluded(TestUtil.ATTR_FIELDS.get(0)));
        Assert.assertFalse(br.isExcluded(TestUtil.ATTR_FIELDS.get(1)));

        br.setExcluded(TestUtil.ATTR_FIELDS.get(0), true);
        Assert.assertTrue(br.isExcluded(TestUtil.ATTR_FIELDS.get(0)));
    }
}
