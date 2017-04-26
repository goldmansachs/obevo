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
package com.gs.obevo.db.impl.core.changetypes;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;

public class StaticDataInsertRow {
    private final ImmutableSortedMap<String, Object> params;

    public StaticDataInsertRow(ImmutableMap<String, Object> params) {
        this.params = new TreeSortedMap(params.toMap()).toImmutable();
    }

    public MutableList<String> getInsertColumns() {
        return this.params.keysView().toList();
    }

    public MutableList<Object> getParamVals() {
        return this.params.valuesView().toList();
    }

    public ImmutableSortedMap<String, Object> getParams() {
        return this.params;
    }
}
