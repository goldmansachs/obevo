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
package com.gs.obevocomparer.compare.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gs.obevocomparer.compare.CatoDataComparator;
import com.gs.obevocomparer.compare.CatoProperties;
import com.gs.obevocomparer.compare.breaks.BreakExclude;

public class SimpleCatoProperties implements CatoProperties {

    private final List<String> keyFields;
    private Set<String> excludeFields;

    private Map<String, String> mappedFields;
    private Collection<BreakExclude> breakExcludes;

    private int decimalPrecision;

    public SimpleCatoProperties(List<String> keyFields) {
        this(keyFields, Collections.<String>emptyList());
    }

    public SimpleCatoProperties(List<String> keyFields, Collection<String> excludeFields) {

        this.keyFields = new LinkedList<String>(keyFields);
        this.excludeFields = new LinkedHashSet<String>(excludeFields);

        this.mappedFields = new HashMap<String, String>();
        this.breakExcludes = new ArrayList<BreakExclude>();
        this.decimalPrecision = CatoDataComparator.DEFAULT_DECIMAL_PRECISION;
    }

    public List<String> getKeyFields() {
        return this.keyFields;
    }

    public Set<String> getExcludeFields() {
        return this.excludeFields;
    }

    public Map<String, String> getMappedFields() {
        return this.mappedFields;
    }

    private void setMappedFields(Map<String, String> mappedFields) {
        this.mappedFields = mappedFields;
    }

    public void addMappedField(String leftField, String rightField) {
        this.mappedFields.put(leftField, rightField);
    }

    public void setMappedFields(String... fields) {
        if (fields.length % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of fields to map");
        }

        Map<String, String> mappedFields = new HashMap<String, String>();
        for (int i = 0; i < fields.length; i += 2) {
            mappedFields.put(fields[i], fields[i + 1]);
        }

        this.setMappedFields(mappedFields);
    }

    public Collection<BreakExclude> getBreakExcludes() {
        return this.breakExcludes;
    }

    public SimpleCatoProperties withBreakExcludes(Collection<BreakExclude> breakExcludes) {
        this.breakExcludes = breakExcludes;
        return this;
    }

    public SimpleCatoProperties withDecimalPrecision(int precision) {
        this.decimalPrecision = precision;
        return this;
    }

    public int getDecimalPrecision() {
        return this.decimalPrecision;
    }

    public void setDecimalPrecision(int decimalPrecision) {
        this.decimalPrecision = decimalPrecision;
    }

    public SimpleCatoProperties withExcludedFields(Set<String> excludeFields) {
        this.excludeFields = excludeFields;
        return this;
    }

    public SimpleCatoProperties withMappedFields(String... fields) {
        this.setMappedFields(fields);
        return this;
    }
}
