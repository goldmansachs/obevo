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
package com.gs.obevocomparer.compare.breaks;

import com.gs.obevocomparer.data.CatoDataObject;

public class BreakExclude {

    private final CatoDataObject key;
    private final String field;
    private final Object leftValue;
    private final Object rightValue;

    public BreakExclude(CatoDataObject key, String field) {
        this(key, field, null, null);
    }

    public BreakExclude(CatoDataObject key, String field, Object leftValue, Object rightValue) {
        if (key == null && field == null && leftValue == null && rightValue == null) {
            throw new IllegalArgumentException("Cannot pass all null values");
        }

        this.key = key;
        this.field = field;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    public CatoDataObject getKey() {
        return this.key;
    }

    public String getField() {
        return this.field;
    }

    public Object getLeftValue() {
        return this.leftValue;
    }

    public Object getRightValue() {
        return this.rightValue;
    }
}
