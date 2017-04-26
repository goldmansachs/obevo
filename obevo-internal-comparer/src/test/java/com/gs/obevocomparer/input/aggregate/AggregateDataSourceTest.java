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
package com.gs.obevocomparer.input.aggregate;

import java.util.Arrays;
import java.util.List;

import com.gs.obevocomparer.compare.simple.SimpleDataComparator;
import com.gs.obevocomparer.compare.simple.SimpleDataObjectComparator;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.simple.SimpleDataSchema;
import com.gs.obevocomparer.input.aggregate.field.SumField;
import com.gs.obevocomparer.sort.simple.MemorySort;
import com.gs.obevocomparer.util.MockDataSource;
import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class AggregateDataSourceTest {

    private final List<String> keyFields = Arrays.asList("A", "B");
    private final List<AggregateField> aggFields = Arrays.<AggregateField>asList(new SumField("C"));

    private final MockDataSource baseDataSource = new MockDataSource();
    private final AggregateDataSource dataSource = new AggregateDataSource(this.baseDataSource, this.keyFields, this.aggFields);

    @Test
    public void testDataSource() {
        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "abc", "B", 1, "C", 1.0));
        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "abc", "B", 1, "C", 2.0));
        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "abc", "B", 1, "C", 5.0));

        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "abc", "B", 2, "C", 3.0));
        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "abc", "B", 2, "C", 4.0));

        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "def", "B", 2, "C", 1.0));

        this.baseDataSource.addData(TestUtil.createDataObjectWithKeys("A", "ghi", "B", 3, "C", 2.0));

        this.dataSource.setSort(new MemorySort<CatoDataObject>(new SimpleDataObjectComparator(new SimpleDataComparator(), this.keyFields)));
        this.dataSource.setDataObjectComparator(new SimpleDataObjectComparator(new SimpleDataComparator(), this.keyFields));
        this.dataSource.setDataSchema(new SimpleDataSchema());

        List<CatoDataObject> dataObjects = TestUtil.getData(this.dataSource);

        Assert.assertEquals(4, dataObjects.size());
        Assert.assertEquals(8.0, dataObjects.get(0).getValue("C"));
        Assert.assertEquals(7.0, dataObjects.get(1).getValue("C"));
        Assert.assertEquals(1.0, dataObjects.get(2).getValue("C"));
        Assert.assertEquals(2.0, dataObjects.get(3).getValue("C"));
    }

    @Test
    public void testSorted() {
        Assert.assertTrue(this.dataSource.isSorted());

        try {
            this.dataSource.setSorted(false);
            Assert.fail();
        } catch (UnsupportedOperationException ex) {
        }
    }
}
