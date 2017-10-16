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
package com.gs.obevo.db.impl.core.cleaner;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.ChangeRerunnable;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaForeignKey;
import com.gs.obevo.dbmetadata.api.DaNamedObject;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.Changeset;
import com.gs.obevo.impl.ChangesetCreator;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.util.inputreader.ConsoleInputReader;
import com.gs.obevo.util.inputreader.UserInputReader;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation to drop the objects in a schema.
 */
public class DbEnvironmentCleaner implements EnvironmentCleaner {
    private static final Logger LOG = LoggerFactory.getLogger(DbEnvironmentCleaner.class);

    private final DbEnvironment env;
    private final SqlExecutor statementExecutor;
    private final DbMetadataManager dbMetadataManager;
    private final ChangesetCreator changesetCreator;
    private final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    private final UserInputReader userInputReader = new ConsoleInputReader();

    public DbEnvironmentCleaner(DbEnvironment env, SqlExecutor statementExecutor,
            DbMetadataManager dbMetadataManager, ChangesetCreator changesetCreator, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        this.env = env;
        this.statementExecutor = statementExecutor;
        this.dbMetadataManager = dbMetadataManager;
        this.changesetCreator = changesetCreator;
        this.changeTypeBehaviorRegistry = changeTypeBehaviorRegistry;
    }

    private ImmutableList<DbCleanCommand> getDropStatements(PhysicalSchema physicalSchema) {
        DaCatalog database = this.dbMetadataManager.getDatabase(physicalSchema, new DaSchemaInfoLevel().setRetrieveAllObjectsMinimum().setRetrieveTableForeignKeys(true), true, true);

        MutableSet<DbCleanCommand> cleanCommands = Sets.mutable.empty();

        cleanCommands.withAll(getRoutineDrops(database, physicalSchema));
        cleanCommands.withAll(getTableDrops(database, physicalSchema));
        cleanCommands.withAll(getObjectDrops(database.getPackages(), ChangeType.PACKAGE_STR, physicalSchema));
        cleanCommands.withAll(getObjectDrops(database.getSequences(), ChangeType.SEQUENCE_STR, physicalSchema));
        cleanCommands.withAll(getObjectDrops(database.getSynonyms(), ChangeType.SYNONYM_STR, physicalSchema));
        cleanCommands.withAll(getObjectDrops(database.getRules(), ChangeType.RULE_STR, physicalSchema));
        cleanCommands.withAll(getObjectDrops(database.getUserTypes(), ChangeType.USERTYPE_STR, physicalSchema));

        return cleanCommands.toList().toImmutable();
    }

    private ImmutableCollection<DbCleanCommand> getTableDrops(DaCatalog database, final PhysicalSchema physicalSchema) {
        final ChangeType viewType = this.env.getPlatform().getChangeType(ChangeType.VIEW_STR);
        final ChangeType fkType = this.env.getPlatform().getChangeType(ChangeType.FOREIGN_KEY_STR);
        final ChangeType tableType = this.env.getPlatform().getChangeType(ChangeType.TABLE_STR);
        return database.getTables().flatCollect(new Function<DaTable, Iterable<DbCleanCommand>>() {
            @Override
            public Iterable<DbCleanCommand> valueOf(DaTable table) {
                if (table.isView()) {
                    return Lists.immutable.with(new DbCleanCommand(physicalSchema, viewType, table.getName()));
                } else {
                    MutableList<DbCleanCommand> cleanCommands = Lists.mutable.empty();
                    for (DaForeignKey foreignKey : table.getImportedForeignKeys()) {
                        cleanCommands.add(new DbCleanCommand(physicalSchema, fkType, table.getName(),
                                "ALTER TABLE " + table.getName() + " DROP CONSTRAINT " + foreignKey.getName()));
                    }

                    cleanCommands.add(new DbCleanCommand(physicalSchema, tableType, table.getName()));

                    return cleanCommands;
                }
            }
        });
    }

