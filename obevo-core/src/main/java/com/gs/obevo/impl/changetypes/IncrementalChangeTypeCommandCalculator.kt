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
import com.gs.obevo.api.appdata.ChangeIncremental
import com.gs.obevo.api.appdata.ObjectKey
import com.gs.obevo.api.platform.ChangeCommand
import com.gs.obevo.api.platform.ChangePair
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator
import com.gs.obevo.impl.changecalc.ChangeCommandFactory
import com.gs.obevo.impl.command.AlreadyDroppedTableWarning
import com.gs.obevo.impl.command.BaselineChangeCommand
import com.gs.obevo.impl.command.HashMismatchWarning
import com.gs.obevo.impl.command.IncompleteBaselineWarning
import com.gs.obevo.impl.command.ParallelDeployChangeCommand
import com.gs.obevo.impl.command.UnmanageChangeCommand
import com.gs.obevo.util.DAStringUtil
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.api.list.ListIterable
import org.eclipse.collections.api.list.MutableList
import org.eclipse.collections.api.map.MutableMap
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.block.factory.Predicates
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Maps
import org.eclipse.collections.impl.factory.Sets
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import org.eclipse.collections.impl.set.mutable.UnifiedSet
import org.slf4j.LoggerFactory

/**
 * The [ChangeTypeCommandCalculator] implementation to use for incremental (i.e. rerunnable == false) [ChangeType]s.
 */
class IncrementalChangeTypeCommandCalculator internal constructor(private val numThreads: Int) : ChangeTypeCommandCalculator {

    private val changeCommandFactory = ChangeCommandFactory()

