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
package com.gs.obevo.db.impl.platforms.sybaseiq.iqload;

import org.eclipse.collections.api.block.function.Function;

public class FieldToColumnMapping {
    private final String fieldName;
    private final String columnName;
    private final Object defaultValue;

    public static Function<FieldToColumnMapping, String> fieldName() {
        return new Function<FieldToColumnMapping, String>() {
            @Override
            public String valueOf(FieldToColumnMapping arg0) {
                return arg0.getFieldName();
            }
        };
    }

    public static Function<FieldToColumnMapping, String> columnName() {
        return new Function<FieldToColumnMapping, String>() {
            @Override
            public String valueOf(FieldToColumnMapping arg0) {
                return arg0.getColumnName();
            }
        };
    }

    public static Function<FieldToColumnMapping, Object> defaultValue() {
        return new Function<FieldToColumnMapping, Object>() {
            @Override
            public Object valueOf(FieldToColumnMapping arg0) {
                return arg0.getDefaultValue();
            }
        };
    }

    public static FieldToColumnMapping create(String fieldName, String columnName) {
        return new FieldToColumnMapping(fieldName, columnName);
    }

    /**
     * Creates a Field to Column Mapping with a default value that does not map to a field.
     * Hence, the default value will always be used.
     */
    public static FieldToColumnMapping createWithDefaultValue(String columnName, Object defaultValue) {
        return new FieldToColumnMapping(null, columnName, defaultValue);
    }

    /**
     * @param fieldName
     * @param columnName
     * @param defaultValue
     * @return
     */
    public static FieldToColumnMapping createWithDefaultValue(String fieldName, String columnName, Object defaultValue) {
        return new FieldToColumnMapping(fieldName, columnName, defaultValue);
    }

    FieldToColumnMapping(String fieldName, String columnName) {
        this(fieldName, columnName, null);
    }

    private FieldToColumnMapping(String fieldName, String columnName, Object defaultValue) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.defaultValue = defaultValue;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }
}
