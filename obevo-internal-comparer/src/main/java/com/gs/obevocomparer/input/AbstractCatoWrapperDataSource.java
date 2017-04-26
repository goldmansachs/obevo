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
package com.gs.obevocomparer.input;

import com.gs.obevocomparer.util.CatoConfiguration;

public abstract class AbstractCatoWrapperDataSource implements CatoDataSource {

    protected final CatoDataSource baseDataSource;

    protected AbstractCatoWrapperDataSource(CatoDataSource baseDataSource) {
        this.baseDataSource = baseDataSource;
    }

    @Override
    public String getName() {
        return this.baseDataSource.getName();
    }

    @Override
    public String getShortName() {
        return this.baseDataSource.getShortName();
    }

    @Override
    public void open() {
        this.baseDataSource.open();
    }

    @Override
    public void close() {
        this.baseDataSource.close();
    }

    @Override
    public void addDerivedField(CatoDerivedField field) {
        this.baseDataSource.addDerivedField(field);
    }

    @Override
    public void setTypeConverter(CatoTypeConverter converter) {
        this.baseDataSource.setTypeConverter(converter);
    }

    @Override
    public void setSorted(boolean sorted) {
        this.baseDataSource.setSorted(sorted);
    }

    @Override
    public boolean isSorted() {
        return this.baseDataSource.isSorted();
    }

    @Override
    public void setCatoConfiguration(CatoConfiguration configuration) {
        this.baseDataSource.setCatoConfiguration(configuration);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from a DataSource");
    }
}
