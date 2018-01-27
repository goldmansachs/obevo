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
package com.gs.obevo.dbmetadata.api;

public class DaColumnImpl implements DaColumn {
    private final DaTable table;
    private final String name;
    private DaColumnDataType columnDataType;
    private boolean nullable = true;
    private String defaultValue;
    private int size;
    private int decimalDigits;

    public DaColumnImpl(DaTable table, String name) {
        this.table = table;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DaColumnDataType getColumnDataType() {
        return columnDataType;
    }

    public void setColumnDataType(DaColumnDataType columnDataType) {
        this.columnDataType = columnDataType;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public DaTable getParent() {
        return table;
    }

    @Override
    public String getWidth() {
        return null;
    }

    @Override
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(int decimalDigits) {
        this.decimalDigits = decimalDigits;
    }
}
