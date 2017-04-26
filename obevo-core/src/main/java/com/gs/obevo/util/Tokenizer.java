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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.Pair;

public class Tokenizer {
    private final MapIterable<String, String> params;
    private final String paramPrefix;
    private final String paramSuffix;

    public Tokenizer(MapIterable<String, String> params, String paramPrefix, String paramSuffix) {
        this.params = params;
        this.paramPrefix = paramPrefix;
        this.paramSuffix = paramSuffix;
    }

    public String tokenizeString(String input) {
        String output = input;
        for (Pair<String, String> entry : params.keyValuesView()) {
            output = output.replace(paramPrefix + entry.getOne() + paramSuffix, entry.getTwo());
        }

        return output;
    }

    public Function<String, String> tokenizeString() {
        return new Function<String, String>() {
            @Override
            public String valueOf(String input) {
                return tokenizeString(input);
            }
        };
    }
}
