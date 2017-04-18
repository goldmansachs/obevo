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

import java.util.Set;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;

/**
 * Utility class to work w/ graphs in the JGraphT library. There are a couple usages that need syntax sugar...
 */
public class GraphUtil {
    public static <T> SetIterable<T> getDependentNodes(final DirectedGraph<T, DefaultEdge> graph, T vertex) {
        return SetAdapter.adapt(graph.outgoingEdgesOf(vertex)).collect(new Function<DefaultEdge, T>() {
            @Override
            public T valueOf(DefaultEdge edge) {
                return graph.getEdgeTarget(edge);
            }
        });
    }

    public static <T> SetIterable<T> getDependencyNodes(final DirectedGraph<T, DefaultEdge> graph, T vertex) {
        return SetAdapter.adapt(graph.incomingEdgesOf(vertex)).collect(new Function<DefaultEdge, T>() {
            @Override
            public T valueOf(DefaultEdge edge) {
                return graph.getEdgeSource(edge);
            }
        });
    }

    public static <T, E> SetIterable<Pair<T, E>> getDependencyNodesAndEdges(final DirectedGraph<T, E> graph, T vertex) {
        return SetAdapter.adapt(graph.incomingEdgesOf(vertex)).collect(new Function<E, Pair<T, E>>() {
            @Override
            public Pair<T, E> valueOf(E edge) {
                return Tuples.pair(graph.getEdgeSource(edge), edge);
            }
        });
    }

    public static <T> void validateNoCycles(final DirectedGraph<T, DefaultEdge> graph) {
        validateNoCycles(graph, Functions.getToString(), null);
    }

    public static <T, E> void validateNoCycles(DirectedGraph<T, E> graph, final Function<? super T, String> vertexToString, final Function<? super E, String> edgeToString) {
        CycleDetector detector = new CycleDetector(graph);
        if (detector.detectCycles()) {
            final MutableSet<T> cycleVertices = SetAdapter.adapt((Set<T>) detector.findCycles());
            final DirectedSubgraph<T, E> cycleSubgraph = new DirectedSubgraph<T, E>(graph, cycleVertices, null);

            String cycleChangeString = SetAdapter.adapt(cycleSubgraph.vertexSet()).toSortedListBy(vertexToString).collect(new Function<T, String>() {
                @Override
                public String valueOf(final T vertex) {
                    SetIterable<Pair<T, E>> dependencyNodesAndEdges = getDependencyNodesAndEdges(cycleSubgraph, vertex);
                    return vertexToString.valueOf(vertex) + " ==> " + dependencyNodesAndEdges.collect(new Function<Pair<T,E>, String>() {
                        @Override
                        public String valueOf(Pair<T, E> tePair) {
                            String str = vertexToString.valueOf(tePair.getOne());
                            if (edgeToString != null) {
                                str += edgeToString.valueOf(tePair.getTwo());
                            }
                            return str;
                        }
                    }).toSortedList().makeString(",");
                }
            }).makeString("\n");
            throw new IllegalArgumentException(
                    String.format(
                            "Found cycles for the changes below. Please correct the object content - helpful to analyze the EXPLICIT dependency types. You can remediate by A) manually excluding false dependencies using //// METADATA excludeDependencies=A,B,C or  B) defining appropriate dependencies using the METADATA includeDependencies or dependencies attributes\n%s",
                            cycleChangeString));
        }
    }
}
