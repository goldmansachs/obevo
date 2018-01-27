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
package com.gs.obevocomparer.input.aggregate;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.CatoDataSchema;
import com.gs.obevocomparer.input.AbstractCatoWrapperDataSource;
import com.gs.obevocomparer.input.CatoDataSource;
import com.gs.obevocomparer.sort.Sort;
import com.gs.obevocomparer.sort.SortedGroupIterator;
import com.gs.obevocomparer.util.CatoConfiguration;

public class AggregateDataSource extends AbstractCatoWrapperDataSource {

    private final List<String> keyFields;
    private final List<? extends AggregateField> aggFields;

    private Iterator<List<CatoDataObject>> sortedGroupData;

    private Sort<CatoDataObject> sort;
    private Comparator<CatoDataObject> dataObjectComparator;
    private CatoDataSchema dataSchema;

    public AggregateDataSource(CatoDataSource baseDataSource, List<String> keyFields, List<? extends AggregateField> aggFields) {
        super(baseDataSource);
        this.keyFields = keyFields;
        this.aggFields = aggFields;
    }

    @Override
    public boolean hasNext() {
        return this.sortedGroupData.hasNext();
    }

    @Override
    public CatoDataObject next() {
        List<CatoDataObject> objs = this.sortedGroupData.next();

        CatoDataObject aggObj = this.dataSchema.createDataObject();

        for (String keyField : this.keyFields) {
            aggObj.setValue(keyField, objs.get(0).getValue(keyField));
        }

        for (AggregateField field : this.aggFields) {
            aggObj.setValue(field.getName(), field.getValue(objs));
        }

        return aggObj;
    }

    @Override
    public void open() {
        super.open();

        Iterator<CatoDataObject> sortedData = this.baseDataSource.isSorted() ? this.baseDataSource : this.sort.sort(this.baseDataSource);
        this.sortedGroupData = new SortedGroupIterator<CatoDataObject>(sortedData, this.dataObjectComparator);
    }

    @Override
    public void setCatoConfiguration(CatoConfiguration configuration) {
        super.setCatoConfiguration(configuration);

        this.sort = configuration.sort();
        this.dataObjectComparator = configuration.dataObjectComparator();
        this.dataSchema = configuration.dataSchema();
    }

    @Override
    public boolean isSorted() {
        return true;
    }

    @Override
    public void setSorted(boolean sorted) {
        throw new UnsupportedOperationException("AggregateDataSource is always sorted");
    }

    public void setSort(Sort<CatoDataObject> sort) {
        this.sort = sort;
    }

    public void setDataObjectComparator(Comparator<CatoDataObject> dataObjectComparator) {
        this.dataObjectComparator = dataObjectComparator;
    }

    public void setDataSchema(CatoDataSchema dataSchema) {
        this.dataSchema = dataSchema;
    }
}
