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
package com.gs.obevocomparer.input.converter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gs.obevocomparer.input.CatoTypeConverter;

public class SimpleTypeConverter implements CatoTypeConverter {

    protected final int integerLength = Integer.toString(Integer.MAX_VALUE).length();
    protected final int longLength = Long.toString(Long.MAX_VALUE).length();
    protected final Pattern integerPattern = Pattern.compile("^[-]?[0-9]+$");
    protected final Pattern doublePattern = Pattern.compile("^[+-]?[0-9]*(\\.[0-9]*)?$");

    protected final List<DateFormat> dateFormats;
    protected int precision = -1;

    public SimpleTypeConverter(int precision, String... dateFormats) {
        this.precision = precision;
        this.dateFormats = this.createDateFormats(dateFormats);
    }

    public SimpleTypeConverter(String... dateFormats) {

        this.dateFormats = this.createDateFormats(dateFormats);
    }

    public Object convert(Object value) {

        return value;
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
