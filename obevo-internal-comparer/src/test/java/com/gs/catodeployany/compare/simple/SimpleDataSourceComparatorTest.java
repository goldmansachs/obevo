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
package com.gs.catodeployany.compare.simple;

import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.compare.breaks.Break;
import com.gs.catodeployany.compare.breaks.DataObjectBreak;
import com.gs.catodeployany.compare.breaks.FieldBreak;
import com.gs.catodeployany.compare.breaks.GroupBreak;
import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.spring.CatoSimpleJavaConfiguration;
import com.gs.catodeployany.util.MockDataSource;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDataSourceComparatorTest {

    private static MockDataSource dataSource1;
    private static MockDataSource dataSource2;
    private static SimpleDataSourceComparator comparator;

    private final SimpleCatoProperties properties = TestUtil.getProperties();
    private CatoDataObject obj1;
    private CatoDataObject obj2;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleDataSourceComparatorTest.class);

    @Before
    public void init() {
        CatoSimpleJavaConfiguration configuration = new CatoSimpleJavaConfiguration();
        configuration.setProperties(this.properties);
        comparator = (SimpleDataSourceComparator) configuration.dataSourceComparator();

        LOG.info("Using DataSourceComparator class {}", comparator.getClass());
    }

    @Test
    public void testCompareDataObjectsNoBreak() {
        this.obj1 = TestUtil.createDataObject("A", "A", "1", "2", "3");
        this.obj2 = TestUtil.createDataObject("A", "A", "1", "2", "3");
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));
    }

    @Test
    public void testCompareDataObjectsWithBreak() {
        this.obj1 = TestUtil.createDataObject("A", "A", "1", "2", "3");
        this.obj2 = TestUtil.createDataObject("A", "A", "1", "4", "3");
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));
    }

    @Test
    public void testCompareDataObjectsWithBreaks2() {
        this.obj1 = TestUtil.createDataObject("A", "A", "1", "2", "5");
        this.obj2 = TestUtil.createDataObject("A", "A", "1", "4", "5");
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));
    }

    @Test
    public void testCompareDataObjectsWithExcludes() {
        this.obj1 = TestUtil.createDataObject("A", "A", "1", "2", "3", "4");
        this.obj2 = TestUtil.createDataObject("A", "A", "1", "2", "3", "5");
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));
    }

    @Test
    public void testCompareDataObjectsWithDifferentKeys() {
        try {
            this.obj1 = TestUtil.createDataObject("A", "A", "1", "2", "3");
            this.obj2 = TestUtil.createDataObject("A", "B", "1", "2", "3");
            comparator.compareDataObjects(this.obj1, this.obj2);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            Assert.fail();
        }
    }

    @Test
    public void testCompareDataObjectsMissingFields() {
        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2);
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.properties.getExcludeFields().add("C");
        System.out.println("-------" + this.properties.getExcludeFields());

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.properties.getExcludeFields().remove("C");
    }

    @Test
    public void testCompareDataObjectsWithMappedFields() {

        this.properties.addMappedField("C", "D");
        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 3);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 4);
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "D", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 3);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "D", 3);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "D", 3);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "D", 3);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.properties.getExcludeFields().add("E");
        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "E", 4);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 3, "E", 44);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.properties.getMappedFields().remove("C");
        this.properties.getExcludeFields().remove("E");
    }

    @Test
    public void testCompareDataObjectsWithMissingAndMappedAndExcludeFields() {
        this.properties.addMappedField("C", "D");
        this.properties.getExcludeFields().add("Q");

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "Q", 4);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 3, "Q", 44);
        Assert.assertNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "E", 4, "Q", 4);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 3, "Q", 44);
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));

        this.obj1 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "C", 3, "Q", 4);
        this.obj2 = TestUtil.createDataObjectWithKeys("A", 1, "B", 2, "D", 3, "E", 4, "Q", 44);
        Assert.assertNotNull(comparator.compareDataObjects(this.obj1, this.obj2));
    }

    @Test
    public void testCompareDataObjectsWithNullObjects() {
        this.obj1 = TestUtil.createDataObject("A", "A", "1", "2", "3");
        this.obj2 = TestUtil.createDataObject("A", "A", "1", "2", "3");

        try {
            comparator.compareDataObjects(null, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            Assert.fail();
        }
        try {
            comparator.compareDataObjects(this.obj1, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            Assert.fail();
        }

        try {
            comparator.compareDataObjects(null, this.obj2);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            Assert.fail();
        }
    }

    @Test
    public void trivialCompareTest() {
        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0);
    }

    @Test
    public void fieldBreakCompareTest() {
        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource2.addData(1, 2, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0);

        dataSource1.addData(1, 3, 3, 4, 5);
        dataSource2.addData(1, 3, 4, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 1, 0, 0);

        dataSource1.addData(1, 4, 1, 2, 3);
        dataSource2.addData(1, 4, 1, 3, 4);
        this.assertBreaks(dataSource1, dataSource2, 2, 0, 0);

        dataSource1.addData(1, 5, 1, 2, 3, "ignore");
        dataSource2.addData(1, 5, 1, 2, 3, "ignore2");
        this.assertBreaks(dataSource1, dataSource2, 2, 0, 0);
    }

    @Test
    public void objectBreakCompareTest() {
        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 1);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource2.addData(1, 2, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 1, 0);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource2.addData(1, 3, 3, 4, 5);
        dataSource2.addData(1, 4, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 2, 0);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 3, 3, 4, 5);
        dataSource2.addData(1, 3, 3, 4, 5);
        dataSource1.addData(1, 4, 3, 4, 5);
        dataSource2.addData(1, 5, 3, 4, 5);
        dataSource2.addData(1, 6, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 2, 1);
    }

    @Test
    public void objectGroupBreakCompareTest() {
        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource1.addData(1, 2, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 2, 0, 0);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource1.addData(1, 2, 3, 4, 6);
        dataSource2.addData(1, 2, 3, 4, 7);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0, 1, 2);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource1.addData(1, 2, 3, 4, 6);
        dataSource2.addData(1, 2, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0, 0, 1);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource2.addData(1, 2, 3, 4, 5);
        dataSource2.addData(1, 2, 3, 4, 6);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0, 1, 0);

        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();
        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource2.addData(1, 2, 3, 4, 5);
        dataSource2.addData(1, 2, 3, 4, 5);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0, 1, 0);

        dataSource1.addData(1, 3, 3, 4, 5);
        dataSource2.addData(1, 3, 3, 4, 6);
        dataSource2.addData(1, 3, 3, 4, 7);
        dataSource2.addData(1, 3, 3, 4, 8);
        this.assertBreaks(dataSource1, dataSource2, 0, 0, 0, 4, 1);
    }

    @Test
    public void multipleBreakTypesCompareTest() {
        dataSource1 = new MockDataSource();
        dataSource2 = new MockDataSource();

        dataSource1.addData(1, 2, 3, 4, 5);
        dataSource2.addData(1, 2, 3, 4, 5);

        dataSource1.addData(1, 3, 1, 2, 3);
        dataSource2.addData(1, 3, 1, 2, 4);

        dataSource1.addData(1, 4, 1, 2, 3);
        dataSource2.addData(1, 4, 1, 3, 4);

        dataSource1.addData(1, 5, 1, 2, 3, "ignore");
        dataSource2.addData(1, 5, 1, 2, 3, "ignore2");
        this.assertBreaks(dataSource1, dataSource2, 2, 0, 0, 0, 0);

        dataSource1.addData(1, 6, 1, 2, 3);
        this.assertBreaks(dataSource1, dataSource2, 2, 0, 1, 0, 0);

        dataSource2.addData(1, 7, 1, 2, 3);
        this.assertBreaks(dataSource1, dataSource2, 2, 1, 1, 0, 0);

        dataSource2.addData(1, 7, 1, 2, 3);
        dataSource2.addData(1, 7, 1, 2, 4);
        this.assertBreaks(dataSource1, dataSource2, 2, 3, 1, 0, 0);

        dataSource1.addData(1, 8, 1, 2, 3);
        dataSource2.addData(1, 8, 1, 2, 3);
        dataSource2.addData(1, 8, 1, 2, 3);
        this.assertBreaks(dataSource1, dataSource2, 2, 3, 1, 1, 0);

        dataSource1.addData(1, 8, 1, 2, 4);
        dataSource2.addData(1, 8, 1, 2, 4);
        this.assertBreaks(dataSource1, dataSource2, 2, 3, 1, 1, 0);

        dataSource1.addData(1, 8, 1, 2, 5);
        dataSource2.addData(1, 8, 1, 2, 6);
        this.assertBreaks(dataSource1, dataSource2, 2, 3, 1, 2, 1);

        dataSource1.addData(1, 9, 1, 2, 3);
        dataSource2.addData(1, 9, 1, 2, 3);
        dataSource2.addData(1, 9, 1, 2, 4);
        this.assertBreaks(dataSource1, dataSource2, 2, 3, 1, 3, 1);

        dataSource1.addData(1, 10, 1, 2, 3, "ignore1");
        dataSource2.addData(1, 10, 1, 2, 3, "ignore2");
        dataSource2.addData(1, 10, 1, 2, 4, "ignore3");
        this.assertBreaks(dataSource1, dataSource2, 2, 3, 1, 4, 1);
    }

    private void assertBreaks(MockDataSource dataSource1, MockDataSource dataSource2, int fieldBreakCount, int missingBreakCount, int additionalBreakCount) {
        this.assertBreaks(dataSource1, dataSource2, fieldBreakCount, missingBreakCount, additionalBreakCount, 0, 0);
    }

    private void assertBreaks(MockDataSource dataSource1, MockDataSource dataSource2, int fieldBreakCount, int missingBreakCount, int additionalBreakCount, int missingGroupBreakCount, int additionalGroupBreakCount) {
        for (int i = 0; i < 10; i++) {
            int fieldBreaks = 0;
            int missingBreaks = 0;
            int additionalBreaks = 0;
            int missingGroupBreaks = 0;
            int additionalGroupBreaks = 0;

            dataSource1.shuffle();
            dataSource2.shuffle();

            CatoComparison comparison = comparator.compare("Test Comp", dataSource1, dataSource2);

            Assert.assertEquals("Test Comp", comparison.getName());
            Assert.assertEquals(this.properties, comparison.getProperties());

            Assert.assertEquals(dataSource1, comparison.getLeftDataSource());
            Assert.assertEquals(dataSource2, comparison.getRightDataSource());

            for (Break br : comparison.getBreaks()) {
                if (br instanceof FieldBreak) {
                    fieldBreaks++;
                } else if (br instanceof DataObjectBreak && br.getDataSide() == CatoDataSide.LEFT) {
                    additionalBreaks++;
                } else if (br instanceof DataObjectBreak && br.getDataSide() == CatoDataSide.RIGHT) {
                    missingBreaks++;
                } else if (br instanceof GroupBreak && br.getDataSide() == CatoDataSide.LEFT) {
                    additionalGroupBreaks++;
                } else if (br instanceof GroupBreak && br.getDataSide() == CatoDataSide.RIGHT) {
                    missingGroupBreaks++;
                }
            }

            Assert.assertEquals(fieldBreakCount, fieldBreaks);
            org.junit.Assert.assertEquals(missingBreakCount, missingBreaks);
            org.junit.Assert.assertEquals(additionalBreakCount, additionalBreaks);
            org.junit.Assert.assertEquals(missingGroupBreakCount, missingGroupBreaks);
            org.junit.Assert.assertEquals(additionalGroupBreakCount, additionalGroupBreaks);
        }
    }
}
