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
package com.gs.obevo.util.lookuppredicate;

import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Does a hash-set lookup against the given list of inputs. Leverage this for a faster lookup if possible.
 */
public class LookupIndex implements Index {
    private final ImmutableSet<String> values;

    public LookupIndex(ImmutableSet<String> values) {
        this.values = values;
    }

    @Override
    public boolean accept(String each) {
        return values.contains(each);
    }

    @Override
    public String toString() {
        return "LookupIndex{" +
                "values=" + values +
                '}';
    }
}
