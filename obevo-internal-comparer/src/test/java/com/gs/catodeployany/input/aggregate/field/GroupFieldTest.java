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
package com.gs.catodeployany.input.aggregate.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class GroupFieldTest {

    private GroupField field;

    @Test
    public void testConstructor() {
        this.field = new GroupField("My Name", "Base Data");
        Assert.assertEquals("My Name", this.field.getName());
        Assert.assertEquals("Base Data", this.field.getBaseField());

        this.field = new GroupField("My Name");
        Assert.assertEquals("My Name", this.field.getName());
        Assert.assertEquals("My Name", this.field.getBaseField());
    }

    @Test
    public void testGetValue() {
        List<CatoDataObject> objs = new ArrayList<CatoDataObject>();
        objs.add(TestUtil.createDataObjectWithKeys("Val 1", null, "Val 2", 13));
        objs.add(TestUtil.createDataObjectWithKeys("Val 1", -4, "Val 2", 14));
        objs.add(TestUtil.createDataObjectWithKeys("Val 1", "abc", "Val 2", 15));
        objs.add(TestUtil.createDataObjectWithKeys("Val 1", "def", "Val 2", 15));
        objs.add(TestUtil.createDataObjectWithKeys("Val 1", "abc", "Val 2", 15));
        objs.add(TestUtil.createDataObjectWithKeys("Val 1", null, "Val 2", null));

        this.field = new GroupField("Val 1");

        Set<Object> expectedValue = new LinkedHashSet<Object>();
        expectedValue.addAll(Arrays.<Object>asList(null, -4, "abc", "def"));
        Set<Object> actualValue = this.field.getValue(objs);

        Assert.assertEquals(4, actualValue.size());
        TestUtil.assertEquals(expectedValue, actualValue);
    }
}
