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

import java.util.Objects;

import com.gs.obevo.api.appdata.CodeDependency;
import com.gs.obevo.api.appdata.CodeDependencyType;
import com.gs.obevo.api.platform.ChangeType;
import kotlin.jvm.functions.Function1;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created a graph out of the input changes, so that the graph can be shared by other components
 */
public class GraphEnricherImpl implements GraphEnricher {
    private static final Logger LOG = LoggerFactory.getLogger(GraphEnricherImpl.class);

    private final Function<String, String> convertDbObjectName;

    public GraphEnricherImpl(Function<String, String> convertDbObjectName) {
        this.convertDbObjectName = Objects.requireNonNull(convertDbObjectName);
    }

    @Override
    public <T extends SortableDependencyGroup> Graph<T, DefaultEdge> createDependencyGraph(Iterable<? extends T> inputs2, boolean rollback) {
        RichIterable<T> inputs = Lists.mutable.withAll(inputs2);

        final RichIterable<ChangeIndex<T>> changeIndexes = Lists.mutable.of(
                new ObjectIndex<T>(),
                new SchemaObjectIndex<T>(),
                new ObjectChangeIndex<T>(),
                new SchemaChangeObjectIndex<T>()
        );

        for (ChangeIndex<T> changeIndex : changeIndexes) {
            for (T change : inputs) {
                changeIndex.add(change);
            }
        }

        final DefaultDirectedGraph<T, DefaultEdge> graph = new DefaultDirectedGraph<T, DefaultEdge>(DefaultEdge.class);

        // First - add the core objects to the graph
        for (T change : inputs) {
            graph.addVertex(change);
        }

        // Now add the declared dependencies to the graph
        for (T changeGroup : inputs) {
            for (SortableDependency change : changeGroup.getComponents()) {
                if (change.getCodeDependencies() != null) {
                    for (CodeDependency dependency : change.getCodeDependencies()) {
                        T dependencyVertex = null;
                        for (ChangeIndex<T> changeIndex : changeIndexes) {
                            dependencyVertex = changeIndex.retrieve(change.getChangeKey().getObjectKey().getSchema(), dependency.getTarget());
                            if (dependencyVertex != null) {
                                if (LOG.isTraceEnabled()) {
                                    LOG.trace("Discovered dependency from {} to {} using index {}",
                                            dependencyVertex,
                                            change.getChangeKey(),
                                            changeIndex);
                                }
                                break;
                            }
                        }

                        if (dependencyVertex == null) {
                            LOG.trace("Dependency not found; likely due to not enriching the full graph in source. Should be OK to ignore: {} - {}", dependency, change);
                        } else {
                            graph.addEdge(dependencyVertex, changeGroup, new DependencyEdge(dependencyVertex, changeGroup, dependency.getCodeDependencyType()));
                        }
                    }
                }
            }
        }

        // Add in changes within incremental files to ensure proper order
        RichIterable<Pair<T, SortableDependency>> groupToComponentPairs = inputs.flatCollect(new Function<T, Iterable<Pair<T, SortableDependency>>>() {
            @Override
            public Iterable<Pair<T, SortableDependency>> valueOf(T group) {
                return group.getComponents().collect(Functions.pair(Functions.getFixedValue(group), Functions.<SortableDependency>getPassThru()));
            }
        });

        final Multimap<String, Pair<T, SortableDependency>> incrementalChangeByObjectMap = groupToComponentPairs.groupBy(new Function<Pair<T, SortableDependency>, String>() {
            @Override
            public String valueOf(Pair<T, SortableDependency> pair) {
                SortableDependency tSortMetadata = pair.getTwo();
                String changeType = tSortMetadata.getChangeKey().getObjectKey().getChangeType().getName();
                if (changeType.equals(ChangeType.TRIGGER_INCREMENTAL_OLD_STR) || changeType.equals(ChangeType.FOREIGN_KEY_STR)) {
                    changeType = ChangeType.TABLE_STR;
                }
                return changeType + ":" + tSortMetadata.getChangeKey().getObjectKey().getSchema() + ":" + convertDbObjectName.valueOf(tSortMetadata.getChangeKey().getObjectKey().getObjectName());
            }
        });

        for (RichIterable<Pair<T, SortableDependency>> sortMetadataPairs : incrementalChangeByObjectMap.multiValuesView()) {
            final MutableList<Pair<T, SortableDependency>> sortedChanges = sortMetadataPairs.toSortedListBy(Functions.chain(Functions.<SortableDependency>secondOfPair(), new Function<SortableDependency, Integer>() {
                @Override
                public Integer valueOf(SortableDependency sortableDependency) {
                    return sortableDependency.getOrderWithinObject();
                }
            }));
            if (sortedChanges.size() > 1) {
                if (rollback) {
                    sortedChanges.forEachWithIndex(0, sortedChanges.size() - 2, new ObjectIntProcedure<Pair<T, SortableDependency>>() {
                        @Override
                        public void value(Pair<T, SortableDependency> each, int index) {
                            // for rollback, we go in reverse-order (each change follows the one after it in the file)
                            Pair<T, SortableDependency> nextChange = sortedChanges.get(index + 1);

                            graph.addEdge(nextChange.getOne(), each.getOne(), new DependencyEdge(nextChange.getOne(), each.getOne(), CodeDependencyType.IMPLICIT));
                        }
                    });
                } else {
                    sortedChanges.forEachWithIndex(1, sortedChanges.size() - 1, new ObjectIntProcedure<Pair<T, SortableDependency>>() {
                        @Override
                        public void value(Pair<T, SortableDependency> each, int index) {
                            // for regular mode, we go in regular-order (each change follows the one before it in the file)
                            Pair<T, SortableDependency> previousChange = sortedChanges.get(index - 1);

                            graph.addEdge(previousChange.getOne(), each.getOne(), new DependencyEdge(previousChange.getOne(), each.getOne(), CodeDependencyType.IMPLICIT));
                        }
                    });
                }
            }
        }

        // validate
        GraphUtil.validateNoCycles(graph,
                new Function<T, String>() {
                    @Override
                    public String valueOf(T t) {
                        return t.getComponents().collect(new Function<SortableDependency, String>() {
                            @Override
                            public String valueOf(SortableDependency sortableDependency) {
                                return "[" + sortableDependency.getChangeKey().getObjectKey().getObjectName() + "." + sortableDependency.getChangeKey().getChangeName() + "]";
                            }
                        }).makeString(", ");
                    }
                },
                new Function<DefaultEdge, String>() {
                    @Override
                    public String valueOf(DefaultEdge dependencyEdge) {
                        return "-" + ((DependencyEdge) dependencyEdge).getEdgeType();
                    }
                });

        return graph;
    }

