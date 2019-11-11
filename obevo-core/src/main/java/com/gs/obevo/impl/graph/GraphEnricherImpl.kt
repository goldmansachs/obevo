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
package com.gs.obevo.impl.graph

import com.gs.obevo.api.appdata.CodeDependencyType
import com.gs.obevo.api.platform.ChangeType
import org.eclipse.collections.api.tuple.Pair
import org.eclipse.collections.impl.tuple.Tuples
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.slf4j.LoggerFactory

/**
 * Created a graph out of the input changes, so that the graph can be shared by other components
 */
class GraphEnricherImpl(private val convertDbObjectName: Function1<String, String>) : GraphEnricher {
    override fun <T : SortableDependencyGroup> createDependencyGraph(inputs: Iterable<T>, rollback: Boolean): Graph<T, DefaultEdge> {
        val changeIndexes = listOf(
                ObjectIndex(),
                SchemaObjectIndex(),
                ObjectChangeIndex<T>(),
                SchemaChangeObjectIndex<T>()
        )

        changeIndexes.forEach { changeIndex -> inputs.forEach(changeIndex::add) }

        val graph = DefaultDirectedGraph<T, DefaultEdge>(DefaultEdge::class.java)

        // First - add the core objects to the graph
        inputs.forEach { graph.addVertex(it) }

        // Now add the declared dependencies to the graph
        for (changeGroup in inputs) {
            for (change in changeGroup.components) {
                if (change.codeDependencies != null) {
                    for (dependency in change.codeDependencies) {
                        var dependencyVertex: T? = null
                        for (changeIndex in changeIndexes) {
                            dependencyVertex = changeIndex.retrieve(change.changeKey.objectKey.schema, dependency.target)
                            if (dependencyVertex != null) {
                                if (LOG.isTraceEnabled) {
                                    LOG.trace("Discovered dependency from {} to {} using index {}",
                                            dependencyVertex,
                                            change.changeKey,
                                            changeIndex)
                                }
                                break
                            }
                        }

                        if (dependencyVertex == null) {
                            LOG.trace("Dependency not found; likely due to not enriching the full graph in source. Should be OK to ignore: {} - {}", dependency, change)
                        } else {
                            graph.addEdge(dependencyVertex, changeGroup, DependencyEdge(dependency.codeDependencyType))
                        }
                    }
                }
            }
        }

        // Add in changes within incremental files to ensure proper order
        val groupToComponentPairs = inputs.flatMap { group -> group.components.map { Pair(group, it) } }

        val incrementalChangeByObjectMap = groupToComponentPairs.groupBy { pair ->
            val tSortMetadata = pair.second
            var changeType = tSortMetadata.changeKey.objectKey.changeType.name
            if (changeType == ChangeType.TRIGGER_INCREMENTAL_OLD_STR || changeType == ChangeType.FOREIGN_KEY_STR) {
                changeType = ChangeType.TABLE_STR
            }
            changeType + ":" + tSortMetadata.changeKey.objectKey.schema + ":" + convertDbObjectName(tSortMetadata.changeKey.objectKey.objectName)
        }

        incrementalChangeByObjectMap.values
                .map { it.sortedBy { pair -> pair.second.orderWithinObject } }
                .forEach { pair ->
                    pair.map { it.first }.zipWithNext().forEach { (each, nextChange) ->
                        // if rollback, then go from the next change to the previous
                        val fromVertex = if (rollback) nextChange else each
                        val toVertex = if (rollback) each else nextChange
                        graph.addEdge(fromVertex, toVertex, DependencyEdge(CodeDependencyType.IMPLICIT))
                    }
                }

        // validate
        GraphUtil.validateNoCycles(graph,
                { t -> t.components.collect { sortableDependency -> "[" + sortableDependency.changeKey.objectKey.objectName + "." + sortableDependency.changeKey.changeName + "]" }.makeString(", ") },
                { dependencyEdge -> (dependencyEdge as DependencyEdge).edgeType.name })

        return graph
    }

