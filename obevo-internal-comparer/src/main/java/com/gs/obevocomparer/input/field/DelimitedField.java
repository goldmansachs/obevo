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
package com.gs.obevocomparer.input.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class DelimitedField extends AbstractDerivedField<String> {

    private final String delim;

    public DelimitedField(String field, String delim) {
        this(field, field, delim);
    }

    private DelimitedField(String sourceField, String targetField, String delim) {
        super(sourceField, targetField, String.class);
        this.delim = delim;
    }

    protected Object getValue(String sourceValue) {
        StringTokenizer tokenizer = new StringTokenizer(sourceValue, this.delim);
        List<String> tokens = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            tokens.add(tokenizer.nextToken().trim());
        }

        Collections.sort(tokens);

        StringBuilder value = new StringBuilder();
        for (String token : tokens) {
            value.append(token).append(this.delim);
        }

        return tokens.size() > 0 ? value.substring(0, value.length() - this.delim.length()) : value.toString();
    }
}
