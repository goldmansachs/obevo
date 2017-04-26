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
package com.gs.obevocomparer.input.field;

public class UpperCaseField extends AbstractDerivedField<String> {

    public UpperCaseField(String field) {
        super(field, String.class);
    }

    public UpperCaseField(String sourceField, String targetField) {
        super(sourceField, targetField, String.class);
    }

    protected Object getValue(String sourceValue) {
        return sourceValue.toUpperCase();
    }
}