    override fun <T> createSimpleDependencyGraph(inputs: Iterable<T>, edgesFunction: Function1<T, Iterable<T>>): Graph<T, DefaultEdge> {
        val graph = DefaultDirectedGraph<T, DefaultEdge>(DefaultEdge::class.java)
        inputs.forEach { graph.addVertex(it) }

        inputs.forEach { input ->
            val targetVertices = edgesFunction.invoke(input)
            for (targetVertex in targetVertices) {
                if (graph.containsVertex(targetVertex)) {
                    graph.addEdge(targetVertex, input)
                } else {
                    LOG.info("Problem?")
                }
            }
        }

        return graph
    }

    private interface ChangeIndex<T> {
        fun add(change: T)

        fun retrieve(schema: String, dependency: String): T?
    }

    /**
     * Looks for the given dependency/object
     */
    private inner class ObjectIndex<T : SortableDependencyGroup> : ChangeIndex<T> {
        private val schemaToObjectMap = mutableMapOf<Pair<String, String>, T>()

        override fun add(changeGroup: T) {
            for (change in changeGroup.components) {
                val existingChange = retrieve(change.changeKey.objectKey.schema, convertDbObjectName(change.changeKey.objectKey.objectName))
                // TODO getFirst is not ideal here
                if (existingChange == null || existingChange.components.first.orderWithinObject < change.orderWithinObject) {
                    // only keep the latest (why latest vs earliest?)
                    schemaToObjectMap[Tuples.pair(change.changeKey.objectKey.schema, convertDbObjectName(change.changeKey.objectKey.objectName))] = changeGroup
                }
            }
        }

        override fun retrieve(schema: String, dependency: String): T? {
            return schemaToObjectMap[Tuples.pair(schema, convertDbObjectName(dependency))]
        }
    }

    private inner class SchemaObjectIndex<T : SortableDependencyGroup> : ChangeIndex<T> {
        private val objectMap = mutableMapOf<String, T>()

        override fun add(changeGroup: T) {
            for (change in changeGroup.components) {
                val existingChange = retrieve(change.changeKey.objectKey.schema, convertDbObjectName(change.changeKey.objectKey.objectName))
                // TODO getFirst is not ideal here
                if (existingChange == null || existingChange.components.first.orderWithinObject < change.orderWithinObject) {
                    // only keep the latest (why latest vs earliest?)
                    objectMap[convertDbObjectName(change.changeKey.objectKey.schema + "." + change.changeKey.objectKey.objectName)] = changeGroup
                }
            }
        }

        override fun retrieve(schema: String, dependency: String): T? {
            return objectMap[convertDbObjectName(dependency)]
        }
    }

    private inner class ObjectChangeIndex<T : SortableDependencyGroup> : ChangeIndex<T> {
        private val schemaToObjectMap = mutableMapOf<Pair<String, String>, T>()

        override fun add(changeGroup: T) {
            for (change in changeGroup.components) {
                schemaToObjectMap[Tuples.pair(change.changeKey.objectKey.schema, convertDbObjectName(change.changeKey.objectKey.objectName + "." + change.changeKey.changeName))] = changeGroup
            }
        }

        override fun retrieve(schema: String, dependency: String): T? {
            return schemaToObjectMap[Tuples.pair(schema, convertDbObjectName(dependency))]
        }
    }

    private inner class SchemaChangeObjectIndex<T : SortableDependencyGroup> : ChangeIndex<T> {
        private val objectMap = mutableMapOf<String, T>()

        override fun add(changeGroup: T) {
            for (change in changeGroup.components) {
                objectMap[convertDbObjectName(change.changeKey.objectKey.schema + "." + change.changeKey.objectKey.objectName + "." + change.changeKey.changeName)] = changeGroup
            }
        }

        override fun retrieve(schema: String, dependency: String): T? {
            return objectMap[convertDbObjectName(dependency)]
        }
    }

    /**
     * Custom edge type to allow for better error logging for cycles, namely to show the dependency edge type.
     */
    private class DependencyEdge internal constructor(internal val edgeType: CodeDependencyType) : DefaultEdge()

    companion object {
        private val LOG = LoggerFactory.getLogger(GraphEnricherImpl::class.java)
    }
}
