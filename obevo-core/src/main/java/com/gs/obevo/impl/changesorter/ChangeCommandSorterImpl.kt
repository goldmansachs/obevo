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
package com.gs.obevo.impl.changesorter

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.Platform
import com.gs.obevo.impl.ExecuteChangeCommand
import com.gs.obevo.impl.graph.GraphEnricher
import com.gs.obevo.impl.graph.GraphEnricherImpl
import com.gs.obevo.impl.graph.GraphSorter
import com.gs.obevo.impl.graph.SortableDependencyGroup
import org.eclipse.collections.api.block.function.Function
import org.eclipse.collections.impl.block.factory.Comparators
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter
import org.eclipse.collections.impl.factory.Sets
import org.slf4j.LoggerFactory

/**
 * Original implementation of the sort order algorithm that:
 * - first sorts based on the ChangeType
 * - then sorts interdependencies within the change types.
 *
 *
 * Approach for implementation:
 * 1) declared dependencies
 * 2) dependencies derived from content (incl.. sps and the incremental file entries)
 * 3) subtract excluded dependencies
 * 3) add other implied dependencies assuming the target (or source?) didn't have other dependencies to worry about. This is optional, just for nicer output.
 *
 * Assumes that dependent objects are already defined
 */
class ChangeCommandSorterImpl(
        private val dialect: Platform
) : ChangeCommandSorter {
    private val enricher: GraphEnricher
    private val graphSorter = GraphSorter()

    init {
        this.enricher = GraphEnricherImpl(dialect.convertDbObjectName()::valueOf)
    }

    override fun sort(changeCommands: Iterable<ExecuteChangeCommand>): List<ExecuteChangeCommand> {
        val addOrDropPartition = changeCommands.partition { !it.isDrop }
        val commandDatas = addOrDropPartition.first.map { DbCommandSortKey(it) }

        val dataCommandPartition = commandDatas.partition { it.changeType.name == ChangeType.STATICDATA_STR }

        val orderedAdds = sortAddCommands(dataCommandPartition.second)
        val orderedDrops = sortDropCommands(addOrDropPartition.second)
        val orderedDatas = sortDataCommands(dataCommandPartition.first)

        return orderedDrops.plus(orderedAdds).plus(orderedDatas).map { it.changeCommand }
    }

    private fun sortAddCommands(addCommands: Iterable<DbCommandSortKey>): List<DbCommandSortKey> {
        val changeToSortKeyMap = CollectionAdapter.wrapList(addCommands).groupByEach { it.changeCommand.changes };
        val addGraph = enricher.createSimpleDependencyGraph(addCommands, {
            it.changeCommand.changes
                    .flatMap { it.dependentChanges ?: Sets.immutable.empty()}
                    .flatMap(changeToSortKeyMap::get)
        })

        val addChanges = graphSorter.sortChanges(addGraph, SortableDependencyGroup.GRAPH_SORTER_COMPARATOR)
        addChanges.forEachIndexed { order, dbCommandSortKey -> dbCommandSortKey.order = order }

        return addCommands.sortedBy { it.order }
    }

    private fun sortDropCommands(dropCommands: Iterable<ExecuteChangeCommand>): List<DbCommandSortKey> {
        val dropSortKeys = dropCommands.map { DbCommandSortKey(it) }

        val dropByChangeTypePartition = dropSortKeys.partition { it.changeType.isRerunnable }

        val incrementalDrops = dropByChangeTypePartition.second
        val rerunnableDrops = dropByChangeTypePartition.first

        val dropKeyComparator = Comparators.fromFunctions(Function<DbCommandSortKey, Int> { it.changeType.deployOrderPriority }, Function<DbCommandSortKey, String> { it.objectName })

        if (dialect.isDropOrderRequired) {
            val changeToSortKeyMap = CollectionAdapter.wrapList(rerunnableDrops).groupByEach { it.changeCommand.changes };
            val addGraph = enricher.createSimpleDependencyGraph(rerunnableDrops, {
                it.changeCommand.changes
                        .flatMap { it.dependentChanges ?: Sets.mutable.empty()}
                        .flatMap(changeToSortKeyMap::get)
            })

            val addChanges = graphSorter.sortChanges(addGraph, SortableDependencyGroup.GRAPH_SORTER_COMPARATOR)
            addChanges.forEachIndexed { order, dbCommandSortKey -> dbCommandSortKey.order = order }
        } else {
            // Sort by the object type to facilitate any dependencies that would naturally occur, e.g. for packages vs. package bodies in Oracle
            rerunnableDrops.sortedWith(dropKeyComparator).forEachIndexed { index, it -> it.order = index }
        }

        incrementalDrops.sortedWith(dropKeyComparator).forEachIndexed { index, it -> it.order = index }

        return dropSortKeys.sortedBy { it.order }.reversed()
    }

    private fun sortDataCommands(dataCommands: Iterable<DbCommandSortKey>): List<DbCommandSortKey> {
        // The background behind this order check: while we want to rely on the FK-dependencies generally to
        // figure out the order of deployment (and once we group by connected components,
        // then the dependencies between components shouldn't matter), there are a couple use cases where we
        // still need to rely on the "order" attribute:
        // 1) for backwards-compatibility w/ teams using older versions of this tool that didn't have
        // this more-advanced ordering logic and that instead needed the "order" attribute
        // 2) the MetadataGroup use case (see "MetadataGroupTest")
        // Hence, we will still rely on the "changeOrder" attribute here as a fallback for the order
        val sortedDataCommands = dataCommands.sortedBy { dbCommandSortKey ->
            val changes = dbCommandSortKey.changeCommand.changes
            if (changes.isEmpty() || changes.size > 1) {
                Change.DEFAULT_CHANGE_ORDER
            } else {
                changes.first().order
            }
        }

        sortedDataCommands.forEachIndexed { order, it -> it.order = order }

        return dataCommands.sortedBy { it.order }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ChangeCommandSorterImpl::class.java)
    }
}
