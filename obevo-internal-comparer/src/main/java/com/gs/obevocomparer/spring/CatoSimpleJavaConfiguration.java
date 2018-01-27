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
package com.gs.obevocomparer.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javax.annotation.Resource;

import com.gs.obevocomparer.compare.CatoBreakExcluder;
import com.gs.obevocomparer.compare.CatoDataComparator;
import com.gs.obevocomparer.compare.CatoDataSourceComparator;
import com.gs.obevocomparer.compare.CatoProperties;
import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.compare.simple.SimpleBreakExcluder;
import com.gs.obevocomparer.compare.simple.SimpleDataComparator;
import com.gs.obevocomparer.compare.simple.SimpleDataObjectComparator;
import com.gs.obevocomparer.compare.simple.SimpleDataSourceComparator;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.CatoDataSchema;
import com.gs.obevocomparer.data.simple.SimpleDataSchema;
import com.gs.obevocomparer.sort.Sort;
import com.gs.obevocomparer.sort.simple.MemorySort;
import com.gs.obevocomparer.util.CatoConfiguration;
import com.gs.obevocomparer.util.CollectionFactory;
import com.gs.obevocomparer.util.Factory;

/**
 * Java-based configuration; named as such as we are refactoring this from the Spring configuration, and want the
 * Spring config class to keep the original name for backwards-compatibility
 */
public class CatoSimpleJavaConfiguration implements CatoConfiguration {
    @Resource  // we keep the resource annotation here to help w/ the Spring config subclass, and it is harmless here
    private
    CatoProperties properties;

    public CatoSimpleJavaConfiguration() {
    }

    public CatoSimpleJavaConfiguration(CatoProperties properties) {
        this.properties = properties;
    }

    @Override
    public CatoProperties getProperties() {
        return this.properties;
    }

    public void setProperties(CatoProperties properties) {
        this.properties = properties;
    }

    @Override
    public CatoDataSourceComparator dataSourceComparator() {
        return new SimpleDataSourceComparator(this.properties, this.dataObjectComparator(), this.dataComparator(), this.sort(),
                this.breakCollectionFactory(), this.dataCollectionFactory());
    }

    private Factory<Collection<Break>> breakCollectionFactory() {
        return new CollectionFactory<Break>(ArrayList.class);
    }

    private Factory<Collection<CatoDataObject>> dataCollectionFactory() {
        return new CollectionFactory<CatoDataObject>(ArrayList.class);
    }

    private CatoDataComparator dataComparator() {
        return new SimpleDataComparator(this.properties.getDecimalPrecision());
    }

    public Comparator<CatoDataObject> dataObjectComparator() {
        return new SimpleDataObjectComparator(this.dataComparator(), this.properties.getKeyFields());
    }

    public Sort<CatoDataObject> sort() {
        return new MemorySort<CatoDataObject>(this.dataObjectComparator());
    }

    @Override
    public CatoBreakExcluder breakExcluder() {
        return new SimpleBreakExcluder(this.dataComparator());
    }

    // prototype
    public CatoDataSchema dataSchema() {
        return new SimpleDataSchema(this.properties.getMappedFields());
    }
}
