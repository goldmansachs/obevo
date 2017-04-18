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
package com.gs.catodeployany.input.text;

import java.io.StringReader;
import java.util.List;

import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.util.TestUtil;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

public class DelimitedStreamDataSourceTest {

    @Test
    public void testDelim() {
        TestUtil.clearLogged();
        DelimitedStreamDataSource dataSource = new DelimitedStreamDataSource("Test", new StringReader(TEST_FILE), "|");

        Assert.assertEquals("|", dataSource.getDelimiter());
        TestUtil.assertLogged(Level.WARN,
                "The delimiter is a regex, are you sure you want to use \"|\" instead of \"\\|\"?");
    }

    @Test
    public void testFetchData() {
        try {
            DelimitedStreamDataSource dataSource = new DelimitedStreamDataSource(
                    "Test", new StringReader(TEST_FILE), ",");
            dataSource.setTypeConverter(null);

            Assert.assertEquals(",", dataSource.getDelimiter());

            List<CatoDataObject> data = TestUtil.getData(dataSource);

            Assert.assertEquals(3, data.size());

            CatoDataObject obj1 = data.get(0);
            CatoDataObject obj2 = data.get(1);
            CatoDataObject obj3 = data.get(2);

            Assert.assertEquals("123", obj1.getValue("Num"));
            Assert.assertEquals("456", obj2.getValue("Num"));
            Assert.assertEquals("2", obj3.getValue("Num"));

            Assert.assertEquals("Test", obj1.getValue("String"));
            Assert.assertEquals("Test2", obj2.getValue("String"));
            Assert.assertEquals("Test3  ", obj3.getValue("String"));

            Assert.assertEquals("Hello", obj1.getValue("Other"));
            Assert.assertEquals("This is a long string", obj2.getValue("Other"));
            Assert.assertEquals("Other Test", obj3.getValue("Other"));
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    private static final String TEST_FILE =
            "Num, String    ,Other\n" +
                    "123,Test,Hello\n" +
                    "456,Test2,This is a long string\n" +
                    "2,Test3  ,Other Test\n";
}
