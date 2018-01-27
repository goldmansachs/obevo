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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class StringTypeConverter extends SimpleTypeConverter {
    public StringTypeConverter(int precision, String... dateFormats) {
        super(precision, dateFormats);
    }

    public StringTypeConverter(String... dateFormats) {

        super(dateFormats);
    }

    public Object convert(Object value) {
        if (value == null) {
            return value;
        }

        if (!(value instanceof String)) {
            return value;
        }

        Object str = this.handleString(value);

        return str;
    }

    Object handleString(Object value) {
        String str = value.toString().trim();

        if (str.length() == 0) {
            return null;
        }

        if (str.equalsIgnoreCase("TRUE") || str.equalsIgnoreCase("FALSE")) {
            return Boolean.valueOf(str);
        }

        if (this.integerPattern.matcher(str).matches()) {
            if (str.length() < this.integerLength) {
                return Integer.valueOf(str);
            } else if (str.length() < this.longLength) {
                return Long.valueOf(str);
            }
        }

        if (this.doublePattern.matcher(str).matches()) {
            try {
                Double valueOf = Double.valueOf(str);

                valueOf = this.handlePrecision(valueOf);

                return valueOf;
            } catch (NumberFormatException ex) {
            }
        }

        for (DateFormat format : this.dateFormats) {
            try {
                return format.parse(str);
            } catch (ParseException ex) {
            }
        }
        return str;
    }

    Double handlePrecision(Double valueOf) {
        if (this.precision >= 0 && valueOf > Long.MIN_VALUE && valueOf < Long.MAX_VALUE) {

            double factor = Math.pow(10, this.precision);
            valueOf = Math.round(valueOf * factor) / factor;
        }
        return valueOf;
    }

    private List<DateFormat> createDateFormats(String[] dateFormatStrs) {
        List<DateFormat> dateFormats = new ArrayList<DateFormat>();
        SimpleDateFormat dateFormat;

        for (String format : dateFormatStrs) {
            dateFormat = new SimpleDateFormat(format);
            dateFormat.setLenient(false);
            dateFormats.add(dateFormat);
        }
        return dateFormats;
    }

    public void sethPrecision(int precision) {
        this.precision = precision;
    }
}
