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
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.collections.IteratorUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * Iterates through the inputs and graph to come up w/ a proper topological sorting.
 *
 * We expect that the graph elements should either be Comparable, or a Comparator be provided. This is to guarantee a
 * consistent topological order, which is much friendlier for clients to debug and for consistency across different
 * environments.
 *
 * (Note that on a given graph, we could have many valid topological orders, which is what we want to get consistency
 * on - see https://en.wikipedia.org/wiki/Topological_sorting).
 */
public class GraphSorter {
    /**
     * Sorts the graph to provide a consistent topological ordering. The vertices of the graph must implement {@link Comparable}
     *
     * @param graph The input graph
     * @param subsetVertices The subset vertices of the graph we want to sort
     */
    public <T> ImmutableList<T> sortChanges(DirectedGraph<T, DefaultEdge> graph, RichIterable<T> subsetVertices) {
        return sortChanges(graph, subsetVertices, null);
    }

    /**
     * Sorts the graph to provide a consistent topological ordering.
     *
     * @param graph The input graph
     * @param subsetVertices The subset vertices of the graph we want to sort
     * @param comparator The comparator on which to order the vertices to guarantee a consistent topological ordering
     */
    public <T> ImmutableList<T> sortChanges(DirectedGraph<T, DefaultEdge> graph, RichIterable<T> subsetVertices, Comparator<? super T> comparator) {
        if (subsetVertices.toSet().size() != subsetVertices.size()) {
            throw new IllegalStateException("Unexpected state - have some dupe elements here: " + subsetVertices);
        }

        DirectedGraph<T, DefaultEdge> subsetGraph = new DirectedSubgraph<T, DefaultEdge>(
                graph, subsetVertices.toSet(), null);

        // At one point, we _thought_ that the DirectedSubGraph was dropping vertices that don't have edges, so we
        // manually add them back to the graph to ensure that we can still order them.
        // However, that no longer seems to be the case. We add a check here just in case this comes up again.
        if (subsetVertices.size() != subsetGraph.vertexSet().size()) {
            throw new IllegalArgumentException("This case should never happen! [subsetVertices: " + subsetVertices + ", subsetGraphVertices: " + subsetGraph.vertexSet());
        }

        return sortChanges(subsetGraph, comparator);
    }

    /**
     * Sorts the graph to provide a consistent topological ordering. The vertices of the graph must implement {@link Comparable}
     *
     * @param graph The input graph - all vertices in the graph will be returned in the output list
     */
    public <T> ImmutableList<T> sortChanges(final DirectedGraph<T, DefaultEdge> graph) {
        return sortChanges(graph, (Comparator<T>) null);
    }

    /**
     * Sorts the graph to provide a consistent topological ordering.
     *
     * @param graph The input graph - all vertices in the graph will be returned in the output list
     * @param comparator The comparator on which to order the vertices to guarantee a consistent topological ordering
     */
    public <T> ImmutableList<T> sortChanges(final DirectedGraph<T, DefaultEdge> graph, Comparator<? super T> comparator) {
        if (graph.vertexSet().isEmpty()) {
            return Lists.immutable.empty();
        }

        GraphUtil.validateNoCycles(graph);

        TopologicalOrderIterator<T, DefaultEdge> iterator = getTopologicalOrderIterator(graph, comparator);

        return Lists.immutable.withAll(IteratorUtils.toList(iterator));
    }

    private <T> TopologicalOrderIterator<T, DefaultEdge> getTopologicalOrderIterator(DirectedGraph<T, DefaultEdge> graph, Comparator<? super T> comparator) {
        if (comparator != null) {
            Queue<T> queue = new PriorityQueue<T>(10, comparator);
            return new TopologicalOrderIterator<T, DefaultEdge>(graph, queue);
        } else if (graph.vertexSet().iterator().next() instanceof Comparable) {
            Queue<T> queue = new PriorityQueue<T>();
            return new TopologicalOrderIterator<T, DefaultEdge>(graph, queue);
        } else {
            throw new IllegalArgumentException("Unsortable graph elements - either need to provide a Comparator or have Comparable vertices to guarantee a consistent topological order");
        }
    }
}
