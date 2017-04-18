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
package com.gs.obevo.impl.graph;

import java.util.Comparator;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;

public interface SortableDependencyGroup {
    Comparator<SortableDependencyGroup> GRAPH_SORTER_COMPARATOR = Comparators.byFunction(new Function<SortableDependencyGroup, SortableDependency>() {
        @Override
        public SortableDependency valueOf(SortableDependencyGroup sortableDependency) {
            return sortableDependency.getComponents().min(SortableDependency.GRAPH_SORTER_COMPARATOR);
        }
    }, SortableDependency.GRAPH_SORTER_COMPARATOR);

    ImmutableSet<SortableDependency> getComponents();
}
