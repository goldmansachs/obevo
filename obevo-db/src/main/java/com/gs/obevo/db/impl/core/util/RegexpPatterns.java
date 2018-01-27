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
package com.gs.obevo.db.impl.core.util;

import java.util.regex.Pattern;

public class RegexpPatterns {
    public static final Pattern fkPattern = Pattern.compile("(?i)foreign\\s+key");
    public static final Pattern triggerPattern = Pattern.compile("(?i)create\\s+trigger");
    public static final Pattern fkWithNamePattern = Pattern.compile("(?i)foreign\\s+key\\s+\\w+\\s*\\(");
    public static final Pattern modifyTablePattern = Pattern.compile("(?i)alter\\s+table\\s+(\\w+)\\s+modify");
}