    override fun calculateCommands(changeType: ChangeType, changePairs: RichIterable<ChangePair>, unused: RichIterable<Change>, initAllowedOnHashExceptions: Boolean): ImmutableList<ChangeCommand> {
        val changeset = Lists.mutable.empty<ChangeCommand>()

        val dropObjectKeys = getDroppedTableChangesThatAreAlreadyRemoved(changePairs)

        val dropObjectPartition = changePairs.partition { dropObjectKeys.contains(it.objectKey) }
        changeset.addAll(dropObjectPartition.selected.map { AlreadyDroppedTableWarning(it.sourceChange!!) })

        val deployChanges = Lists.mutable.empty<ChangeIncremental>()
        val newBaselines = Lists.mutable.empty<ChangeIncremental>()
        val baselinedDrops = Lists.mutable.empty<ChangeIncremental>()

        dropObjectPartition.rejected.each { changePair ->
            // TODO make this a bit more OO, e.g. avoid the casting if possible
            val source = changePair.sourceChange
            val deployed = changePair.deployedChange

            if (source == null && deployed == null) {
                // this branch and exception throwing here is to avoid null deference warnings in findbugs for the next else branch
                throw IllegalStateException("This code branch should never happen; either of source or deployed should exist")
            } else if (source == null && deployed != null) {
                // In this case - the change exists in the target DB but was removed from the source
                val incrementalDeployed = deployed as ChangeIncremental
                if (initAllowedOnHashExceptions) {
                    incrementalDeployed.rollbackIfAlreadyDeployedContent = "INIT-only. sql should not actually be invoked"
                    incrementalDeployed.setContent("INIT-only content. sql should not actually be invoked")

                    changeset.add(UnmanageChangeCommand(deployed, "INIT-only"))
                } else if (incrementalDeployed.isRollbackActivated) {
                    if (!StringUtils.isEmpty(DAStringUtil.normalizeWhiteSpaceFromString(incrementalDeployed.rollbackContent))) {
                        // do a drop-with-sql here? or, automatically populate the drop command as the
                        // rollback?
                        changeset.add(changeCommandFactory.createRollback(incrementalDeployed, "Running Rollback"))
                    } else {
                        changeset.add(changeCommandFactory.createUnrolledbackWarning(incrementalDeployed))
                    }
                } else if (deployed.changeType.name.equals(ChangeType.MIGRATION_STR, ignoreCase = true)) {
                    // unmanage the change. Note that this clause should come after the rollback, as we will let the rollback logic happen if possible
                    incrementalDeployed.rollbackIfAlreadyDeployedContent = "migration-only. sql should not actually be invoked"
                    incrementalDeployed.setContent("migration-only content. sql should not actually be invoked")

                    changeset.add(UnmanageChangeCommand(deployed, "migration-only"))
                } else {
                    // possible candidate for an exception if we don't have a baseline. That ultimate logic is left to the methods below.
                    baselinedDrops.add(incrementalDeployed)
                }
            } else if (source != null && deployed == null) {
                // In this case - the change does not exist in the target DB and was added to the source
                val incrementalSource = source as ChangeIncremental
                if (incrementalSource.rollbackIfAlreadyDeployedContent != null || !incrementalSource.isActive) {
                    LOG.debug("Removal of change {} is okay as it had getRollbackIfAlreadyDeployedContent set or it was marked as inactive", incrementalSource)
                } else {
                    if (incrementalSource.baselinedChanges.isEmpty) {
                        if (incrementalSource.isDrop) {
                            // don't order this as a drop explicitly - put it in the right order
                            if (!incrementalSource.isManuallyCodedDrop) {
                                // for table drops
                                changeset.add(changeCommandFactory.createRemove(incrementalSource).withDrop(!incrementalSource.isKeepIncrementalOrder))
                            } else {
                                // typically set for drops of foreign keys in the EnvironmentCleaner
                                changeset.add(changeCommandFactory.createDeployCommand(incrementalSource).withDrop(true))
                            }
                        } else {
                            deployChanges.add(incrementalSource)
                        }
                    } else {
                        newBaselines.add(incrementalSource)
                    }
                }
            } else if (ObjectUtils.equals(source!!.contentHash, deployed!!.contentHash) || source.acceptableHashes.contains(deployed.contentHash)) {
                // In this case - the change exists in both the source and target db.
                // We need to check if anything has changed, using the hash
                val incrementalSource = source as ChangeIncremental
                val incrementalDeployed = deployed as ChangeIncremental

                if (incrementalSource.isActive && !incrementalDeployed.isActive) {
                    incrementalDeployed.isActive = true
                    changeset.add(changeCommandFactory.createUpdateAuditTableOnly(incrementalDeployed,
                            "Activating change"))
                } else if (!incrementalSource.isActive && incrementalDeployed.isActive) {
                    incrementalDeployed.isActive = false
                    changeset.add(changeCommandFactory.createUpdateAuditTableOnly(incrementalDeployed,
                            "Deactivating change"))
                } else if (!incrementalSource.isActive && !incrementalDeployed.isActive) {
                    changeset.add(changeCommandFactory.createCurrentDeactivationWarning(incrementalDeployed))
                }

                if (!ObjectUtils.equals(DAStringUtil.normalizeWhiteSpaceFromString(incrementalSource.rollbackContent),
                                DAStringUtil.normalizeWhiteSpaceFromString(incrementalDeployed.rollbackContent))) {
                    incrementalDeployed.rollbackContent = incrementalSource.rollbackContent
                    changeset.add(changeCommandFactory.createUpdateAuditTableOnly(incrementalDeployed,
                            "Updating rollback script"))
                }

                if (incrementalSource.rollbackIfAlreadyDeployedContent != null && incrementalSource.isActive) {
                    changeset.add(changeCommandFactory.createRollback(incrementalSource, "Rolling back due to ROLLBACK-IF-ALREADY-DEPLOYED flag."))
                }
            } else {
                if (initAllowedOnHashExceptions) {
                    // TODO handle init exceptions
                    changeset.add(changeCommandFactory.createUpdateAuditTableOnly(source, "initOnly"))
                } else {
                    changeset.add(HashMismatchWarning(source, deployed))
                }
            }
        }

        changeset.withAll(this.handleBaselineChanges(newBaselines, baselinedDrops))

        val parallelChangesPartition = deployChanges.partition(Predicates.attributeNotNull { changeIncremental -> changeIncremental.parallelGroup })
        for (singleChange in parallelChangesPartition.rejected) {
            changeset.add(changeCommandFactory.createDeployCommand(singleChange))
        }

        val parallelGroupedChanges = parallelChangesPartition.selected.groupBy { changeIncremental -> changeIncremental.parallelGroup }

        for (groupedChanges in parallelGroupedChanges.multiValuesView()) {
            if (groupedChanges.size() == 1) {
                changeset.add(changeCommandFactory.createDeployCommand(groupedChanges.first))
            } else {
                changeset.add(ParallelDeployChangeCommand(groupedChanges.first.schema, groupedChanges.toList().toImmutable(), numThreads))
            }
        }

        return changeset.toImmutable()
    }

