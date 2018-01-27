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
package com.gs.obevocomparer.input.converter;

import com.gs.obevocomparer.input.CatoTypeConverter;
import org.eclipse.collections.api.block.function.Function;

class CatoTypeConverters {

    public static CatoTypeConverter newStringType() {
        return new StringTypeConverter();
    }

    public static CatoTypeConverter newStringType(String... dateFormats) {
        return new StringTypeConverter(dateFormats);
    }

    public static CatoTypeConverter newStringType(int precision, String... dateFormats) {
        return new StringTypeConverter(precision, dateFormats);
    }

    public static CatoTypeConverter newPreciseDoubleStringType(int precision, String... dateFormats) {
        return new PreciseDoubleStringTypeConverter(precision, dateFormats);
    }

    public static CatoTypeConverter newCustomTypeConverter(final Function<Object, Object> customConvertor) {
        return new CatoTypeConverter() {

            @Override
            public Object convert(Object data) {

                return customConvertor.valueOf(data);
            }
        };
    }
}
