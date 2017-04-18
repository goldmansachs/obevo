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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SimpleDataSchemaTest {

    SimpleDataSchema schema = new SimpleDataSchema();

    @Test
    public void testConstructor() {
        Assert.assertEquals(0, this.schema.getMappedFields().size());

        Map<String, String> mappedFields = new HashMap<String, String>();
        mappedFields.put("Test1", "Test1 Ref");
        mappedFields.put("Test2", "Test2 Ref");
        this.schema = new SimpleDataSchema(mappedFields);

        Assert.assertEquals(2, this.schema.getMappedFields().size());
    }

    @Test
    public void testFields() {
        Assert.assertTrue(this.schema.getFields().isEmpty());

        Assert.assertNull(this.schema.getFieldIndex("Field 1"));
        Assert.assertTrue(this.schema.getFields().isEmpty());

        Assert.assertEquals((Integer) 0, this.schema.getOrCreateFieldIndex("Field 1"));
        Assert.assertEquals((Integer) 1, this.schema.getOrCreateFieldIndex("Field 2"));
        Assert.assertEquals((Integer) 2, this.schema.getOrCreateFieldIndex("Field 3"));

        Assert.assertEquals(3, this.schema.getFields().size());

        Assert.assertEquals((Integer) 0, this.schema.getFieldIndex("Field 1"));
        Assert.assertEquals((Integer) 1, this.schema.getFieldIndex("Field 2"));
        Assert.assertEquals((Integer) 2, this.schema.getFieldIndex("Field 3"));
    }

    @Test
    public void testCreateDataObject() {
        SimpleDataObject obj = this.schema.createDataObject();

        Assert.assertNotNull(obj);
        Assert.assertSame(this.schema, obj.getSchema());
    }
}
