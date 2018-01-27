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

import org.eclipse.collections.api.block.function.Function;

public interface DaColumn extends DaNamedObject {
    Function<DaColumn, DaColumnDataType> TO_COLUMN_DATA_TYPE = new Function<DaColumn, DaColumnDataType>() {
        @Override
        public DaColumnDataType valueOf(DaColumn arg0) {
            return arg0.getColumnDataType();
        }
    };
    Function<DaColumn, String> TO_WIDTH = new Function<DaColumn, String>() {
        @Override
        public String valueOf(DaColumn arg0) {
            return arg0.getWidth();
        }
    };
    Function<DaColumn, String> TO_DEFAULT_VALUE = new Function<DaColumn, String>() {
        @Override
        public String valueOf(DaColumn arg0) {
            return arg0.getDefaultValue();
        }
    };
    Function<DaColumn, Boolean> TO_NULLABLE = new Function<DaColumn, Boolean>() {
        @Override
        public Boolean valueOf(DaColumn arg0) {
            return arg0.isNullable();
        }
    };
    Function<DaColumn, Integer> TO_DECIMAL_DIGITS = new Function<DaColumn, Integer>() {
        @Override
        public Integer valueOf(DaColumn arg0) {
            return arg0.getDecimalDigits();
        }
    };
    Function<DaColumn, Integer> TO_SIZE = new Function<DaColumn, Integer>() {
        @Override
        public Integer valueOf(DaColumn arg0) {
            return arg0.getSize();
        }
    };

    DaColumnDataType getColumnDataType();

    String getDefaultValue();

    boolean isNullable();

    DaTable getParent();

    String getWidth();

    int getDecimalDigits();

    int getSize();
}
