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
package com.gs.obevo.util;

import java.util.regex.Pattern;

/**
 * Utility to easily convert a wildcard string into a regexp so that we can still rely on {@link Pattern}.
 * (wildcard strings are much easier for users to use).
 */
public class RegexUtil {
    /**
     * Converts the incoming wildcardPattern (* and % are glob patterns) to the Java-regexp pattern.
     */
    public static String convertWildcardPatternToRegex(String wildcardPattern) {
        return wildcardPattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("%", ".*")
                ;
    }
}
