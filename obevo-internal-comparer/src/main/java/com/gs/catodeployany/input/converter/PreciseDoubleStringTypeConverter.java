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
package com.gs.catodeployany.input.converter;

public class PreciseDoubleStringTypeConverter extends StringTypeConverter {

    @Override
    public Object convert(Object value) {
        if (value == null) {
            return value;
        }

        if (value instanceof Double) {
            Double valueOf = (Double) value;

            valueOf = this.handlePrecision(valueOf);
            return valueOf;
        }

        if (!(value instanceof String)) {
            return value;
        }

        Object str = this.handleString(value);

        return str;
    }

    public PreciseDoubleStringTypeConverter(int precision, String... dateFormats) {
        super(precision, dateFormats);
    }
}
