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
package com.gs.obevo.impl.changecalc;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ChangePair;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
import com.gs.obevo.impl.AuditChangeCommand;
import com.gs.obevo.impl.ChangeCommandWarning;
import com.gs.obevo.impl.Changeset;
import com.gs.obevo.impl.ChangesetCreator;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.changesorter.ChangeCommandSorter;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.collections.impl.block.factory.Predicates.attributeAnySatisfy;
import static org.eclipse.collections.impl.block.factory.Predicates.attributeIsNull;
import static org.eclipse.collections.impl.block.factory.Predicates.instanceOf;

public class ChangesetCreatorImpl implements ChangesetCreator {
    private static final Logger LOG = LoggerFactory.getLogger(ChangesetCreatorImpl.class);
    private static final HashingStrategy<Change> hashStrategy = HashingStrategies.fromFunction(Change.TO_CHANGE_KEY);
    /**
     * By default, we will always defer those predicates marked w/ the changeset attribute as the existence of that
     * indicates something that may not be good to run alongside a regular release.
     */
    private static final Predicate<? super ExecuteChangeCommand> DEFAULT_DEFERRED_PREDICATE =
            attributeAnySatisfy(ChangeCommand.TO_CHANGES, attributeIsNull(Change.TO_CHANGESET));

    private final ChangeCommandSorter changeCommandSorter;
    private final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    public ChangesetCreatorImpl(ChangeCommandSorter changeCommandSorter, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        this.changeCommandSorter = changeCommandSorter;
        this.changeTypeBehaviorRegistry = changeTypeBehaviorRegistry;
    }

    @Override
    public Changeset determineChangeset(RichIterable<Change> deploys, final RichIterable<Change> sources,
            final boolean rollback, final boolean initAllowedOnHashExceptions, Predicate<? super ExecuteChangeCommand> changesetPredicate) {
        final Multimap<ChangeType, Change> deployChangesByType = deploys.groupBy(Change.TO_CHANGE_TYPE);
        final Multimap<ChangeType, Change> sourceChangesByType = sources.groupBy(Change.TO_CHANGE_TYPE);

        SetIterable<ChangeType> changeTypes = Sets.mutable.withAll(deployChangesByType.keysView()).withAll(sourceChangesByType.keysView());

        RichIterable<ChangeCommand> commands = changeTypes.flatCollect(new Function<ChangeType, Iterable<ChangeCommand>>() {
            @Override
            public Iterable<ChangeCommand> valueOf(ChangeType changeType) {
                RichIterable<Change> changeTypeDeploys = deployChangesByType.get(changeType);
                RichIterable<Change> changeTypeSources = sourceChangesByType.get(changeType);

                final MutableMap<Change, ChangePair> changes = UnifiedMapWithHashingStrategy
                        .newMap(hashStrategy);
                Procedure2<Change, Boolean> addChangeToMap = new Procedure2<Change, Boolean>() {
                    @Override
                    public void value(Change change, Boolean fromSource) {
                        ChangePair changePair = changes.get(change);
                        if (changePair == null) {
                            changePair = new ChangePair();
                            changes.put(change, changePair);
                        }
                        if (fromSource) {
                            changePair.setSourceChange(change);
                        } else {
                            changePair.setDeployedChange(change);
                        }
                    }
                };
                changeTypeSources.forEachWith(addChangeToMap, true);
                changeTypeDeploys.forEachWith(addChangeToMap, false);

                return changeTypeBehaviorRegistry.getChangeTypeBehavior(changeType.getName()).getChangeTypeCalculator().calculateCommands(changeType, changes.valuesView(), sources, rollback, initAllowedOnHashExceptions);
            }
        });

        PartitionIterable<ChangeCommand> executePartition = commands.partition(instanceOf(ExecuteChangeCommand.class));
        PartitionIterable<ChangeCommand> auditPartition = executePartition.getRejected().partition(instanceOf(AuditChangeCommand.class));
        PartitionIterable<ChangeCommand> warningPartition = auditPartition.getRejected().partition(instanceOf(ChangeCommandWarning.class));

        if (warningPartition.getRejected().notEmpty()) {
            throw new IllegalStateException("These changes are not of an expected class type: " + executePartition.getRejected());
        }

        ImmutableList<ExecuteChangeCommand> changeCommands = changeCommandSorter.sort(cast(executePartition.getSelected(), ExecuteChangeCommand.class), rollback);

        if (changesetPredicate == null) {
            changesetPredicate = DEFAULT_DEFERRED_PREDICATE;
        }
        PartitionImmutableList<ExecuteChangeCommand> changesetPartition = changeCommands.partition(
                changesetPredicate
        );

        return new Changeset(changesetPartition.getSelected(),
                changesetPartition.getRejected(),
                cast(auditPartition.getSelected(), AuditChangeCommand.class),
                cast(warningPartition.getSelected(), ChangeCommandWarning.class)
        );
    }

    /**
     * Casts the collection (needed prior to Java 8).
     * @param collection
     * @param clazz class to cast to; not used in the actual implementation
     * @param <T>
     * @return
     */
    private static <T> RichIterable<T> cast(RichIterable<? super T> collection, Class<T> clazz){
        return (RichIterable<T>)collection;
    }
}
