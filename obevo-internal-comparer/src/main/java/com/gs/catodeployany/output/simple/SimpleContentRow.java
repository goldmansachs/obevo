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
package com.gs.catodeployany.output.simple;

import com.gs.catodeployany.output.CatoContentRow;

public class SimpleContentRow implements CatoContentRow {

    private final Object[] values;
    private final ValueType[] valueTypes;

    public SimpleContentRow(int size) {
        this.values = new Object[size];
        this.valueTypes = new ValueType[size];
    }

    public Object getValue(int index) {
        return this.values[index];
    }

    public void setValue(int index, Object value) {
        this.values[index] = value;
    }

    public ValueType getValueType(int index) {
        return this.valueTypes[index];
    }

    public void setValueType(int index, ValueType type) {
        this.valueTypes[index] = type;
    }

    public void set(int index, Object value, ValueType type) {
        this.setValue(index, value);
        this.setValueType(index, type);
    }

    public int getSize() {
        return this.values.length;
    }
}
