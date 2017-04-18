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
package com.gs.obevo.dbmetadata.deepcompare;

import org.eclipse.collections.api.block.function.Function;

public abstract class CompareBreak {
    public static final Function<CompareBreak, Class> TO_CLAZZ = new Function<CompareBreak, Class>() {
        @Override
        public Class valueOf(CompareBreak object) {
            return object.getClazz();
        }
    };

    public static final Function<CompareBreak, String> TO_COMPARE_SUBJECT = new Function<CompareBreak, String>() {
        @Override
        public String valueOf(CompareBreak object) {
            return object.getCompareSubject();
        }
    };

    private final Class clazz;

    protected CompareBreak(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return this.clazz;
    }

    public abstract String getCompareSubject();
}