    @NotNull
    @Override
    public <T> Graph<T, DefaultEdge> createSimpleDependencyGraph(@NotNull Iterable<? extends T> inputs, @NotNull Function1<? super T, ? extends Iterable<? extends T>> edgesFunction) {
        final DefaultDirectedGraph<T, DefaultEdge> graph = new DefaultDirectedGraph<T, DefaultEdge>(DefaultEdge.class);
        for (T input : inputs) {
            graph.addVertex(input);
        }

        for (T input : inputs) {
            Iterable<? extends T> targetVertices = edgesFunction.invoke(input);
            for (T targetVertex : targetVertices) {
                if (graph.containsVertex(targetVertex)) {
                    graph.addEdge(targetVertex, input);
                } else {
                    LOG.info("Problem?");
                }
            }
        }

        return graph;
    }

    private interface ChangeIndex<T> {
        void add(T change);

        T retrieve(String schema, String dependency);
    }

    /**
     * Looks for the given dependency/object
     */
    private class ObjectIndex<T extends SortableDependencyGroup> implements ChangeIndex<T> {
        private final MutableMap<Pair<String, String>, T> schemaToObjectMap = Maps.mutable.empty();

        @Override
        public void add(T changeGroup) {
            for (SortableDependency change : changeGroup.getComponents()) {
                T existingChange = retrieve(change.getChangeKey().getObjectKey().getSchema(), convertDbObjectName.valueOf(change.getChangeKey().getObjectKey().getObjectName()));
                // TODO getFirst is not ideal here
                if (existingChange == null || existingChange.getComponents().getFirst().getOrderWithinObject() < change.getOrderWithinObject()) {
                    // only keep the latest (why latest vs earliest?)
                    schemaToObjectMap.put(Tuples.pair(change.getChangeKey().getObjectKey().getSchema(), convertDbObjectName.valueOf(change.getChangeKey().getObjectKey().getObjectName())), changeGroup);
                }
            }
        }