    private ImmutableCollection<DbCleanCommand> getRoutineDrops(DaCatalog database, final PhysicalSchema physicalSchema) {
        final ChangeType functionType = env.getPlatform().getChangeType(ChangeType.FUNCTION_STR);
        final ChangeType spType = env.getPlatform().getChangeType(ChangeType.SP_STR);

        return database.getRoutines().collect(new Function<DaRoutine, DbCleanCommand>() {
            @Override
            public DbCleanCommand valueOf(DaRoutine routine) {
                switch (routine.getRoutineType()) {
                case function:
                    return new DbCleanCommand(physicalSchema, functionType, routine.getName());
                case procedure:
                    return new DbCleanCommand(physicalSchema, spType, routine.getName());
                default:
                    throw new IllegalArgumentException("Unexpected routine type here: " + routine.getRoutineType() + ":" + routine);
                }
            }
        });
    }

    private ImmutableCollection<DbCleanCommand> getObjectDrops(ImmutableCollection<? extends DaNamedObject> dbObjects, String changeTypeName, final PhysicalSchema physicalSchema) {
        if (!env.getPlatform().hasChangeType(changeTypeName)) {
            return Lists.immutable.empty();
        }

        final ChangeType type = env.getPlatform().getChangeType(changeTypeName);

        return dbObjects.collect(new Function<DaNamedObject, DbCleanCommand>() {
            @Override
            public DbCleanCommand valueOf(DaNamedObject dbObject) {
                return new DbCleanCommand(physicalSchema, type, dbObject.getName());
            }
        });
    }

    @Override
    public void cleanEnvironment(final boolean noPrompt) {
        Validate.isTrue(env.isCleanBuildAllowed(), "Clean build not allowed for this environment [" + env.getName()
                + "] ! Exiting...");

        // some schemas have complex dependencies that we currently aren't handling w/ the drop code. To work
        // around it, we just retry the drop if we have progress in dropping objects.
        // Note that regular forward deploys can handle dependencies properly; we just need the logic to extract
        // the object definitions out for all object types to enable this.
        int tryCount = 0;
        while (true) {
            tryCount++;
            LOG.info("Attempting to clean objects from environment");
            final Pair<Boolean, MutableList<Exception>> clearResults = clearEnvironmentInternal(noPrompt);
            if (!clearResults.getOne()) {
                throw new DeployerRuntimeException("Could not clean schema; remaining exceptions: " + clearResults.getTwo().collect(TO_EXCEPTION_STACK_TRACE));
            } else if (clearResults.getTwo().isEmpty()) {
                return;
            } else if (tryCount <= 10) {
                LOG.info("Failed to clean up schema on try #" + tryCount + " but able to make progress, will continue to try");
            } else {
                throw new DeployerRuntimeException("Could not clean schema after max " + tryCount + " tries; will exit with remaining exceptions: " + clearResults.getTwo().collect(TO_EXCEPTION_STACK_TRACE));
            }
        }

    }

    private static final Function<Exception, String> TO_EXCEPTION_STACK_TRACE = new Function<Exception, String>() {
        @Override
        public String valueOf(Exception exc) {
            return ExceptionUtils.getStackTrace(exc);
        }
    };

