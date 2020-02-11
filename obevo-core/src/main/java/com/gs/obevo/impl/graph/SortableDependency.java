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

import java.util.Comparator;

import com.gs.obevo.api.appdata.ChangeKey;
import com.gs.obevo.api.appdata.CodeDependency;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 * Interface to expose the minimum required fields for the {@link com.gs.obevo.impl.changesorter.ChangeCommandSorter} to work. This is an interface
 * as we may need multiple kinds of objects to get sorted.
 */
public interface SortableDependency {
    /**
     * Returns the dependencies to use for the graph enrichment.
     */
    ImmutableSet<CodeDependency> getCodeDependencies();

    ChangeKey getChangeKey();

    /**
     * Returns the object order name for usage in the enricher index.
     */
    int getOrderWithinObject();

    /**
     * This is the recommended comparator to use for the graph sorter - for a topological sorting to the user that is
     * as friendly to read as possible.
     */
    Comparator<SortableDependency> GRAPH_SORTER_COMPARATOR = Comparators.chain(
            Comparators.fromFunctions(sortableDependency -> sortableDependency.getChangeKey().getObjectKey().getChangeType().getDeployOrderPriority()),
            Comparators.fromFunctions(sortableDependency -> sortableDependency.getChangeKey().getObjectKey().getSchema()),
            Comparators.fromFunctions(sortableDependency -> sortableDependency.getChangeKey().getObjectKey().getObjectName()),
            Comparators.fromFunctions(sortableDependency -> sortableDependency.getOrderWithinObject())
    );
}
