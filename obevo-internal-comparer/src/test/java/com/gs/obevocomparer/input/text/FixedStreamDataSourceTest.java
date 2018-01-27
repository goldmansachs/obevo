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
package com.gs.obevocomparer.input.text;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class FixedStreamDataSourceTest {

    private FixedStreamDataSource dataSource;

    @Test
    public void parseFieldsTest() {
        try {
            this.dataSource = new FixedStreamDataSource(
                    "Test", new StringReader(TEST_FILE),
                    "Num", 0, 3, "String", 3, 7, "Test", 8, 12, "Digit", 14, 19);
            this.dataSource.setTypeConverter(null);
            List<CatoDataObject> data = TestUtil.getData(this.dataSource);

            Assert.assertEquals(3, data.size());
            CatoDataObject obj1 = data.get(0);
            CatoDataObject obj2 = data.get(1);
            CatoDataObject obj3 = data.get(2);

            Assert.assertEquals(4, obj1.getFields().size());
            Assert.assertEquals(4, obj2.getFields().size());
            Assert.assertEquals(4, obj3.getFields().size());
            TestUtil.assertEquals(Arrays.asList("Num", "String", "Test", "Digit"), obj1.getFields());
            TestUtil.assertEquals(Arrays.asList("Num", "String", "Test", "Digit"), obj2.getFields());
            TestUtil.assertEquals(Arrays.asList("Num", "String", "Test", "Digit"), obj3.getFields());

            Assert.assertEquals("123", obj1.getValue("Num"));
            Assert.assertEquals("456", obj2.getValue("Num"));
            Assert.assertEquals("789", obj3.getValue("Num"));

            Assert.assertEquals("123", obj1.getValue("Num"));
            Assert.assertEquals("456", obj2.getValue("Num"));
            Assert.assertEquals("789", obj3.getValue("Num"));

            Assert.assertEquals("abcd", obj1.getValue("String"));
            Assert.assertEquals("efgh", obj2.getValue("String"));
            Assert.assertEquals("ijkl", obj3.getValue("String"));

            Assert.assertEquals("test", obj1.getValue("Test"));
            Assert.assertEquals("test", obj2.getValue("Test"));
            Assert.assertEquals("tsst", obj3.getValue("Test"));

            Assert.assertEquals("023", obj1.getValue("Digit"));
            Assert.assertEquals(" 23", obj2.getValue("Digit"));
            Assert.assertEquals("  3", obj3.getValue("Digit"));
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    private static final String TEST_FILE =
            "123abcd test  023\n" +
                    "456efgh test   23\n" +
                    "789ijkl tsst    3\n";
}
