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
package com.gs.catodeployany.data.simple;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.gs.catodeployany.data.CatoDataSchema;

public class SimpleDataSchema implements CatoDataSchema {

    private final Map<String, Integer> fieldMap = new LinkedHashMap<String, Integer>();

    private final Map<String, String> mappedFields = new HashMap<String, String>();

    public SimpleDataSchema() {
    }

    public SimpleDataSchema(Map<String, String> mappedFields) {
        this.mappedFields.putAll(mappedFields);
    }

    Integer getFieldIndex(String field) {
        return this.fieldMap.get(field);
    }

    Integer getOrCreateFieldIndex(String field) {
        Integer index = this.getFieldIndex(field);
        if (index == null) {
            index = this.fieldMap.size();
            this.fieldMap.put(field, index);
        }
        return index;
    }

    @Override
    public Set<String> getFields() {
        return this.fieldMap.keySet();
    }

    @Override
    public SimpleDataObject createDataObject() {
        return new SimpleDataObject(this);
    }

    @Override
    public Map<String, String> getMappedFields() {
        return this.mappedFields;
    }
}
