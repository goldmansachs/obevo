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
/*
 * The getCycleComponents method leveraged some code from JGraphT, which is EPL.
 *
 * The Eclipse Public License is available at:
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.gs.obevo.impl.graph;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.IteratorUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.traverse.DepthFirstIterator;

/**
 * Utility class to work w/ graphs in the JGraphT library. There are a couple usages that need syntax sugar...
 */
public final class GraphUtil {
    private GraphUtil() {
    }

    public static <T> SetIterable<T> getDependentNodes(final DirectedGraph<T, DefaultEdge> graph, T vertex) {
        return SetAdapter.adapt(graph.outgoingEdgesOf(vertex)).collect(graph::getEdgeTarget);
    }

    public static <T> SetIterable<T> getDependencyNodes(final DirectedGraph<T, DefaultEdge> graph, T vertex) {
        return SetAdapter.adapt(graph.incomingEdgesOf(vertex)).collect(graph::getEdgeSource);
    }

    public static <T, E> SetIterable<Pair<T, E>> getDependencyNodesAndEdges(final DirectedGraph<T, E> graph, T vertex) {
        return SetAdapter.adapt(graph.incomingEdgesOf(vertex)).collect(edge -> Tuples.pair(graph.getEdgeSource(edge), edge));
    }

    public static <T> void validateNoCycles(final DirectedGraph<T, DefaultEdge> graph) {
        validateNoCycles(graph, Functions.getToString(), null);
    }

    public static <T, E> void validateNoCycles(final DirectedGraph<T, E> graph, final Function<? super T, String> vertexToString, final Function<? super E, String> edgeToString) {
        ListIterable<Set<T>> cycleComponents = getCycleComponents(graph);

        if (!cycleComponents.isEmpty()) {
            final AtomicInteger cycleCounter = new AtomicInteger(0);
            ListIterable<String> cycleMessages = cycleComponents.collect(cycleComponent -> {
                final StringBuilder sb = new StringBuilder();
                final DirectedSubgraph<T, E> cycleSubgraph = new DirectedSubgraph<T, E>(graph, cycleComponent, null);
                DepthFirstIterator<T, E> iterator = new DepthFirstIterator<T, E>(cycleSubgraph) {
                    boolean started = false;
                    boolean afterCycle = false;

                    @Override
                    protected void encounterVertex(T vertex, E edge) {
                        if (!started) {
                            sb.append("Cycle #" + cycleCounter.incrementAndGet() + ":");
                            sb.append("\n    " + vertexToString.valueOf(vertex));
                            started = true;
                        } else {
                            if (afterCycle) {
                                afterCycle = false;
                                sb.append("\nCycle #" + cycleCounter.incrementAndGet() + ":");
                                sb.append("\n    " + vertexToString.valueOf(cycleSubgraph.getEdgeSource(edge)));
                            }
                            sb.append("\n    => ").append(vertexToString.valueOf(vertex)).append(" (").append(edge).append(")");
                        }
                        super.encounterVertex(vertex, edge);
                    }

                    @Override
                    protected void encounterVertexAgain(T vertex, E edge) {
                        sb.append("\n    => ").append(vertexToString.valueOf(vertex)).append(" (").append(edge).append(") (CYCLE FORMED)");
                        afterCycle = true;
                        super.encounterVertexAgain(vertex, edge);
                    }
                };

                IteratorUtils.toList(iterator);  // force iteration through the list

                return sb.toString();
            });

            throw new GraphCycleException(
                    "Found cycles for the changes below. Please correct the object content.\n" +
                            "You can remediate by:\n" +
                            "    A) manually excluding false dependencies using //// METADATA excludeDependencies=A,B,C or\n" +
                            "    B) defining appropriate dependencies using the METADATA includeDependencies or dependencies attributes\n" +
                            "It is helpful to analyze the EXPLICIT dependency types.\n" +
                            "\n" +
                            "Changes are marked as [objectName.changeName]\n" +
                            "Dependency Types: EXPLICIT = marked in code, IMPLICIT = from implied deploy order within incremental table changes\n" +
                            "\n" +
                            cycleMessages.makeString("\n"), cycleComponents);
        }
    }

    /**
     * Returns the components of the graph that are cycles.
     * Taken from the implementation of {@link CycleDetector#findCycles()}. (EPL)
     */
    private static <T, E> ListIterable<Set<T>> getCycleComponents(final DirectedGraph<T, E> graph) {
        StrongConnectivityInspector<T, E> inspector =
                new StrongConnectivityInspector<T, E>(graph);

        return ListAdapter.adapt(inspector.stronglyConnectedSets()).select(new Predicate<Set<T>>() {
            @Override
            public boolean accept(Set<T> each) {
                if (each.size() > 1) {
                    // multi-vertex strongly-connected component is a cycle
                    return true;
                }

                // vertex with an edge to itself is a cycle
                T vertex = each.iterator().next();
                return graph.containsEdge(vertex, vertex);
            }
        });
    }
}
