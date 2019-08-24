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
package com.gs.obevo.impl.changetypes

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.GroupChange
import com.gs.obevo.api.platform.ChangeCommand
import com.gs.obevo.api.platform.ChangePair
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator
import com.gs.obevo.impl.ExecuteChangeCommand
import com.gs.obevo.impl.changecalc.ChangeCommandFactory
import com.gs.obevo.impl.command.UnmanageChangeCommand
import com.gs.obevo.impl.graph.GraphEnricher
import com.gs.obevo.impl.graph.GraphSorter
import com.gs.obevo.impl.graph.SortableDependency
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.collection.MutableCollection
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.api.list.MutableList
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.list.mutable.ListAdapter
import org.eclipse.collections.impl.set.mutable.SetAdapter
import org.jgrapht.alg.connectivity.ConnectivityInspector
import org.slf4j.LoggerFactory
import java.util.Objects

internal class GroupChangeTypeCommandCalculator(private val changeCommandFactory: ChangeCommandFactory, private val enricher: GraphEnricher) : ChangeTypeCommandCalculator {
    private val sorter = GraphSorter()

    override fun calculateCommands(changeType: ChangeType, changePairs: RichIterable<ChangePair>, sourcesUnused: RichIterable<Change>, initAllowedOnHashExceptions: Boolean): ImmutableList<ChangeCommand> {
        val rerunnableObjectInfo = changePairs.injectInto(RerunnableObjectInfo(), { it, changePair ->
            // TODO make this a bit more OO, e.g. avoid the instanceof all over the place
            val source = changePair.sourceChange
            val deployed = changePair.deployedChange

            if (source == null && deployed == null) {
                // this branch and exception throwing here is to avoid null deference warnings in findbugs for the next else branch
                throw IllegalStateException("This code branch should never happen; either of source or deployed should exist")
            } else if (source == null && deployed != null) {
                // In this case - the change exists in the target DB but was removed from the source
                it.addDroppedObject(deployed)
            } else if (source != null && deployed == null) {
                it.addChangedObject(source)
            } else if (Objects.equals(source!!.contentHash, deployed!!.contentHash) || source.acceptableHashes.contains(deployed.contentHash)) {
                // In this case - the change exists in both the source and target db.
                // We need to check if anything has changed, using the hash
                LOG.trace("Nothing to do here; source [{}] and target [{}] match in hash", source, deployed)
            } else {
                it.addChangedObject(source)
            }

            it
        })

        return this.processRerunnableChanges(rerunnableObjectInfo)
    }

    /**
     * @param rerunnableObjectInfo
     */
    private fun processRerunnableChanges(rerunnableObjectInfo: RerunnableObjectInfo): ImmutableList<ChangeCommand> {
        val commands : MutableList<ChangeCommand> = rerunnableObjectInfo.droppedObjects
                .toSortedListBy { it.objectName }
                .collect { droppedObject -> UnmanageChangeCommand(droppedObject, "static data change to be unmanaged") }

        return commands.withAll(this.handleChanges(rerunnableObjectInfo.changedObjects)).toImmutable()
    }

    private fun handleChanges(fromSourceList: MutableCollection<Change>): MutableList<ExecuteChangeCommand> {
        val graph = enricher.createDependencyGraph(fromSourceList, false)
//        val graph = enricher.createSimpleDependencyGraph(fromSourceList, Change::getDependentChanges)

        val connectivityInspector = ConnectivityInspector(graph)

        return ListAdapter.adapt(connectivityInspector.connectedSets()).collect({ connectedSet ->
            // once we have a connectedSet, sort within those changes to ensure that we still sort them in the
            // right order (i.e. via topological sort)
            val fullChanges = sorter.sortChanges(graph, SetAdapter.adapt(connectedSet), SortableDependency.GRAPH_SORTER_COMPARATOR)
            changeCommandFactory.createDeployCommand(GroupChange(fullChanges))
        }, Lists.mutable.empty())
    }

    private class RerunnableObjectInfo {
        internal val droppedObjects: MutableCollection<Change> = Lists.mutable.empty()
        internal val changedObjects: MutableCollection<Change> = Lists.mutable.empty()

        internal fun addDroppedObject(`object`: Change?) {
            this.droppedObjects.add(`object`)
        }

        internal fun addChangedObject(`object`: Change?) {
            this.changedObjects.add(`object`)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GroupChangeTypeCommandCalculator::class.java)
    }
}
