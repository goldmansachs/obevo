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
package com.gs.obevo.impl.changesorter;

import java.util.Comparator;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeKey;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.graph.GraphEnricher;
import com.gs.obevo.impl.graph.GraphEnricherImpl;
import com.gs.obevo.impl.graph.GraphSorter;
import com.gs.obevo.impl.graph.GraphUtil;
import com.gs.obevo.impl.graph.SortableDependencyGroup;
import com.gs.obevo.impl.text.TextDependencyExtractor;
import com.gs.obevo.impl.text.TextDependencyExtractorImpl;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.eclipse.collections.impl.factory.Lists;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public class ChangeCommandSorterImpl implements ChangeCommandSorter {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeCommandSorterImpl.class);
    private final Platform dialect;
    private final GraphEnricher enricher;
    private final Function<Change, String> getDefinitionFromEnvironment;
    private final GraphSorter graphSorter = new GraphSorter();

    public ChangeCommandSorterImpl(Platform dialect, Function<Change, String> getDefinitionFromEnvironment) {
        this.dialect = dialect;
        this.getDefinitionFromEnvironment = getDefinitionFromEnvironment;
        this.enricher = new GraphEnricherImpl(dialect.convertDbObjectName());
    }

    @Override
    public ImmutableList<ExecuteChangeCommand> sort(RichIterable<ExecuteChangeCommand> changeCommands, boolean rollback) {
        final RichIterable<DbCommandSortKey> commandDatas = changeCommands.collect(new Function<ExecuteChangeCommand, DbCommandSortKey>() {
            @Override
            public DbCommandSortKey valueOf(ExecuteChangeCommand changeCommand) {
                return new DbCommandSortKey(changeCommand);
            }
        });

        PartitionIterable<DbCommandSortKey> dataCommandPartition = commandDatas.partition(new Predicate<DbCommandSortKey>() {
            @Override
            public boolean accept(DbCommandSortKey sortKey) {
                return sortKey.getChangeType().getName().equals(ChangeType.STATICDATA_STR);
            }
        });

        PartitionIterable<DbCommandSortKey> dropPartition = dataCommandPartition.getRejected().partition(new Predicate<DbCommandSortKey>() {
            @Override
            public boolean accept(DbCommandSortKey dbCommandSortKey1) {
                return dbCommandSortKey1.isDrop();
            }
        });

        ListIterable<DbCommandSortKey> orderedAdds = sortAddCommands(dropPartition.getRejected(), rollback);
        ListIterable<DbCommandSortKey> orderedDrops = sortDropCommands(dropPartition.getSelected());
        ListIterable<DbCommandSortKey> orderedDatas = sortDataCommands(dataCommandPartition.getSelected());

        return Lists.mutable.withAll(orderedDrops).withAll(orderedAdds).withAll(orderedDatas).collect(new Function<DbCommandSortKey, ExecuteChangeCommand>() {
            @Override
            public ExecuteChangeCommand valueOf(DbCommandSortKey dbCommandSortKey) {
                return dbCommandSortKey.getChangeCommand();
            }
        }).toImmutable();
    }

    private ListIterable<DbCommandSortKey> sortAddCommands(RichIterable<DbCommandSortKey> addCommands, boolean rollback) {
        DirectedGraph<DbCommandSortKey, DefaultEdge> addGraph = enricher.createDependencyGraph(addCommands, rollback);

        // enrich the actual dependency information for usage later in MainDeployer class. TODO clean this up in GITHUB#153
        for (DbCommandSortKey addCommand : addCommands) {
            ImmutableSet<DbCommandSortKey> dependencyNodes = GraphUtil.getDependencyNodes(addGraph, addCommand).toSet().toImmutable();
            ImmutableSet<ChangeKey> changeKeys = dependencyNodes.flatCollect(new Function<DbCommandSortKey, Iterable<Change>>() {
                @Override
                public Iterable<Change> valueOf(DbCommandSortKey dep) {
                    return dep.getChangeCommand().getChanges();
                }
            }).collect(new Function<Change, ChangeKey>() {
                @Override
                public ChangeKey valueOf(Change change) {
                    return change.getChangeKey();
                }
            });
            addCommand.getChangeCommand().setDependencyChangeKeys(changeKeys.toSet().toImmutable());
        }

        ListIterable<DbCommandSortKey> addChanges = graphSorter.sortChanges(addGraph, SortableDependencyGroup.GRAPH_SORTER_COMPARATOR);
        addChanges.forEachWithIndex(new ObjectIntProcedure<DbCommandSortKey>() {
            @Override
            public void value(DbCommandSortKey dbCommandSortKey1, int order) {
                dbCommandSortKey1.setOrder(order);
            }
        });

        return addCommands.toSortedListBy(new Function<DbCommandSortKey, Integer>() {
            @Override
            public Integer valueOf(DbCommandSortKey dbCommandSortKey) {
                return dbCommandSortKey.getOrder();
            }
        });
    }

    private ListIterable<DbCommandSortKey> sortDropCommands(RichIterable<DbCommandSortKey> dropCommands) {
        final PartitionIterable<DbCommandSortKey> dropByChangeTypePartition = dropCommands.partition(new Predicate<DbCommandSortKey>() {
            @Override
            public boolean accept(DbCommandSortKey sortKey) {
                return sortKey.getChangeType().isRerunnable();
            }
        });

        final RichIterable<DbCommandSortKey> incrementalDrops = dropByChangeTypePartition.getRejected();
        final RichIterable<DbCommandSortKey> rerunnableDrops = dropByChangeTypePartition.getSelected();

        if (dialect.isDropOrderRequired()) {

            // do not do this for tables...

            for (DbCommandSortKey dbCommandSortKey : rerunnableDrops) {
                Change drop = dbCommandSortKey.getChangeCommand().getChanges().getFirst();
                String sql = getDefinitionFromEnvironment.valueOf(drop);
                LOG.debug("Found the sql from the DB for dropping: {}", sql);
                drop.setContent(sql != null ? sql : "");
            }

            TextDependencyExtractor textDependencyExtractor = new TextDependencyExtractorImpl(dialect.convertDbObjectName());
            textDependencyExtractor.calculateDependencies(rerunnableDrops.collect(new Function<DbCommandSortKey, Change>() {
                @Override
                public Change valueOf(DbCommandSortKey object) {
                    return object.getChangeCommand().getChanges().getFirst();
                }
            }));

            // enrichment is needed here
            DirectedGraph<DbCommandSortKey, DefaultEdge> dropGraph = enricher.createDependencyGraph(rerunnableDrops, false);

            ListIterable<DbCommandSortKey> dropChanges = graphSorter.sortChanges(dropGraph, SortableDependencyGroup.GRAPH_SORTER_COMPARATOR);
            dropChanges.forEachWithIndex(new ObjectIntProcedure<DbCommandSortKey>() {
                @Override
                public void value(DbCommandSortKey dbCommandSortKey, int order) {
                    dbCommandSortKey.setOrder(order);
                }
            });
        } else {
            // Sort by the object type to facilitate any dependencies that would naturally occur, e.g. for packages vs. package bodies in Oracle
            Comparator<DbCommandSortKey> dropKeyComparator = Comparators.fromFunctions(new Function<DbCommandSortKey, Integer>() {
                @Override
                public Integer valueOf(DbCommandSortKey it) {
                    return it.getChangeType().getDeployOrderPriority();
                }
            }, new Function<DbCommandSortKey, String>() {
                @Override
                public String valueOf(DbCommandSortKey dbCommandSortKey1) {
                    return dbCommandSortKey1.getObjectName();
                }
            });
            rerunnableDrops.toSortedList(dropKeyComparator).forEachWithIndex(new ObjectIntProcedure<DbCommandSortKey>() {
                @Override
                public void value(DbCommandSortKey dbCommandSortKey, int order) {
                    dbCommandSortKey.setOrder(order);
                }
            });
        }

        incrementalDrops.toSortedListBy(new Function<DbCommandSortKey, String>() {
            @Override
            public String valueOf(DbCommandSortKey dbCommandSortKey2) {
                return dbCommandSortKey2.getObjectName();
            }
        }).forEachWithIndex(new ObjectIntProcedure<DbCommandSortKey>() {
            @Override
            public void value(DbCommandSortKey dbCommandSortKey1, int order) {
                dbCommandSortKey1.setOrder(order);
            }
        });

        return dropCommands.toSortedListBy(new Function<DbCommandSortKey, Integer>() {
            @Override
            public Integer valueOf(DbCommandSortKey dbCommandSortKey) {
                return dbCommandSortKey.getOrder();
            }
        }).reverseThis();
    }

    private ListIterable<DbCommandSortKey> sortDataCommands(RichIterable<DbCommandSortKey> dataCommands) {
        // The background behind this order check: while we want to rely on the FK-dependencies generally to
        // figure out the order of deployment (and once we group by connected components,
        // then the dependencies between components shouldn't matter), there are a couple use cases where we
        // still need to rely on the "order" attribute:
        // 1) for backwards-compatibility w/ teams using older versions of this tool that didn't have
        // this more-advanced ordering logic and that instead needed the "order" attribute
        // 2) the MetadataGroup use case (see "MetadataGroupTest")
        // Hence, we will still rely on the "changeOrder" attribute here as a fallback for the order
        MutableList<DbCommandSortKey> sortedDataCommands = dataCommands.toSortedListBy(new Function<DbCommandSortKey, Integer>() {
            @Override
            public Integer valueOf(DbCommandSortKey dbCommandSortKey) {
                ListIterable<Change> changes = dbCommandSortKey.getChangeCommand().getChanges();
                if (changes.isEmpty() || changes.size() > 1) {
                    return Change.DEFAULT_CHANGE_ORDER;
                } else {
                    return changes.getFirst().getOrder();
                }
            }
        });

        sortedDataCommands.forEachWithIndex(new ObjectIntProcedure<DbCommandSortKey>() {
            @Override
            public void value(DbCommandSortKey dbCommandSortKey1, int order) {
                dbCommandSortKey1.setOrder(order);
            }
        });

        return dataCommands.toSortedListBy(new Function<DbCommandSortKey, Integer>() {
            @Override
            public Integer valueOf(DbCommandSortKey dbCommandSortKey) {
                return dbCommandSortKey.getOrder();
            }
        });
    }
}
