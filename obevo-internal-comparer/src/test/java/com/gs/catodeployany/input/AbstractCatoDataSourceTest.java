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
package com.gs.catodeployany.input;

import java.util.Arrays;
import java.util.Iterator;

import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.data.CatoDataSchema;
import com.gs.catodeployany.input.converter.StringTypeConverter;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractCatoDataSourceTest {

    TestDataSource source;
    final CatoDataSchema schema = TestUtil.createSchema();

    @Before
    public void setUp() {
        this.source = new TestDataSource("Test Source");
        this.source.setDataSchema(this.schema);
    }

    @Test
    public void testDefinedShortName() {
        TestDataSource source = new TestDataSource("Long Name", "Short Name");

        Assert.assertEquals("Long Name", source.getName());
        Assert.assertEquals("Short Name", source.getShortName());
    }

    @Test
    public void testOneWordShortName() {
        TestDataSource source = new TestDataSource("Test");

        Assert.assertEquals("Test", source.getName());
        Assert.assertEquals("Test", source.getShortName());

        source = new TestDataSource(" 	Test2");

        Assert.assertEquals("Test2", source.getName());
        Assert.assertEquals("Test2", source.getShortName());

        source = new TestDataSource("	Test3  \n ");

        Assert.assertEquals("Test3", source.getName());
        Assert.assertEquals("Test3", source.getShortName());
    }

    @Test
    public void testMultiWordShortName() {
        TestDataSource source = new TestDataSource("Test Full Name");

        Assert.assertEquals("Test Full Name", source.getName());
        Assert.assertEquals("Test", source.getShortName());

        source = new TestDataSource("  Test2 Full Name ");

        Assert.assertEquals("Test2 Full Name", source.getName());
        Assert.assertEquals("Test2", source.getShortName());

        source = new TestDataSource("		Test3 Full Name With Tabs	");

        Assert.assertEquals("Test3 Full Name With Tabs", source.getName());
        Assert.assertEquals("Test3", source.getShortName());
    }

    @Test
    public void testDataSchema() throws Exception {
        TestDataSource source = new TestDataSource("Test");

        CatoDataSchema schema = mock(CatoDataSchema.class);
        when(schema.createDataObject()).thenReturn(TestUtil.createDataObjectWithKeys());
        source.setDataSchema(schema);

        source.nextDataObject();

        verify(schema).createDataObject();
    }

    @Test
    public void testOpenAndClose() throws Exception {

        Assert.assertFalse(this.source.isOpen());
        Assert.assertFalse(this.source.isClosed());

        this.source.open();

        Assert.assertTrue(this.source.isOpen());
        Assert.assertFalse(this.source.isClosed());

        Assert.assertEquals(1, this.source.next().getValue("a"));
        Assert.assertEquals(2, this.source.next().getValue("a"));
        Assert.assertEquals(3, this.source.next().getValue("a"));

        this.source.close();

        Assert.assertTrue(this.source.isOpen());
        Assert.assertTrue(this.source.isClosed());
    }

    @Test
    public void testNextBeforeOpen() {
        try {
            this.source.next();
            Assert.fail();
        } catch (Exception ex) {
            TestUtil.assertException("Cannot iterate over a DataSource", UnsupportedOperationException.class, ex);
        }
    }

    @Test
    public void testCloseBeforeOpen() {
        try {
            this.source.close();
            org.junit.Assert.fail();
        } catch (Exception ex) {
            TestUtil.assertException("is not open", UnsupportedOperationException.class, ex.getCause());
        }
    }

    @Test
    public void testOpenAfterOpen() {
        this.source.open();

        try {
            this.source.open();
            org.junit.Assert.fail();
        } catch (Exception ex) {
            TestUtil.assertException("is already open", UnsupportedOperationException.class, ex.getCause());
        }
    }

    @Test
    public void testOpenAfterClose() {
        this.source.open();
        this.source.close();

        try {
            this.source.open();
            org.junit.Assert.fail();
        } catch (Exception ex) {
            TestUtil.assertException("is already closed", UnsupportedOperationException.class, ex.getCause());
        }
    }

    private static class TestDataSource extends AbstractCatoDataSource {

        private final Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();

        private boolean open = false;
        private boolean closed = false;

        public TestDataSource(String name) {
            super(name, new StringTypeConverter());
        }

        public TestDataSource(String name, String shortName) {
            super(name, shortName, new StringTypeConverter());
        }

        public boolean isOpen() {
            return this.open;
        }

        public boolean isClosed() {
            return this.closed;
        }

        @Override
        protected void openSource() throws Exception {
            this.open = true;
        }

        @Override
        protected void closeSource() throws Exception {
            this.closed = true;
        }

        @Override
        public CatoDataObject nextDataObject() throws Exception {
            if (!this.iter.hasNext()) {
                return null;
            }

            CatoDataObject obj = this.createDataObject();
            obj.setValue("a", this.iter.next());
            return obj;
        }
    }
}
