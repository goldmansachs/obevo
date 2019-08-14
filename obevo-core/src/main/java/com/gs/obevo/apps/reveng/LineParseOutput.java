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
package com.gs.obevo.apps.reveng;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

public class LineParseOutput {
    private String lineOutput;
    private MutableMap<String, String> tokens = Maps.mutable.empty();

    public LineParseOutput() {
    }

    public LineParseOutput(String lineOutput) {
        this.lineOutput = lineOutput;
    }

    public String getLineOutput() {
        return lineOutput;
    }

    public void setLineOutput(String lineOutput) {
        this.lineOutput = lineOutput;
    }

    public MutableMap<String, String> getTokens() {
        return tokens;
    }

    public void addToken(String key, String value) {
        tokens.put(key, value);
    }

    LineParseOutput withToken(String key, String value) {
        tokens.put(key, value);
        return this;
    }
}
