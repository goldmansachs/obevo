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
package com.gs.obevo.db.impl.platforms.sqltranslator.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.impl.platforms.sqltranslator.PostParsedSqlTranslator;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;

public class DateFormatterPostParsedSqlTranslator implements PostParsedSqlTranslator {
    private static final ThreadLocal<DateFormat> COMMON_OUTPUT_DATE_FORMAT = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        }
    };

    private final Pattern datetimeConstantPattern = Pattern.compile("(?i)default\\s+'([A-Za-z 0-9:\\-/\\.]+)'");

    private final ImmutableList<ThreadLocal<DateFormat>> dateFormats;

    /**
     * Creates the translator based on the input format strings in JDK format. We take in the strings here so that we
     * can convert to ThreadLocal underneath the hood. (Joda Time could be an alternative, but the existing unit tests
     * on this class need to pass).
     */
    public DateFormatterPostParsedSqlTranslator(ImmutableList<String> dateFormatStrings) {
        this.dateFormats = dateFormatStrings.collect(new Function<String, ThreadLocal<DateFormat>>() {
            @Override
            public ThreadLocal<DateFormat> valueOf(final String dateFormat) {
                return new ThreadLocal<DateFormat>() {
                    @Override
                    protected DateFormat initialValue() {
                        return new SimpleDateFormat(dateFormat);
                    }
                };
            }
        });
    }

    @Override
    public String handleAnySqlPostTranslation(String string, Change change) {
        Matcher matcher = this.datetimeConstantPattern.matcher(string);
        while (matcher.find()) {
            String dateConstant = matcher.group(1);
            for (ThreadLocal<DateFormat> inputFormat : this.dateFormats) {
                try {
                    Date date = inputFormat.get().parse(dateConstant);
                    if (date == null) {
                        continue;
                    }
                    string = string.replace(dateConstant, COMMON_OUTPUT_DATE_FORMAT.get().format(date));
                    break;
                } catch (ParseException e) {
                    continue;
                }
            }
        }

        return string;
    }
}
