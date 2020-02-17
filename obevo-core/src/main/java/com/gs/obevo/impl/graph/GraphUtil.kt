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
package com.gs.obevo.impl.graph

import org.jgrapht.Graph
import org.jgrapht.alg.cycle.HawickJamesSimpleCycles
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.EdgeReversedGraph

/**
 * Utility class to work w/ graphs in the JGraphT library. There are a couple usages that need syntax sugar...
 */
object GraphUtil {
    @JvmStatic
    fun <T> getDependentNodes(graph: Graph<T, DefaultEdge>, vertex: T): Set<T> {
        return graph.outgoingEdgesOf(vertex).mapTo(mutableSetOf(), graph::getEdgeTarget)
    }

    @JvmStatic
    fun <T> getDependencyNodes(graph: Graph<T, DefaultEdge>, vertex: T): Set<T> {
        return graph.incomingEdgesOf(vertex).mapTo(mutableSetOf(), graph::getEdgeSource)
    }

    @JvmStatic
    fun <T> validateNoCycles(graph: Graph<T, DefaultEdge>) {
        validateNoCycles(graph, { v, _, _ -> v.toString() }, DefaultEdge::toString)
    }


    @JvmStatic
    fun <T, E> validateNoCycles(graph: Graph<T, E>, vertexToString: VertexToString<in T, in E>, edgeToString: Function1<in E, String>) {
        val simpleCycles = HawickJamesSimpleCycles(graph)
        val cycleComponents = simpleCycles.findSimpleCycles()
        cycleComponents.zipWithNext()

        if (cycleComponents.isNotEmpty()) {
            val cycleMessages = cycleComponents.mapIndexed { cycleCounter, cycleComponent ->
                val subgraph = EdgeReversedGraph(AsSubgraph(graph, cycleComponent.toSet()))
                val visitedVertices = mutableSetOf<T>()

                val sb = StringBuilder()
                sb.append("Cycle #$cycleCounter:")

                val walkIterator = RandomWalkEdgeIterator(subgraph)
                var prevVertex = walkIterator.nextEdge().first
                visitedVertices.add(prevVertex)
                while (walkIterator.hasNext()) {
                    val nextEdge = walkIterator.nextEdge()
                    val nextVertex = nextEdge.first
                    sb.append("\n    " + vertexToString(prevVertex, false, nextEdge.second) + " == depends on ==> " + vertexToString(nextVertex, true, nextEdge.second) + "   (" + edgeToString(nextEdge.second) + " dependency)")
                    if (!visitedVertices.contains(nextVertex)) {
                        visitedVertices.add(nextVertex)
                    } else {
                        sb.append(" (CYCLE FORMED)")
                        break
                    }
                    prevVertex = nextVertex
                }
                sb.toString()
            }

            throw GraphCycleException(
                    "Found cycles for the changes below. Please correct the object content.\n" +
                            "You can remediate by:\n" +
                            "    A) manually excluding false dependencies (likely the DISCOVERED dependencies) using //// METADATA excludeDependencies=A,B,C or\n" +
                            "    B) defining appropriate dependencies using the METADATA includeDependencies or dependencies attributes\n" +
                            "    C) excluding unnecessary EXPLICIT dependency types\n" +
                            "\n" +
                            "Changes are marked as [objectName.changeName]\n" +
                            "\n" +
                            "Overview of dependency types:\n" +
                            " * DISCOVERED: dependencies found through the text code analysis.\n" +
                            "       These are the likeliest candidates for causing cycles.\n" +
                            "       Use excludeDependencies on the object name (not the change name) if needed on this\n" +
                            " * EXPLICIT: user-defined dependencies set via the includeDependencies or dependencies attributes\n" +
                            " * IMPLICIT: implied change dependencies determined by the order within incremental table changes\n" +
                            "\n" +
                            cycleMessages.joinToString("\n"), cycleComponents)
        }
    }
}

typealias VertexToString<T, E> = (vertex: T, target: Boolean, edge: E) -> String
