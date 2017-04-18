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
package com.gs.catodeployany.data.simple;

import java.util.Arrays;

import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleDataObjectTest {

    static SimpleDataObject obj;
    static SimpleDataObject obj2;
    static final SimpleDataSchema schema = TestUtil.createSchema();

    @BeforeClass
    public static void setUp() {
        obj = schema.createDataObject();
        obj2 = schema.createDataObject();

        obj.setValue("a", 123);

        obj2.setValue("x", "5");
        obj2.setValue("y", "q");

        obj.setValue("b", true);
        obj.setValue("c", "test string");
        obj.setValue("d", null);
        obj.setValue("e", Arrays.asList(1, 2, 3));
    }

    @Test
    public void getSchemaTest() {
        Assert.assertSame(schema, obj.getSchema());
        Assert.assertSame(schema, obj2.getSchema());
    }

    @Test
    public void setValueTest() {

        Assert.assertEquals(7, obj.getFields().size());
        Assert.assertEquals(7, obj2.getFields().size());
        Assert.assertNull(obj.getValue("x"));
        Assert.assertNull(obj.getValue("axax"));
        Assert.assertNull(obj2.getValue("a"));
        Assert.assertNull(obj2.getValue("b"));
        Assert.assertNull(obj2.getValue("e"));
    }

    @Test
    public void containsFieldTest() {
        Assert.assertTrue(obj.getFields().contains("a"));
        Assert.assertTrue(obj.getFields().contains("d"));
        Assert.assertFalse(obj.getFields().contains("f"));
    }

    @Test
    public void getValueTest() {
        Assert.assertEquals(123, obj.getValue("a"));
        Assert.assertEquals(true, obj.getValue("b"));
        Assert.assertEquals("test string", obj.getValue("c"));
        Assert.assertNull(obj.getValue("d"));
        Assert.assertEquals(Arrays.asList(1, 2, 3), obj.getValue("e"));
        Assert.assertNull(obj.getValue("f"));
    }

    @Test
    public void toStringTest() {
        try {
            Assert.assertEquals(
                    "SimpleDataObject {}",
                    TestUtil.createDataObjectWithKeys().toString());

            Assert.assertEquals(
                    "SimpleDataObject {Key 1=A [String], Key 2=4 [Integer], Val 1=1.4 [Double], Val 2=str [String], Val 3=[1, 2, 3] [ArrayList]}",
                    TestUtil.createDataObject("A", 4, 1.4, "str", Arrays.asList(1, 2, 3)).toString());

            Assert.assertEquals(
                    "SimpleDataObject {Key 1=A [String], Key 2=null, Val 1=1.4 [Double], Val 2=null, Val 3=[1, null, 3] [ArrayList]}",
                    TestUtil.createDataObject("A", null, 1.4, null, Arrays.asList(1, null, 3)).toString());

            Assert.assertEquals(
                    "SimpleDataObject {Key 1=null, Key 2=null, Val 1=null, Val 2=null, Val 3=[null, null, 3] [ArrayList]}",
                    TestUtil.createDataObject(null, null, null, null, Arrays.asList(null, null, 3)).toString());
        } catch (Exception ex) {
            Assert.fail();
        }
    }
}
