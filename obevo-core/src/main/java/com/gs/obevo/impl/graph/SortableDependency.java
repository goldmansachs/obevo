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

import com.gs.obevo.api.appdata.CodeDependency;
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.block.factory.Functions;

/**
 * Interface to expose the minimum required fields for the {@link com.gs.obevo.impl.changesorter.ChangeCommandSorter} to work. This is an interface
 * as we may need multiple kinds of objects to get sorted.
 */
public interface SortableDependency {
    /**
     * Returns the dependencies to use for the graph enrichment.
     */
    ImmutableSet<CodeDependency> getCodeDependencies();

    ObjectKey getObjectKey();

    /**
     * Returns the change name for usage in the enricher index.
     */
    String getChangeName();

    /**
     * Returns the object order name for usage in the enricher index.
     */
    int getOrderWithinObject();

    Function<SortableDependency, String> TO_SCHEMA = new Function<SortableDependency, String>() {
        @Override
        public String valueOf(SortableDependency arg0) {
            return arg0.getObjectKey().getSchema();
        }
    };

    Function<SortableDependency, ChangeType> TO_CHANGE_TYPE = new Function<SortableDependency, ChangeType>() {
        @Override
        public ChangeType valueOf(SortableDependency arg0) {
            return arg0.getObjectKey().getChangeType();
        }
    };

    Function<SortableDependency, String> TO_CHANGE_NAME = new Function<SortableDependency, String>() {
        @Override
        public String valueOf(SortableDependency arg0) {
            return arg0.getChangeName();
        }
    };

    Function<SortableDependency, String> TO_OBJECT_NAME = new Function<SortableDependency, String>() {
        @Override
        public String valueOf(SortableDependency arg0) {
            return arg0.getObjectKey().getObjectName();
        }
    };

    Function<SortableDependency, Integer> TO_ORDER_WITHIN_OBJECT = new Function<SortableDependency, Integer>() {
        @Override
        public Integer valueOf(SortableDependency arg0) {
            return arg0.getOrderWithinObject();
        }
    };

    /**
     * This is the recommended comparator to use for the graph sorter - for a topological sorting to the user that is
     * as friendly to read as possible.
     */
    Comparator<SortableDependency> GRAPH_SORTER_COMPARATOR = Comparators.chain(
            Comparators.fromFunctions(Functions.chain(TO_CHANGE_TYPE, ChangeType.TO_DEPLOY_ORDER_PRIORITY)),
            Comparators.fromFunctions(TO_SCHEMA),
            Comparators.fromFunctions(TO_OBJECT_NAME),
            Comparators.fromFunctions(TO_ORDER_WITHIN_OBJECT)
    );
}