        @Override
        public T retrieve(String schema, String dependency) {
            return (T) schemaToObjectMap.get(Tuples.pair(schema, convertDbObjectName.valueOf(dependency)));
        }
    }

    private class SchemaObjectIndex<T extends SortableDependencyGroup> implements ChangeIndex<T> {
        private final MutableMap<String, T> objectMap = UnifiedMap.newMap();

        @Override
        public void add(T changeGroup) {
            for (SortableDependency change : changeGroup.getComponents()) {
                T existingChange = retrieve(change.getChangeKey().getObjectKey().getSchema(), convertDbObjectName.valueOf(change.getChangeKey().getObjectKey().getObjectName()));
                // TODO getFirst is not ideal here
                if (existingChange == null || existingChange.getComponents().getFirst().getOrderWithinObject() < change.getOrderWithinObject()) {
                    // only keep the latest (why latest vs earliest?)
                    objectMap.put(convertDbObjectName.valueOf(change.getChangeKey().getObjectKey().getSchema() + "." + change.getChangeKey().getObjectKey().getObjectName()), changeGroup);
                }
            }
        }

        @Override
        public T retrieve(String schema, String dependency) {
            return objectMap.get(convertDbObjectName.valueOf(dependency));
        }
    }

    private class ObjectChangeIndex<T extends SortableDependencyGroup> implements ChangeIndex<T> {
        private final MutableMap<Pair<String, String>, T> schemaToObjectMap = Maps.mutable.empty();

        @Override
        public void add(T changeGroup) {
            for (SortableDependency change : changeGroup.getComponents()) {
                schemaToObjectMap.put(Tuples.pair(change.getChangeKey().getObjectKey().getSchema(), convertDbObjectName.valueOf(change.getChangeKey().getObjectKey().getObjectName() + "." + change.getChangeKey().getChangeName())), changeGroup);
            }
        }

        @Override
        public T retrieve(String schema, String dependency) {
            return (T) schemaToObjectMap.get(Tuples.pair(schema, convertDbObjectName.valueOf(dependency)));
        }
    }

    private class SchemaChangeObjectIndex<T extends SortableDependencyGroup> implements ChangeIndex<T> {
        private final MutableMap<String, T> objectMap = UnifiedMap.newMap();

        @Override
        public void add(T changeGroup) {
            for (SortableDependency change : changeGroup.getComponents()) {
                objectMap.put(convertDbObjectName.valueOf(change.getChangeKey().getObjectKey().getSchema() + "." + change.getChangeKey().getObjectKey().getObjectName() + "." + change.getChangeKey().getChangeName()), changeGroup);
            }
        }

        @Override
        public T retrieve(String schema, String dependency) {
            return objectMap.get(convertDbObjectName.valueOf(dependency));
        }
    }

    /**
     * Custom edge type to allow for better error logging for cycles, namely to show the dependency edge type.
     */
    private static class DependencyEdge<T extends SortableDependencyGroup> extends DefaultEdge {
        private final T source;
        private final T target;
        private final CodeDependencyType edgeType;

        DependencyEdge(T source, T target, CodeDependencyType edgeType) {
            this.source = source;
            this.target = target;
            this.edgeType = edgeType;
        }

        @Override
        public T getSource() {
            return source;
        }

        @Override
        public T getTarget() {
            return target;
        }

        CodeDependencyType getEdgeType() {
            return edgeType;
        }

        @Override
        public String toString() {
            return edgeType.name();
        }
    }
}
