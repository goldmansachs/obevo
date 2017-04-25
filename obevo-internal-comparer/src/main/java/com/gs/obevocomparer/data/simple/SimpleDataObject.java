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
package com.gs.obevocomparer.data.simple;

import java.util.Set;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.data.CatoDataSchema;

public class SimpleDataObject implements CatoDataObject {

    private final SimpleDataSchema schema;
    private Object[] data;

    SimpleDataObject(SimpleDataSchema schema) {
        this.schema = schema;
        this.data = new Object[schema.getFields().size()];
    }

    @Override
    public Object getValue(String field) {
        Integer index = this.schema.getFieldIndex(field);
        if (index == null || this.data.length <= index) {
            return null;
        }

        return this.data[index];
    }

    @Override
    public void setValue(String field, Object value) {
        int index = this.schema.getOrCreateFieldIndex(field);

        if (this.data.length <= index) {
            this.data = this.copy(this.data, index + 1);
        }

        this.data[index] = value;
    }

    @Override
    public Set<String> getFields() {
        return this.schema.getFields();
    }

    @Override
    public CatoDataSchema getSchema() {
        return this.schema;
    }

    private Object[] copy(Object[] data, int newLength) {
        Object[] copy = new Object[newLength];
        System.arraycopy(data, 0, copy, 0, Math.min(data.length, newLength));
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName()).append(" {");

        if (this.getFields().isEmpty()) {
            return builder.append("}").toString();
        }

        for (String field : this.getFields()) {
            builder.append(field).append("=").append(this.getValue(field)).append(this.getClass(field)).append(", ");
        }

        return builder.substring(0, builder.length() - 2) + "}";
    }

    private String getClass(String field) {
        if (this.getValue(field) == null) {
            return "";
        } else {
            return " [" + this.getValue(field).getClass().getSimpleName() + "]";
        }
    }
}
