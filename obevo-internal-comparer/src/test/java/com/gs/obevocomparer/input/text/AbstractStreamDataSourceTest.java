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

import java.io.StringReader;

import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class AbstractStreamDataSourceTest {

    AbstractStreamDataSource dataSource;

    @Test
    public void testFetchData() {
        try {
            this.dataSource = new TestStreamDataSource(TEST_FILE);
            Assert.assertEquals(10, TestUtil.getData(this.dataSource).size());

            this.dataSource = new TestStreamDataSource(TEST_FILE);
            this.dataSource.setStripFirst(1);
            Assert.assertEquals(9, TestUtil.getData(this.dataSource).size());

            this.dataSource = new TestStreamDataSource(TEST_FILE);
            this.dataSource.setStripLast(1);
            Assert.assertEquals(9, TestUtil.getData(this.dataSource).size());

            this.dataSource = new TestStreamDataSource(TEST_FILE);
            this.dataSource.setStripFirst(1);
            this.dataSource.setStripLast(1);
            Assert.assertEquals(8, TestUtil.getData(this.dataSource).size());

            this.dataSource = new TestStreamDataSource(TEST_FILE);
            this.dataSource.setStripFirst(3);
            this.dataSource.setStripLast(1);
            Assert.assertEquals(6, TestUtil.getData(this.dataSource).size());

            this.dataSource = new TestStreamDataSource(TEST_FILE);
            this.dataSource.setStripFirst(1);
            this.dataSource.setStripLast(3);
            Assert.assertEquals(6, TestUtil.getData(this.dataSource).size());

            this.dataSource = new TestStreamDataSource(TEST_FILE);
            this.dataSource.setStripFirst(3);
            this.dataSource.setStripLast(3);
            Assert.assertEquals(4, TestUtil.getData(this.dataSource).size());
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    private static class TestStreamDataSource extends AbstractStreamDataSource {
        public TestStreamDataSource(String data) {
            super("Test", new StringReader(data));
        }

        protected String[] parseData(String line) {
            if (line == null || line.trim().length() == 0) {
                return null;
            }
            String[] fields = new String[1];
            fields[0] = line;
            return fields;
        }
    }

    private static final String TEST_FILE =
            "Row 1\n" +
                    "Row 2\n" +
                    "Row 3\n" +
                    "Row 4\n" +
                    "Row 5\n" +
                    "Row 6\n" +
                    "Row 7\n" +
                    "Row 8\n" +
                    "Row 9\n" +
                    "Row 10\n";
}
