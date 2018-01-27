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
package com.gs.obevo.impl.graph;

import java.util.Set;

import org.eclipse.collections.api.list.ListIterable;

/**
 * Note: Exception classes cannot be generic, so we need the hack in the constructor and getter to get this to compile.
 */
class GraphCycleException extends IllegalArgumentException {
    private final String message;
    private final ListIterable<Set<?>> cycleComponents;

    <T> GraphCycleException(String message, ListIterable<Set<T>> cycleComponents) {
        this.message = message;
        this.cycleComponents = (ListIterable) cycleComponents;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public <T> ListIterable<Set<T>> getCycleComponents() {
        return (ListIterable<Set<T>>) (ListIterable) cycleComponents;
    }
}