    /**
     * Use case: a table file has all of its changes to be deployed but the last one is a DROP_TABLE. This implies
     * that the object had previously been undeployed and is now safe for removal from the source codebase.
     */
    private fun getDroppedTableChangesThatAreAlreadyRemoved(changePairs: RichIterable<ChangePair>): ImmutableSet<ObjectKey> {
        val dropOnlyObjectKeys = Sets.mutable.empty<ObjectKey>()

        val changePairsByObjectName = changePairs.groupBy { changePair2 -> changePair2.objectKey }
        for (stringRichIterablePair in changePairsByObjectName.keyMultiValuePairsView()) {
            val objectKey = stringRichIterablePair.one
            val objectChangePairs = stringRichIterablePair.two
            if (objectChangePairs.allSatisfy { it.sourceChange != null && it.deployedChange == null }) {
                val sourceChanges = objectChangePairs.collect { it.sourceChange }
                val lastChange = sourceChanges.maxBy { it!!.orderWithinObject }
                if ((lastChange as ChangeIncremental).isDrop && !lastChange.isForceDropForEnvCleaning) {
                    dropOnlyObjectKeys.add(objectKey)
                }
            }
        }

        return dropOnlyObjectKeys.toImmutable()
    }

    private fun handleBaselineChanges(newBaselines: MutableList<ChangeIncremental>, baselinedDrops: MutableList<ChangeIncremental>): ListIterable<ChangeCommand> {
        val baselineDeployedMap = Maps.mutable.empty<ObjectKey, MutableMap<String, Change>>()

        for (deployed in baselinedDrops) {
            var submap: MutableMap<String, Change>? = baselineDeployedMap[deployed.objectKey]
            if (submap == null) {
                submap = UnifiedMap.newMap()
                baselineDeployedMap[deployed.objectKey] = submap
            }
            submap!![deployed.changeName] = deployed
        }

        val changeset = Lists.mutable.empty<ChangeCommand>()

        val successfulBaselinedChanges = UnifiedSet.newSet<Change>()
        for (baseline in newBaselines) {
            val relatedChanges = baselineDeployedMap[baseline.objectKey]

            var status = BaselineStatus.FULLY_DEPLOYED
            val nonDeployedChanges = UnifiedSet.newSet<String>()
            if (relatedChanges == null) {
                status = BaselineStatus.NOT_DEPLOYED
            } else {
                successfulBaselinedChanges.addAll(relatedChanges.values)

                for (subchange in baseline.baselinedChanges) {
                    if (!relatedChanges.containsKey(subchange)) {
                        nonDeployedChanges.add(subchange)
                        status = BaselineStatus.PARTIAL_DEPLOYED
                    }
                }
            }

            when (status) {
                IncrementalChangeTypeCommandCalculator.BaselineStatus.NOT_DEPLOYED -> changeset.add(changeCommandFactory.createDeployCommand(baseline))
                IncrementalChangeTypeCommandCalculator.BaselineStatus.FULLY_DEPLOYED ->
                    // removing the baselined artifacts
                    changeset.add(BaselineChangeCommand(baseline, relatedChanges!!.valuesView().toList().toImmutable()))
                IncrementalChangeTypeCommandCalculator.BaselineStatus.PARTIAL_DEPLOYED -> changeset.add(IncompleteBaselineWarning(baseline, nonDeployedChanges.toImmutable()))
                else -> throw IllegalArgumentException("unexpected enum $status")
            }// in this case, treat the baseline change as is (i.e. a regular insert)
        }

        for (baseline in baselinedDrops) {
            if (!successfulBaselinedChanges.contains(baseline)) {
                // TODO do the baseline check here (collect changes that need to be cleared out)
                changeset.add(changeCommandFactory.createImproperlyRemovedWarning(baseline))
            }
        }

        return changeset
    }

    private enum class BaselineStatus {
        NOT_DEPLOYED,
        PARTIAL_DEPLOYED,
        FULLY_DEPLOYED
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(IncrementalChangeTypeCommandCalculator::class.java)
    }
}
