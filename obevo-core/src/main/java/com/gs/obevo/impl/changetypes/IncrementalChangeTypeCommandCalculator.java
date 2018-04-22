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
package com.gs.obevo.impl.changetypes;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ChangePair;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.impl.changecalc.ChangeCommandFactory;
import com.gs.obevo.impl.command.AlreadyDroppedTableWarning;
import com.gs.obevo.impl.command.BaselineChangeCommand;
import com.gs.obevo.impl.command.HashMismatchWarning;
import com.gs.obevo.impl.command.IncompleteBaselineWarning;
import com.gs.obevo.impl.command.ParallelDeployChangeCommand;
import com.gs.obevo.impl.command.UnmanageChangeCommand;
import com.gs.obevo.util.DAStringUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.HashingStrategyMaps;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChangeTypeCommandCalculator} implementation to use for incremental (i.e. rerunnable == false) {@link ChangeType}s.
 */
public class IncrementalChangeTypeCommandCalculator implements ChangeTypeCommandCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(IncrementalChangeTypeCommandCalculator.class);

    private final ChangeCommandFactory changeCommandFactory = new ChangeCommandFactory();
    private final int numThreads;

    IncrementalChangeTypeCommandCalculator(int numThreads) {
        this.numThreads = numThreads;
    }

    @Override
    public ImmutableList<ChangeCommand> calculateCommands(ChangeType changeType, RichIterable<ChangePair> changePairs, RichIterable<Change> unused, final boolean rollback, final boolean initAllowedOnHashExceptions) {
        final MutableList<ChangeCommand> changeset = Lists.mutable.empty();

        ImmutableSet<ObjectKey> dropObjectKeys = getDroppedTableChangesThatAreAlreadyRemoved(changePairs);

        PartitionIterable<ChangePair> dropObjectPartition = changePairs.partition(_this -> dropObjectKeys.contains(_this.getObjectKey()));
        for (ChangePair changePair : dropObjectPartition.getSelected()) {
            changeset.add(new AlreadyDroppedTableWarning(changePair.getSourceChange()));
        }

        final MutableList<ChangeIncremental> deployChanges = Lists.mutable.empty();
        final MutableList<ChangeIncremental> newBaselines = Lists.mutable.empty();
        final MutableList<ChangeIncremental> baselinedDrops = Lists.mutable.empty();

        dropObjectPartition.getRejected().forEach(new Procedure<ChangePair>() {
            @Override
            public void value(ChangePair changePair) {
                // TODO make this a bit more OO, e.g. avoid the casting if possible
                Change source = changePair.getSourceChange();
                Change deployed = changePair.getDeployedChange();

                if (source == null && deployed == null) {
                    // this branch and exception throwing here is to avoid null deference warnings in findbugs for the next else branch
                    throw new IllegalStateException("This code branch should never happen; either of source or deployed should exist");
                }

                if (source == null && deployed != null) {
                    // In this case - the change exists in the target DB but was removed from the source
                    ChangeIncremental incrementalDeployed = (ChangeIncremental) deployed;
                    if (initAllowedOnHashExceptions) {
                        incrementalDeployed.setRollbackIfAlreadyDeployedContent(
                                "INIT-only. sql should not actually be invoked");
                        incrementalDeployed.setContent("INIT-only content. sql should not actually be invoked");

                        changeset.add(new UnmanageChangeCommand(deployed, "INIT-only"));
                    } else if (rollback) {
                        if (!StringUtils.isEmpty(DAStringUtil.normalizeWhiteSpaceFromString(incrementalDeployed.getRollbackContent()))) {
                            // do a drop-with-sql here? or, automatically populate the drop command as the
                            // rollback?
                            incrementalDeployed.setRollbackActivated(true);
                            changeset.add(changeCommandFactory.createRollback(incrementalDeployed, "Running Rollback"));
                        } else {
                            changeset.add(changeCommandFactory.createUnrolledbackWarning(incrementalDeployed));
                        }
                    } else if (deployed.getChangeType().getName().equalsIgnoreCase(ChangeType.MIGRATION_STR)) {
                        // unmanage the change. Note that this clause should come after the rollback, as we will let the rollback logic happen if possible
                        incrementalDeployed.setRollbackIfAlreadyDeployedContent(
                                "migration-only. sql should not actually be invoked");
                        incrementalDeployed.setContent("migration-only content. sql should not actually be invoked");

                        changeset.add(new UnmanageChangeCommand(deployed, "migration-only"));
                    } else {
                        // possible candidate for an exception if we don't have a baseline. That ultimate logic is left to the methods below.
                        baselinedDrops.add(incrementalDeployed);
                    }
                } else if (source != null && deployed == null) {
                    // In this case - the change does not exist in the target DB and was added to the source
                    ChangeIncremental incrementalSource = (ChangeIncremental) source;
                    if (incrementalSource.getRollbackIfAlreadyDeployedContent() != null
                            || !incrementalSource.isActive()) {
                        LOG.debug("Removal of change {} is okay as it had getRollbackIfAlreadyDeployedContent set or it was marked as inactive", incrementalSource);
                    } else {
                        if (incrementalSource.getBaselinedChanges().isEmpty()) {
                            if (incrementalSource.isDrop()) {
                                // don't order this as a drop explicitly - put it in the right order
                                if (!incrementalSource.isManuallyCodedDrop()) {
                                    // for table drops
                                    changeset.add(changeCommandFactory.createRemove(incrementalSource).withDrop(!incrementalSource.isKeepIncrementalOrder()));
                                } else {
                                    // typically set for drops of foreign keys in the EnvironmentCleaner
                                    changeset.add(changeCommandFactory.createDeployCommand(incrementalSource).withDrop(true));
                                }
                            } else {
                                deployChanges.add(incrementalSource);
                            }
                        } else {
                            newBaselines.add(incrementalSource);
                        }
                    }
                } else if (ObjectUtils.equals(source.getContentHash(), deployed.getContentHash())
                        || source.getAcceptableHashes().contains(deployed.getContentHash())) {
                    // In this case - the change exists in both the source and target db.
                    // We need to check if anything has changed, using the hash
                    ChangeIncremental incrementalSource = (ChangeIncremental) source;
                    ChangeIncremental incrementalDeployed = (ChangeIncremental) deployed;

                    if (incrementalSource.isActive() && !incrementalDeployed.isActive()) {
                        incrementalDeployed.setActive(true);
                        changeset.add(changeCommandFactory.createUpdateAuditTableOnly(incrementalDeployed,
                                "Activating change"));
                    } else if (!incrementalSource.isActive() && incrementalDeployed.isActive()) {
                        incrementalDeployed.setActive(false);
                        changeset.add(changeCommandFactory.createUpdateAuditTableOnly(incrementalDeployed,
                                "Deactivating change"));
                    } else if (!incrementalSource.isActive() && !incrementalDeployed.isActive()) {
                        changeset.add(changeCommandFactory.createCurrentDeactivationWarning(incrementalDeployed));
                    }

                    if (!ObjectUtils.equals(DAStringUtil.normalizeWhiteSpaceFromString(incrementalSource.getRollbackContent()),
                            DAStringUtil.normalizeWhiteSpaceFromString(incrementalDeployed.getRollbackContent()))) {
                        incrementalDeployed.setRollbackContent(incrementalSource.getRollbackContent());
                        changeset.add(changeCommandFactory.createUpdateAuditTableOnly(incrementalDeployed,
                                "Updating rollback script"));
                    }

                    if (incrementalSource.getRollbackIfAlreadyDeployedContent() != null
                            && incrementalSource.isActive()) {
                        changeset.add(changeCommandFactory.createRollback(incrementalSource, "Rolling back due to ROLLBACK-IF-ALREADY-DEPLOYED flag."));
                    }
                } else {
                    if (initAllowedOnHashExceptions) {
                        // SHANT handle init exceptions
                        changeset.add(changeCommandFactory.createUpdateAuditTableOnly(source, "initOnly"));
                    } else {
                        changeset.add(new HashMismatchWarning(source, deployed));
                    }
                }
            }
        });

        changeset.withAll(this.handleBaselineChanges(newBaselines, baselinedDrops));

        PartitionMutableList<ChangeIncremental> parallelChangesPartition = deployChanges.partition(Predicates.attributeNotNull(ChangeIncremental::getParallelGroup));
        for (ChangeIncremental singleChange : parallelChangesPartition.getRejected()) {
            changeset.add(changeCommandFactory.createDeployCommand(singleChange));
        }

        MutableListMultimap<String, ChangeIncremental> parallelGroupedChanges = parallelChangesPartition.getSelected().groupBy(ChangeIncremental::getParallelGroup);

        for (RichIterable<ChangeIncremental> groupedChanges : parallelGroupedChanges.multiValuesView()) {
            if (groupedChanges.size() == 1) {
                changeset.add(changeCommandFactory.createDeployCommand(groupedChanges.getFirst()));
            } else {
                changeset.add(new ParallelDeployChangeCommand(groupedChanges.getFirst().getSchema(), groupedChanges.toList().toImmutable(), numThreads));
            }
        }

        return changeset.toImmutable();
    }

    /**
     * Use case: a table file has all of its changes to be deployed but the last one is a DROP_TABLE. This implies
     * that the object had previously been undeployed and is now safe for removal from the source codebase.
     */
    private ImmutableSet<ObjectKey> getDroppedTableChangesThatAreAlreadyRemoved(RichIterable<ChangePair> changePairs) {
        MutableSet<ObjectKey> dropOnlyObjectKeys = Sets.mutable.empty();

        Multimap<ObjectKey, ChangePair> changePairsByObjectName = changePairs.groupBy(ChangePair::getObjectKey);
        for (Pair<ObjectKey, RichIterable<ChangePair>> stringRichIterablePair : changePairsByObjectName.keyMultiValuePairsView()) {
            ObjectKey objectKey = stringRichIterablePair.getOne();
            RichIterable<ChangePair> objectChangePairs = stringRichIterablePair.getTwo();
            if (objectChangePairs.allSatisfy(Predicates.attributeNotNull(ChangePair::getSourceChange).and(Predicates.attributeIsNull(ChangePair::getDeployedChange)))) {
                RichIterable<Change> sourceChanges = objectChangePairs.collect(ChangePair::getSourceChange);
                Change lastChange = sourceChanges.maxBy(Change::getOrderWithinObject);
                if (((ChangeIncremental) lastChange).isDrop() && !((ChangeIncremental) lastChange).isForceDropForEnvCleaning()) {
                    dropOnlyObjectKeys.add(objectKey);
                }
            }
        }

        return dropOnlyObjectKeys.toImmutable();
    }

    private ListIterable<ChangeCommand> handleBaselineChanges(MutableList<ChangeIncremental> newBaselines, MutableList<ChangeIncremental> baselinedDrops) {
        HashingStrategy<Change> hashStrategyForBaseline = HashingStrategies.fromFunction(Change::getObjectKey);

        MutableMap<Change, MutableMap<String, Change>> baselineDeployedMap = HashingStrategyMaps.mutable.of(hashStrategyForBaseline);

        for (Change deployed : baselinedDrops) {
            MutableMap<String, Change> list = baselineDeployedMap.get(deployed);
            if (list == null) {
                list = UnifiedMap.newMap();
                baselineDeployedMap.put(deployed, list);
            }
            list.put(deployed.getChangeName(), deployed);
        }

        MutableList<ChangeCommand> changeset = Lists.mutable.empty();

        MutableSet<Change> successfulBaselinedChanges = UnifiedSet.newSet();
        for (ChangeIncremental baseline : newBaselines) {
            MutableMap<String, Change> relatedChanges = baselineDeployedMap.get(baseline);

            BaselineStatus status = BaselineStatus.FULLY_DEPLOYED;
            MutableSet<String> nonDeployedChanges = UnifiedSet.newSet();
            if (relatedChanges == null) {
                status = BaselineStatus.NOT_DEPLOYED;
            } else {
                successfulBaselinedChanges.addAll(relatedChanges.values());

                for (String subchange : baseline.getBaselinedChanges()) {
                    if (!relatedChanges.containsKey(subchange)) {
                        nonDeployedChanges.add(subchange);
                        status = BaselineStatus.PARTIAL_DEPLOYED;
                    }
                }
            }

            switch (status) {
            case NOT_DEPLOYED:
                changeset.add(changeCommandFactory.createDeployCommand(baseline));
                break;  // in this case, treat the baseline change as is (i.e. a regular insert)
            case FULLY_DEPLOYED:
                // removing the baselined artifacts
                changeset.add(new BaselineChangeCommand(baseline, relatedChanges.valuesView().toList().toImmutable()));
                break;
            case PARTIAL_DEPLOYED:
                changeset.add(new IncompleteBaselineWarning(baseline, nonDeployedChanges.toImmutable()));
                break;
            default:
                throw new IllegalArgumentException("unexpected enum " + status);
            }
        }

        for (ChangeIncremental baseline : baselinedDrops) {
            if (!successfulBaselinedChanges.contains(baseline)) {
                // SHANT do the baseline check here (collect changes that need to be cleared out)
                changeset.add(changeCommandFactory.createImproperlyRemovedWarning(baseline));
            }
        }

        return changeset;
    }

    private enum BaselineStatus {
        NOT_DEPLOYED,
        PARTIAL_DEPLOYED,
        FULLY_DEPLOYED,;
    }
}
