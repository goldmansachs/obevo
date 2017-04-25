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
package com.gs.obevocomparer.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.CatoDataSource;
import com.gs.obevocomparer.input.CatoDerivedField;
import com.gs.obevocomparer.input.CatoTypeConverter;

public class MockDataSource implements CatoDataSource {

    private final String name;

    private final List<CatoDataObject> data = new ArrayList<CatoDataObject>();
    private Iterator<CatoDataObject> iter;

    public MockDataSource() {
        this("Test");
    }

    public MockDataSource(String name) {
        this.name = name;
    }

    public void addData(CatoDataObject obj) {
        this.data.add(obj);
    }

    public void addData(Object... fields) {
        this.data.add(TestUtil.createDataObject(fields));
    }

    public void shuffle() {
        Collections.shuffle(this.data);
    }

    public void addDerivedField(CatoDerivedField field) {
    }

    public void close() {
    }

    public String getName() {
        return this.name;
    }

    public void open() {
        this.iter = this.data.iterator();
    }

    public boolean hasNext() {
        return this.iter.hasNext();
    }

    public CatoDataObject next() {
        return this.iter.next();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean isSorted() {
        return false;
    }

    public void setTypeConverter(CatoTypeConverter converter) {
        throw new UnsupportedOperationException();
    }

    public void setSorted(boolean sorted) {
        throw new UnsupportedOperationException();
    }

    public void setCatoConfiguration(CatoConfiguration configuration) {
    }

    public String getShortName() {
        return this.name;
    }
}