    /**
     * Returns true if we are done or can retry, false if we should stop. Also returns the corresponding exceptions;
     * an empty list indicates we are done.
     */
    private Pair<Boolean, MutableList<Exception>> clearEnvironmentInternal(boolean noPrompt) {
        // the mapping from physical to regular schema is needed in case multiple schemas get mapped to a single
        // physical schema via overrides; note that this should be a rare use case
        final MutableMultimap<PhysicalSchema, Schema> physicalSchemaToSchemaMap = FastListMultimap.newMultimap();
        for (Schema schema : env.getSchemas()) {
            PhysicalSchema physicalSchema = env.getPhysicalSchema(schema.getName());
            physicalSchemaToSchemaMap.put(physicalSchema, schema);
        }

        // Get the unique drops based on the key in case the inputs have duplicated the object names (e.g. in case of specific names for functions)
        MutableSet<DbCleanCommand> drops = UnifiedSetWithHashingStrategy.newSet(HashingStrategies.fromFunction(
                DbCleanCommand.TO_KEY
        ));
        drops.addAll(physicalSchemaToSchemaMap.keysView().flatCollect(new Function<PhysicalSchema,
                RichIterable<DbCleanCommand>>() {
            @Override
            public RichIterable<DbCleanCommand> valueOf(PhysicalSchema physicalSchema) {
                ImmutableList<DbCleanCommand> schemaDrops = getDropStatements(physicalSchema);

                MutableCollection<Schema> schemas = physicalSchemaToSchemaMap.get(physicalSchema);

                for (Schema schema : schemas) {
                    schemaDrops = schemaDrops.select(schema.getObjectExclusionPredicateBuilder().build(Functions.chain(DbCleanCommand.TO_OBJECT_TYPE, ChangeType.TO_NAME), DbCleanCommand.TO_OBJECT_NAME));
                }

                return schemaDrops;
            }
        }).toList());

        // we convert the info in the DB to the Change class so that we can feed it into the
        // ChangesetCreator (i.e. to get it to trigger the logic to drop the objects)
        MutableCollection<Change> artifacts = drops.collect(new Function<DbCleanCommand, Change>() {
            private int changeIndex = 1;

            @Override
            public Change valueOf(DbCleanCommand cleanCommand) {
                MutableCollection<Schema> schemas = physicalSchemaToSchemaMap.get(cleanCommand.getPhysicalSchema());
                if (cleanCommand.getObjectType().isRerunnable()) {
                    ChangeRerunnable change = new ChangeRerunnable(cleanCommand.getObjectType()
                            , schemas.getFirst().getName()
                            , cleanCommand.getObjectName()
                            , "hash"
                            , "n/a"
                    );
                    change.setEnvironment(env);
                    change.setChangeTypeBehavior(changeTypeBehaviorRegistry.getChangeTypeBehavior(cleanCommand.getObjectType().getName()));
                    return change;
                } else {
                    ChangeIncremental dbChangeIncremental = new ChangeIncremental(cleanCommand.getObjectType()
                            , schemas.getFirst().getName()
                            , cleanCommand.getObjectName()
                            , "change" + this.changeIndex++
                            , 0
                            , "hash"
                            , cleanCommand.getSqlStatement()
                    );
                    dbChangeIncremental.setDrop(true);
                    dbChangeIncremental.setManuallyCodedDrop(cleanCommand.getObjectType().getName().equals(ChangeType.FOREIGN_KEY_STR));
                    dbChangeIncremental.setForceDropForEnvCleaning(true);
                    dbChangeIncremental.setEnvironment(env);
                    dbChangeIncremental.setChangeTypeBehavior(changeTypeBehaviorRegistry.getChangeTypeBehavior(cleanCommand.getObjectType().getName()));

                    return dbChangeIncremental;
                }
            }
        });

        PartitionIterable<Change> rerunnableChangesPartition = artifacts.partition(Predicates.attributePredicate(Change.TO_CHANGE_TYPE, ChangeType.IS_RERUNNABLE));
        Changeset changeset = this.changesetCreator.determineChangeset(
                rerunnableChangesPartition.getSelected(),
                rerunnableChangesPartition.getRejected(),
                false, false, null);

        changeset.validateForDeployment();
        LOG.info("Dropping these objects:");
        for (ExecuteChangeCommand changeCommand : changeset.getInserts()) {
            LOG.info("\t: {}", changeCommand.getCommandDescription());
        }
        LOG.info("");


        if (!noPrompt) {
            LOG.info("WARNING - The above database objects will get dropped!!!! ARE YOU SURE that you want to proceed? (Y/N)");

            String input = this.userInputReader.readLine(null);
            Validate.isTrue(input.trim().equalsIgnoreCase("Y"), "User did not enter Y. Hence, we will exit from here.");
        }

        MutableList<Exception> exceptions = Lists.mutable.empty();
        for (ExecuteChangeCommand executeChangeCommand : changeset.getInserts()) {
            LOG.info("Executing the drop: {}", executeChangeCommand.getCommandDescription());
            this.statementExecutor.performExtraCleanOperation(executeChangeCommand, dbMetadataManager);
            try {
                executeChangeCommand.execute();
            } catch (Exception exc) {
                LOG.info("Found error {}, will proceed with other objects (stack trace to come below)", exc.getMessage());
                exceptions.add(exc);
            }
        }

        if (exceptions.size() == 0) {
            return Tuples.pair(true, exceptions);
        } else if (exceptions.size() != changeset.getInserts().size()) {
            return Tuples.pair(true, exceptions);
        } else {
            return Tuples.pair(false, exceptions);
        }
    }
}
