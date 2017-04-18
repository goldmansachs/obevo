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
package com.gs.obevo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

public class DAStringUtil {
    private static final Pattern pattern = Pattern.compile("\\s+", Pattern.MULTILINE);
    public static final Predicate<String> STRING_IS_BLANK = new Predicate<String>() {
        @Override
        public boolean accept(String s) {
            return StringUtils.isBlank(s);
        }
    };

    /**
     * Replaces all forms and lengths of whitespace w/ a single space so that we can subsequently calculate the hash of
     * a string based solely on the textual content.
     * Note that for practical reasons, we treat this as a 99.99999% accurate thing, i.e. we do not try to be smart
     * enough where we only parse out whitespace that is not inside quotes (i.e. if it is an actual string literal).
     * (Though if we can get it to 100% one day, I'm all for it. But in practice, this should be good enough)
     */
    public static String normalizeWhiteSpaceFromString(String content) {
        if (content == null) {
            return null;
        }
        final Matcher matcher = pattern.matcher(content);
        final String s = matcher.replaceAll(" ").trim();
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    /**
     * See {@link this#normalizeWhiteSpaceFromString(String)}. This is the "old" version of that method, with a slightly
     * harder-to-read implementation. I want to switch to {@link this#normalizeWhiteSpaceFromString(String)} as it is
     * a more standard implementation and thus easier to vet. Will replace this in DEPLOYANY-414
     */
    public static String normalizeWhiteSpaceFromStringOld(String content) {
        if (content == null) {
            return null;
        }
        String[] lines = content.split("\\r?\\n");

        MutableList<String> newContent = Lists.mutable.empty();
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                line = line.replaceAll("\\s+", " ");
                newContent.add(line.trim());
            }
        }

        return newContent.makeString(" ").trim();
    }
}
