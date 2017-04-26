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
package com.gs.obevo.util.lookuppredicate;

import java.util.regex.Pattern;

import com.gs.obevo.util.RegexUtil;

/**
 * Does a wildcard pattern match (% or * as glob patterns). Helpful for more-succinct user inputs.
 */
public class WildcardPatternIndex implements Index {
    private final Pattern pattern;

    public WildcardPatternIndex(Pattern pattern) {
        this.pattern = pattern;
    }

    public WildcardPatternIndex(String patternString) {
        this(Pattern.compile(RegexUtil.convertWildcardPatternToRegex(patternString)));
    }

    @Override
    public boolean accept(String each) {
        return pattern.matcher(each).matches();
    }

    @Override
    public String toString() {
        return "WildcardPatternIndex{" +
                "pattern=" + pattern +
                '}';
    }
}
