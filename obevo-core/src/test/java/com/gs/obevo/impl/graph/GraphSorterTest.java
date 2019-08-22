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

import com.gs.obevo.api.appdata.ChangeKey;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.test.Verify;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphSorterTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final GraphSorter sorter = new GraphSorter();

    @Test
    public void testBasicOrdering() {
        String sp1 = "sp1";
        String sp2 = "sp2";
        String sp3 = "sp3";
        String sp4 = "sp4";
        String sp5 = "sp5";

        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        for (String vertex : Lists.mutable.with(sp1, sp2, sp3, sp4, sp5).toReversed()) {
            graph.addVertex(vertex);
        }

        graph.addEdge(sp1, sp5);
        graph.addEdge(sp3, sp5);
        graph.addEdge(sp2, sp1);
        graph.addEdge(sp5, sp4);

        ListIterable<String> sorted = sorter.sortChanges(graph);

        // First, compare the root topological order (i.e. ensure that the dependencies are respected)
        assertEquals(5, sorted.size());
        assertThat(sorted.indexOf(sp1), greaterThan(sorted.indexOf(sp2)));
        assertThat(sorted.indexOf(sp5), greaterThan(sorted.indexOf(sp1)));
        assertThat(sorted.indexOf(sp5), greaterThan(sorted.indexOf(sp3)));
        assertThat(sorted.indexOf(sp4), greaterThan(sorted.indexOf(sp5)));

        // Now check that we can achieve a consistent order too (for easier debuggability for clients)
        assertEquals(Lists.immutable.with(sp2, sp1, sp3, sp5, sp4), sorted);
    }

    @Test
    public void testBasicOrderingWithComparator() {
        SortableDependency sp1 = newVertex("sp1");
        SortableDependency sp2 = newVertex("sp2");
        SortableDependency sp3 = newVertex("sp3");
        SortableDependency sp4 = newVertex("sp4");
        SortableDependency sp5 = newVertex("sp5");

        Graph<SortableDependency, DefaultEdge> graph = new DefaultDirectedGraph<SortableDependency, DefaultEdge>(DefaultEdge.class);

        for (SortableDependency vertex : shuffledList(sp1, sp2, sp3, sp4, sp5)) {
            graph.addVertex(vertex);
        }

        graph.addEdge(sp1, sp5);
        graph.addEdge(sp3, sp5);
        graph.addEdge(sp2, sp1);
        graph.addEdge(sp5, sp4);

        ListIterable<SortableDependency> sorted = sorter.sortChanges(graph, Comparators.fromFunctions(new Function<SortableDependency, String>() {
            @Override
            public String valueOf(SortableDependency sortableDependency) {
                return sortableDependency.getChangeKey().getChangeName();
            }
        }));

        // First, compare the root topological order (i.e. ensure that the dependencies are respected)
        assertEquals(5, sorted.size());
        assertThat(sorted.indexOf(sp1), greaterThan(sorted.indexOf(sp2)));
        assertThat(sorted.indexOf(sp5), greaterThan(sorted.indexOf(sp1)));
        assertThat(sorted.indexOf(sp5), greaterThan(sorted.indexOf(sp3)));
        assertThat(sorted.indexOf(sp4), greaterThan(sorted.indexOf(sp5)));

        // Now check that we can achieve a consistent order too (for easier debuggability for clients)
        assertEquals(Lists.immutable.with(sp2, sp1, sp3, sp5, sp4), sorted);
    }

    @Test
    public void expectExceptionIfNonComparableElementsAreProvidedForSorting() {
        SortableDependency sp1 = newVertex("sp1");

        Graph<SortableDependency, DefaultEdge> graph = new DefaultDirectedGraph<SortableDependency, DefaultEdge>(DefaultEdge.class);

        for (SortableDependency vertex : shuffledList(sp1)) {
            graph.addVertex(vertex);
        }

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsortable graph elements");

        sorter.sortChanges(graph);
    }

    @Test
    public void testOrderingWithSubgraph() {
        String sp1 = "sp1";
        String sp2 = "sp2";
        String sp3 = "sp3";
        String sp4 = "sp4";
        String sp5 = "sp5";

        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        for (String vertex : shuffledList(sp1, sp2, sp3, sp4, sp5)) {
            graph.addVertex(vertex);
        }

        graph.addEdge(sp2, sp1);
        graph.addEdge(sp5, sp4);
        graph.addEdge(sp1, sp5);
        graph.addEdge(sp3, sp5);

        ImmutableList<String> sorted = sorter.sortChanges(graph, Lists.mutable.with(sp1, sp2, sp3));

        // First, compare the root topological order (i.e. ensure that the dependencies are respected)
        assertEquals(3, sorted.size());
        assertThat(sorted.indexOf(sp1), greaterThan(sorted.indexOf(sp2)));

        // Now check that we can achieve a consistent order too (for easier debuggability for clients)
        assertEquals(Lists.immutable.with(sp2, sp1, sp3), sorted);
    }

    @Test
    public void testCycleDetection() {
        SortableDependency sp1 = newVertex("sp1");
        SortableDependency sp2 = newVertex("sp2");
        SortableDependency sp3 = newVertex("sp3");
        SortableDependency sp4 = newVertex("sp4");
        SortableDependency sp5 = newVertex("sp5");
        SortableDependency sp6 = newVertex("sp6");
        SortableDependency sp7 = newVertex("sp7");
        SortableDependency sp8 = newVertex("sp8");

        Graph<SortableDependency, DefaultEdge> graph = new DefaultDirectedGraph<SortableDependency, DefaultEdge>(DefaultEdge.class);
        for (SortableDependency vertex : shuffledList(sp1, sp2, sp3, sp4, sp5, sp6, sp7, sp8)) {
            graph.addVertex(vertex);
        }

        graph.addEdge(sp2, sp1);
        graph.addEdge(sp5, sp4);
        graph.addEdge(sp1, sp5);
        graph.addEdge(sp3, sp5);
        graph.addEdge(sp4, sp5);
        graph.addEdge(sp6, sp5);
        graph.addEdge(sp7, sp6);
        graph.addEdge(sp8, sp7);
        graph.addEdge(sp6, sp8);

        try {
            sorter.sortChanges(graph);
            fail("Expecting exception here: " + GraphCycleException.class);
        } catch (GraphCycleException e) {
            verifyCycleExists(e, Sets.immutable.with("sp4", "sp5"));
            verifyCycleExists(e, Sets.immutable.with("sp6", "sp7", "sp8"));
            Verify.assertSize(2, e.getCycleComponents());
        }
    }

    private void verifyCycleExists(GraphCycleException e, final ImmutableSet<String> cycleVertices) {
        Verify.assertAnySatisfy(e.<SortableDependency>getCycleComponents(), each ->
                CollectionAdapter.wrapSet(each).collect(sortableDependency ->
                        sortableDependency.getChangeKey().getChangeName()).equals(cycleVertices));
    }

    private static SortableDependency newVertex(String vertexName) {
        ChangeType changeType = mock(ChangeType.class);
        SortableDependency vertex = mock(SortableDependency.class);
        when(vertex.getChangeKey()).thenReturn(new ChangeKey("sch1", changeType, "obj1", vertexName));
        when(vertex.toString()).thenReturn(vertexName);
        return vertex;
    }

    /**
     * We shuffle the list to test out the fact that we can guarantee a particular order of traversal.
     */
    private static <T> MutableList<T> shuffledList(T... inputs) {
        return ArrayAdapter.adapt(inputs).shuffleThis();
    }
}
