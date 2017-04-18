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
package com.gs.obevo.db.impl.core.changetypes;

import java.util.Set;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.GroupChange;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ChangePair;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.changecalc.ChangeCommandFactory;
import com.gs.obevo.impl.command.UnmanageChangeCommand;
import com.gs.obevo.impl.graph.GraphEnricher;
import com.gs.obevo.impl.graph.GraphSorter;
import com.gs.obevo.impl.graph.SortableDependency;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticDataChangeTypeCommandCalculator implements ChangeTypeCommandCalculator {
    private static final Logger LOG = LoggerFactory.getLogger(StaticDataChangeTypeCommandCalculator.class);

    private final ChangeCommandFactory changeCommandFactory;
    private final GraphEnricher enricher;
    private final GraphSorter sorter = new GraphSorter();

    public StaticDataChangeTypeCommandCalculator(ChangeCommandFactory changeCommandFactory, GraphEnricher enricher) {
        this.changeCommandFactory = changeCommandFactory;
        this.enricher = enricher;
    }

    @Override
    public ImmutableList<ChangeCommand> calculateCommands(ChangeType changeType, RichIterable<ChangePair> changePairs, RichIterable<Change> sources, boolean rollback, boolean initAllowedOnHashExceptions) {
        RerunnableObjectInfo rerunnableObjectInfo = changePairs.injectInto(new RerunnableObjectInfo(),
                new Function2<RerunnableObjectInfo, ChangePair, RerunnableObjectInfo>() {
                    @Override
                    public RerunnableObjectInfo value(RerunnableObjectInfo rerunnableObjectInfo,
                            ChangePair changePair) {
                        // TODO make this a bit more OO, e.g. avoid the instanceof all over the place
                        Change source = changePair.getSourceChange();
                        Change deployed = changePair.getDeployedChange();

                        if (source == null && deployed == null) {
                            // this branch and exception throwing here is to avoid null deference warnings in findbugs for the next else branch
                            throw new IllegalStateException("This code branch should never happen; either of source or deployed should exist");
                        }

                        if (source == null && deployed != null) {
                            // In this case - the change exists in the target DB but was removed from the source
                            rerunnableObjectInfo.addDroppedObject(deployed);
                        } else if (source != null && deployed == null) {
                            rerunnableObjectInfo.addChangedObject(source);
                        } else if (ObjectUtils.equals(source.getContentHash(), deployed.getContentHash())
                                || source.getAcceptableHashes().contains(deployed.getContentHash())) {
                            // In this case - the change exists in both the source and target db.
                            // We need to check if anything has changed, using the hash
                            LOG.trace("Nothing to do here; source [{}] and target [{}] match in hash", source, deployed);
                        } else {
                            rerunnableObjectInfo.addChangedObject(source);
                        }

                        return rerunnableObjectInfo;
                    }
                });

        return this.processRerunnableChanges(rerunnableObjectInfo);
    }

    /**
     * @param rerunnableObjectInfo
     */
    private ImmutableList<ChangeCommand> processRerunnableChanges(RerunnableObjectInfo rerunnableObjectInfo) {
        final MutableList<ChangeCommand> commands = Lists.mutable.empty();
        MutableCollection<Change> drops = rerunnableObjectInfo.getDroppedObjects();
        drops.toSortedListBy(Change.objectName())
                .forEachWithIndex(new ObjectIntProcedure<Change>() {
                    @Override
                    public void value(Change droppedObject, int order) {
                        commands.add(new UnmanageChangeCommand(droppedObject, "static data change to be unmanaged"));
                    }
                });

        return commands.withAll(this.handleChanges(rerunnableObjectInfo.getChangedObjects())).toImmutable();
    }

    private MutableList<ExecuteChangeCommand> handleChanges(MutableCollection<Change> fromSourceList) {
        final MutableList<ExecuteChangeCommand> commands = Lists.mutable.empty();

        DirectedGraph<Change, DefaultEdge> graph = enricher.createDependencyGraph(fromSourceList, false);

        if (graph != null) {
            ConnectivityInspector<Change, DefaultEdge> connectivityInspector
                    = new ConnectivityInspector<Change, DefaultEdge>(graph);
            for (Set<Change> connectedSet : connectivityInspector.connectedSets()) {
                // once we have a connectedSet, sort within those changes to ensure that we still sort them in the
                // right order (i.e. via topological sort)
                ImmutableList<Change> fullChanges = sorter.sortChanges(graph, SetAdapter.adapt(connectedSet), SortableDependency.GRAPH_SORTER_COMPARATOR);
                commands.add(changeCommandFactory.createDeployCommand(new GroupChange(fullChanges)));
            }
        }

        return commands;
    }

    private static class RerunnableObjectInfo {
        private final MutableCollection<Change> droppedObjects = Lists.mutable.empty();
        private final MutableCollection<Change> changedObjects = Lists.mutable.empty();

        public void addDroppedObject(Change object) {
            this.droppedObjects.add(object);
        }

        public void addChangedObject(Change object) {
            this.changedObjects.add(object);
        }

        public MutableCollection<Change> getDroppedObjects() {
            return droppedObjects;
        }

        public MutableCollection<Change> getChangedObjects() {
            return changedObjects;
        }
    }
}
