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

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.HawickJamesSimpleCycles;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.RandomWalkIterator;

/**
 * Utility class to work w/ graphs in the JGraphT library. There are a couple usages that need syntax sugar...
 */
public final class GraphUtil {
    private GraphUtil() {
    }

    public static <T> SetIterable<T> getDependentNodes(final Graph<T, DefaultEdge> graph, T vertex) {
        return SetAdapter.adapt(graph.outgoingEdgesOf(vertex)).collect(new Function<DefaultEdge, T>() {
            @Override
            public T valueOf(DefaultEdge e) {
                return graph.getEdgeTarget(e);
            }
        });
    }

    public static <T> SetIterable<T> getDependencyNodes(final Graph<T, DefaultEdge> graph, T vertex) {
        return SetAdapter.adapt(graph.incomingEdgesOf(vertex)).collect(new Function<DefaultEdge, T>() {
            @Override
            public T valueOf(DefaultEdge e) {
                return graph.getEdgeSource(e);
            }
        });
    }

    public static <T, E> SetIterable<Pair<T, E>> getDependencyNodesAndEdges(final Graph<T, E> graph, T vertex) {
        return SetAdapter.adapt(graph.incomingEdgesOf(vertex)).collect(new Function<E, Pair<T, E>>() {
            @Override
            public Pair<T, E> valueOf(E edge) {
                return Tuples.pair(graph.getEdgeSource(edge), edge);
            }
        });
    }

    public static <T> void validateNoCycles(final Graph<T, DefaultEdge> graph) {
        validateNoCycles(graph, Functions.getToString(), null);
    }

    public static <T, E> void validateNoCycles(final Graph<T, E> graph, final Function<? super T, String> vertexToString, final Function<? super E, String> edgeToString) {
        DirectedSimpleCycles<T, E> simpleCycles = new HawickJamesSimpleCycles<>(graph);
        List<List<T>> cycleComponents = simpleCycles.findSimpleCycles();

        if (!cycleComponents.isEmpty()) {
            final AtomicInteger cycleCounter = new AtomicInteger(0);
            ListIterable<String> cycleMessages = ListAdapter.adapt(cycleComponents).collect(cycleComponent -> {
                AsSubgraph<T, E> subgraph = new AsSubgraph<>(graph, Sets.mutable.ofAll(cycleComponent));
                Set<T> visitedVertices = Sets.mutable.empty();

                final StringBuilder sb = new StringBuilder();
                sb.append("Cycle #" + cycleCounter.incrementAndGet() + ":");

                RandomWalkIterator<T, E> teRandomWalkIterator = new RandomWalkIterator<>(subgraph);
                while (teRandomWalkIterator.hasNext()) {
                    T next = teRandomWalkIterator.next();
                    if (!visitedVertices.contains(next)) {
                        sb.append("\n    " + vertexToString.valueOf(next) + "   =>");
                        visitedVertices.add(next);
                    } else {
                        sb.append("\n    " + vertexToString.valueOf(next) + " (CYCLE FORMED)");
                        break;
                    }
                }
                return sb.toString();
            }).toList();

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
     * Taken from the implementation of CycleDetector#findCycles(). (EPL)
     */
    private static <T, E> ListIterable<Set<T>> getCycleComponents(final Graph<T, E> graph) {
        StrongConnectivityAlgorithm<T, E> inspector =
                new KosarajuStrongConnectivityInspector<>(graph);

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
