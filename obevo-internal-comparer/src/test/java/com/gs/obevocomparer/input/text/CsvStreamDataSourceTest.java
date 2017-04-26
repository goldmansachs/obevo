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
package com.gs.obevocomparer.input.text;

import java.io.FileReader;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.simple.SimpleDataSchema;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class CsvStreamDataSourceTest {

    @Test
    public void testDataSource() throws Exception {

        CsvStreamDataSource source = new CsvStreamDataSource("Test", new FileReader("src/test/resources/csv-file.txt"));

        source.setDataSchema(new SimpleDataSchema());
        source.open();

        CatoDataObject obj = source.next();

        Assert.assertEquals(1, obj.getValue("a"));
        Assert.assertEquals("a", obj.getValue("b"));
        assertNull(obj.getValue("c"));
        Assert.assertEquals(45.6, obj.getValue("d"));
        Assert.assertEquals("value", obj.getValue("e"));

        obj = source.next();

        Assert.assertEquals("value2", obj.getValue("a"));
        Assert.assertEquals("val,val", obj.getValue("b"));
        assertNull(obj.getValue("c"));
        Assert.assertEquals("rrr", obj.getValue("d"));
        Assert.assertEquals("\"quoted\"", obj.getValue("e"));

        source.close();
    }
}
