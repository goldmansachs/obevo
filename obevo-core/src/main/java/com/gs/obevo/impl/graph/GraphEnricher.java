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

import org.eclipse.collections.api.RichIterable;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Sets up the input changes for deployment as a dependency graph so that the changes can be deployed later (e.g. in
 * topological-sort order or to detect groups of related objects).
 */
public interface GraphEnricher {
    /**
     * See {@link GraphEnricher} javadoc.
     *
     * @param inputs The inputs to sort
     * @param rollback Whether the ordering should be done assuming the changes are to be rolled back
     * (thus, reverse order for incremental changes)
     */
    <T extends SortableDependencyGroup> DirectedGraph<T, DefaultEdge> createDependencyGraph(RichIterable<T> inputs, boolean rollback);
}
