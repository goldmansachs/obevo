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
package com.gs.obevocomparer.compare;

import java.util.Collection;

import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.CatoDataSource;

public class CatoComparison {

    private final String name;
    private final CatoProperties properties;
    private final Collection<Break> breaks;

    private final CatoDataSource leftDataSource;
    private final CatoDataSource rightDataSource;

    private final Collection<CatoDataObject> leftData;
    private final Collection<CatoDataObject> rightData;

    public CatoComparison(String name, CatoProperties properties, Collection<Break> breaks,
            CatoDataSource leftDataSource, Collection<CatoDataObject> leftData,
            CatoDataSource rightDataSource, Collection<CatoDataObject> rightData) {

        this.name = name;
        this.properties = properties;
        this.breaks = breaks;

        this.leftDataSource = leftDataSource;
        this.rightDataSource = rightDataSource;

        this.leftData = leftData;
        this.rightData = rightData;
    }

    public String getName() {
        return this.name;
    }

    public CatoProperties getProperties() {
        return this.properties;
    }

    public Collection<Break> getBreaks() {
        return this.breaks;
    }

    public CatoDataSource getLeftDataSource() {
        return this.leftDataSource;
    }

    public CatoDataSource getRightDataSource() {
        return this.rightDataSource;
    }

    public Collection<CatoDataObject> getLeftData() {
        return this.leftData;
    }

    public Collection<CatoDataObject> getRightData() {
        return this.rightData;
    }

    public Collection<String> getKeyFields() {
        return this.properties.getKeyFields();
    }

    public Collection<String> getExcludeFields() {
        return this.properties.getExcludeFields();
    }
}
