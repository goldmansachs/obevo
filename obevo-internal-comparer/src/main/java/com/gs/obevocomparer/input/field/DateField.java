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
package com.gs.obevocomparer.input.field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateField extends AbstractDerivedField<String> {

    private final DateFormat dateFormat;

    public DateField(String field, String format) {
        super(field, String.class);
        this.dateFormat = new SimpleDateFormat(format);
    }

    public DateField(String sourceField, String targetField, String format) {
        super(sourceField, targetField, String.class);
        this.dateFormat = new SimpleDateFormat(format);
    }

    protected Object getValue(String sourceValue) {
        try {
            return this.dateFormat.parse(sourceValue);
        } catch (ParseException ex) {
            LOG.error("Failed to parse date '{}'", sourceValue);
            return sourceValue;
        }
    }
}
