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
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ChangePair;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.changecalc.ChangeCommandFactory;
import com.gs.obevo.impl.graph.GraphEnricher;
import com.gs.obevo.util.CollectionUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.factory.HashingStrategySets;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChangeTypeCommandCalculator} implementation to use for rerunnable (i.e. rerunnable == true) {@link ChangeType}s.
 */
class RerunnableChangeTypeCommandCalculator implements ChangeTypeCommandCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(RerunnableChangeTypeCommandCalculator.class);

    private final ChangeCommandFactory changeCommandFactory = new ChangeCommandFactory();
    private final GraphEnricher enricher;

    RerunnableChangeTypeCommandCalculator(GraphEnricher enricher) {
        this.enricher = enricher;
    }

    @Override
    public ImmutableList<ChangeCommand> calculateCommands(ChangeType changeType, RichIterable<ChangePair> changePairs, RichIterable<Change> allSourceChanges, boolean initAllowedOnHashExceptions) {
        RerunnableObjectInfo rerunnableObjectInfo = changePairs.injectInto(new RerunnableObjectInfo(), new Function2<RerunnableObjectInfo, ChangePair, RerunnableObjectInfo>() {
            @Override
            public RerunnableObjectInfo value(RerunnableObjectInfo rerunnableObjectInfo1, ChangePair changePair) {
                // TODO make this a bit more OO, e.g. avoid the instanceof all over the place
                Change source = changePair.getSourceChange();
                Change deployed = changePair.getDeployedChange();

                if (source == null && deployed == null) {
                    // this branch and exception throwing here is to avoid null deference warnings in findbugs for the next else branch
                    throw new IllegalStateException("This code branch should never happen; either of source or deployed should exist");
                }

                if (source == null && deployed != null) {
                    // In this case - the change exists in the target DB but was removed from the source
                    rerunnableObjectInfo1.addDroppedObject(deployed);
                } else if (source != null && deployed == null) {
                    rerunnableObjectInfo1.addChangedObject(source);
                } else if (ObjectUtils.equals(source.getContentHash(), deployed.getContentHash())
                        || source.getAcceptableHashes().contains(deployed.getContentHash())) {
                    // In this case - the change exists in both the source and target db.
                    // We need to check if anything has changed, using the hash

                    LOG.trace("Nothing to do here; source [{}] and target [{}] match in hash", source, deployed);
                } else {
                    rerunnableObjectInfo1.addChangedObject(source);
                }

                return rerunnableObjectInfo1;
            }
        });

        return this.processRerunnableChanges(changeType, rerunnableObjectInfo, allSourceChanges);
    }

    /**
     * TODO for rerunnable to support a better change group:
     * -add dependent changes where applicable (e.g. views, sps, functions, but not static data)
     * -group the related changes as needed
     * -create the change for each group
     */
    private ImmutableList<ChangeCommand> processRerunnableChanges(ChangeType changeType,
            RerunnableObjectInfo rerunnableObjectInfo,
            RichIterable<Change> fromSourceList) {
        final MutableList<ChangeCommand> commands = Lists.mutable.empty();

        commands.addAll(rerunnableObjectInfo.getDroppedObjects().collect(new Function<Change, ExecuteChangeCommand>() {
            @Override
            public ExecuteChangeCommand valueOf(Change droppedObject) {
                return changeCommandFactory.createRemove(droppedObject).withDrop(true);
            }
        }));

        if (changeType.isDependentObjectRecalculationRequired()) {
            commands.addAll(getObjectChangesRequiringRecompilation(changeType, fromSourceList, rerunnableObjectInfo.getChangedObjects()
                    .reject(new Predicate<Change>() {
                        @Override
                        public boolean accept(Change change1) {
                            return change1.isCreateOrReplace();
                        }
                    }))
                    .collect(new Function<Change, ExecuteChangeCommand>() {
                        @Override
                        public ExecuteChangeCommand valueOf(Change change) {
                            return changeCommandFactory.createDeployCommand(change, "Re-deploying this object due to change in dependent object [" + change.getObjectName() + "]");
                        }
                    }));
        }

        commands.addAll(rerunnableObjectInfo.getChangedObjects().collect(new Function<Change, ExecuteChangeCommand>() {
            @Override
            public ExecuteChangeCommand valueOf(Change source) {
                return changeCommandFactory.createDeployCommand(source);
            }
        }));

        return commands.toImmutable();
    }

    /**
     * allow for use cases to redeploy changes that require recompiling
     * e.g. to add db objects to the change list to facilitate cases where it depends on another SP that is
     * changing, and so the dependent SP needs to get re-created also
     */
    private MutableSet<Change> getObjectChangesRequiringRecompilation(final ChangeType changeType, RichIterable<Change> fromSourceList, MutableCollection<Change> changedObjects) {
        if (fromSourceList.isEmpty()) {
            return Sets.mutable.empty();
        }
        // do not log errors as info or above here when creating the graph as we know that we don't have the full graph
        LOG.debug("START BLOCK: Ignore any 'Invalid change found?' errors in this block of code");
        Graph<Change, DefaultEdge> graph = enricher.createDependencyGraph(fromSourceList.select(new Predicate<Change>() {
            @Override
            public boolean accept(Change it) {
                return it.getChangeType().equals(changeType);
            }
        }), false);
        LOG.debug("END BLOCK: Ignore any 'Invalid change found?' errors in this block of code");

        MutableCollection<Change> changesForType = changedObjects.select(new Predicate<Change>() {
            @Override
            public boolean accept(Change it) {
                return it.getChangeType().equals(changeType);
            }
        });

        ImmutableSet<Change> changesForTypeSet = HashingStrategySets.immutable.withAll(HashingStrategies.fromFunction(new Function<Change, ObjectKey>() {
            @Override
            public ObjectKey valueOf(Change change1) {
                return change1.getObjectKey();
            }
        }), changesForType);

        MutableSet<Change> newChangesToAdd = HashingStrategySets.mutable.of(HashingStrategies.fromFunction(new Function<Change, ObjectKey>() {
            @Override
            public ObjectKey valueOf(Change change1) {
                return change1.getObjectKey();
            }
        }));
        for (Change change : changesForType) {
            BreadthFirstIterator<Change, DefaultEdge> dependencyIterator = new BreadthFirstIterator<Change, DefaultEdge>(graph, change);

            MutableSet<Change> dependencies = CollectionUtil.iteratorToList(dependencyIterator).toSet();
            dependencies.remove(change);  // the iterator result includes the self; we can remove this

            for (Change changeToAddBack : dependencies) {
                if (!changesForTypeSet.contains(changeToAddBack)) {
                    newChangesToAdd.add(changeToAddBack);
                }
            }
        }
        return newChangesToAdd;
    }

    private static class RerunnableObjectInfo {
        private final MutableCollection<Change> droppedObjects = Lists.mutable.empty();
        private final MutableCollection<Change> changedObjects = Lists.mutable.empty();

        void addDroppedObject(Change object) {
            this.droppedObjects.add(object);
        }

        void addChangedObject(Change object) {
            this.changedObjects.add(object);
        }

        MutableCollection<Change> getDroppedObjects() {
            return this.droppedObjects;
        }

        MutableCollection<Change> getChangedObjects() {
            return this.changedObjects;
        }
    }
}
