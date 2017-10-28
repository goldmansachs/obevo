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
package com.gs.obevo.db.impl.core;

import java.sql.Connection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.ToolVersion;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.checksum.ChecksumBreak;
import com.gs.obevo.db.impl.core.checksum.ChecksumEntry;
import com.gs.obevo.db.impl.core.checksum.ChecksumEntryInclusionPredicate;
import com.gs.obevo.db.impl.core.checksum.DbChecksumManager;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.Changeset;
import com.gs.obevo.impl.ChangesetCreator;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.DeployStrategy;
import com.gs.obevo.impl.MainDeployer;
import com.gs.obevo.impl.MainInputReader;
import com.gs.obevo.impl.PostDeployAction;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.lookuppredicate.LookupIndex;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deployer implementation targeted specifically for DBMS platforms.
 */
public class DbDeployer extends MainDeployer<DbPlatform, DbEnvironment> {
    public static final String INIT_WARNING_MESSAGE =
            "WARNING: An INIT was requested; however, the system has been initialized in the past.\n" +
            "Please double-check that you need this INIT.";

    private static final Logger LOG = LoggerFactory.getLogger(DbDeployer.class);

    private final DbMetadataManager dbMetadataManager;
    private final SqlExecutor sqlExecutor;
    private final DbChecksumManager dbChecksumManager;
    private DbEnvironment env;

    public DbDeployer(ChangeAuditDao artifactDeployerDao, MainInputReader mainInputReader, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry, ChangesetCreator changesetCreator, PostDeployAction postDeployAction, DbMetadataManager dbMetadataManager, SqlExecutor sqlExecutor, DeployMetricsCollector deployMetricsCollector, DbChecksumManager dbChecksumManager, DeployExecutionDao deployExecutionDao, Credential credential) {
        super(artifactDeployerDao, mainInputReader, changeTypeBehaviorRegistry, changesetCreator, postDeployAction, deployMetricsCollector, deployExecutionDao, credential);
        this.dbMetadataManager = dbMetadataManager;
        this.sqlExecutor = sqlExecutor;
        this.dbChecksumManager = dbChecksumManager;
    }

    @Override
    protected void initializeSchema(final Environment env, PhysicalSchema schema) {
        this.sqlExecutor.executeWithinContext(schema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                ((DbEnvironment) env).getDbTranslationDialect().initSchema(sqlExecutor.getJdbcTemplate(), conn);
            }
        });
    }

    @Override
    protected void printArtifactsToProcessForUser(Changeset artifactsToProcess, DeployStrategy deployStrategy, DbEnvironment env, ImmutableCollection<Change> deployedChanges, ImmutableCollection<Change> sourceChanges) {
        this.env = env;  // setting this as a kludge here until proper subsequent refactoring
        super.printArtifactsToProcessForUser(artifactsToProcess, deployStrategy, env, deployedChanges, sourceChanges);


        if (deployStrategy.isInitAllowedOnHashExceptions() && !isInitRequired(env)) {
            LOG.info("*******************************************");
            LOG.info(INIT_WARNING_MESSAGE);
            LOG.info("*******************************************");
            LOG.info("");
        }
    }

    private boolean isInitRequired(DbEnvironment env) {
        return !isAuditTablePresent(env) && getExistingTablesInEnvironment(env).notEmpty();
    }

    private boolean isAuditTablePresent(DbEnvironment env) {
        return env.getPhysicalSchemas().allSatisfy(new Predicate<PhysicalSchema>() {
            @Override
            public boolean accept(PhysicalSchema each) {
                return dbMetadataManager.getTableInfo(each, getArtifactDeployerDao().getAuditContainerName(), new DaSchemaInfoLevel().setRetrieveTables(true)) != null;
            }
        });
    }

    private ImmutableSet<DaTable> getExistingTablesInEnvironment(DbEnvironment env) {
        return env.getPhysicalSchemas().flatCollect(new Function<PhysicalSchema, Iterable<DaTable>>() {
            @Override
            public Iterable<DaTable> valueOf(PhysicalSchema physicalSchema) {
                DaCatalog database = dbMetadataManager.getDatabase(physicalSchema, new DaSchemaInfoLevel().setRetrieveTables(true), true, false);
                return database.getTables().reject(DaTable.IS_VIEW);
            }
        });
    }

    private static final Function2<Change, Function<String, String>, ObjectKey> CHANGE_TO_IDENTITY = new Function2<Change, Function<String, String>, ObjectKey>() {
        @Override
        public ObjectKey value(Change change, Function<String, String> convertDbObjectName) {
            return new ObjectKey(change.getPhysicalSchema().getPhysicalName(), change.getChangeType(), convertDbObjectName.valueOf(change.getObjectName()));
        }
    };

    @Override
    protected void validatePriorToDeployment(DbEnvironment env, DeployStrategy deployStrategy, ImmutableList<Change> sourceChanges, ImmutableCollection<Change> deployedChanges, Changeset artifactsToProcess) {
        super.validatePriorToDeployment(env, deployStrategy, sourceChanges, deployedChanges, artifactsToProcess);

        if (env.isChecksumDetectionEnabled() && dbChecksumManager.isInitialized()) {
            Predicate<? super ChecksumEntry> platformInclusionPredicate = getPlatformInclusionPredicate(env);
            ImmutableCollection<ChecksumBreak> checksumBreaks = this.dbChecksumManager.determineChecksumDifferences(platformInclusionPredicate)
                    .reject(ChecksumBreak.IS_EXPECTED_BREAK);

            if (checksumBreaks.notEmpty()) {
                LOG.info("*******************************************");
                LOG.info("WARNING: The following objects were modified or managed outside of {}.", ToolVersion.getToolName());
                LOG.info("Please revert these changes or incorporate into your {} codebase", ToolVersion.getToolName());

                for (ChecksumBreak checksumBreak : checksumBreaks) {
                    LOG.info("\t" + checksumBreak.toDisplayString());
                }

                LOG.info("*******************************************");
                LOG.info("");
            }
        }
    }

    @Override
    protected void doPostDeployAction(DbEnvironment env, final ImmutableList<Change> sourceChanges) {
        super.doPostDeployAction(env, sourceChanges);
        if (env.isChecksumDetectionEnabled()) {
            if (!dbChecksumManager.isInitialized()) {
                // need this check done here to account for existing clients
                dbChecksumManager.initialize();
            }

            dbChecksumManager.applyChecksumDiffs(Predicates.and(getPlatformInclusionPredicate(env), getSourceChangesInclusionPredicate(sourceChanges)));
        }
    }

    private ChecksumEntryInclusionPredicate createLookupIndexForObjectType(ImmutableList<Change> sourceChanges, String changeTypeName) {
        LookupIndex objectTypeIndex = new LookupIndex(Sets.immutable.with(changeTypeName));
        ImmutableList<Change> objectTypeChanges = sourceChanges.select(Predicates.attributeEqual(Change.TO_CHANGE_TYPE_NAME, changeTypeName));
        MutableSet<String> objectNames = objectTypeChanges.collect(Change.objectName()).collect(env.getPlatform().convertDbObjectName()).toSet();
        LookupIndex objectNameIndex = new LookupIndex(objectNames.toImmutable());
        return new ChecksumEntryInclusionPredicate(
                Lists.immutable.with(objectTypeIndex),
                Lists.immutable.with(objectNameIndex)
        );
    }

    private Predicate<? super ChecksumEntry> getPlatformInclusionPredicate(DbEnvironment env) {
        // 1) exclude those tables that are excluded by default from source code, e.g. explain tables or others that users configure
        ImmutableSet<Predicate<? super ChecksumEntry>> schemaObjectNamePredicates = env.getSchemas().collect(new Function<Schema, Predicate<? super ChecksumEntry>>() {
            @Override
            public Predicate<? super ChecksumEntry> valueOf(Schema schema) {
                return schema.getObjectExclusionPredicateBuilder().build(ChecksumEntry.TO_OBJECT_TYPE, ChecksumEntry.TO_NAME1);
            }
        });

        // 2) exclude the audit tables
        MutableMultimap<String, String> tablesToExclude = Multimaps.mutable.set.empty();

        tablesToExclude.putAll(ChangeType.TABLE_STR, Sets.immutable.with(
                env.getPlatform().convertDbObjectName().valueOf(getArtifactDeployerDao().getAuditContainerName()),
                env.getPlatform().convertDbObjectName().valueOf(dbChecksumManager.getChecksumContainerName()),
                env.getPlatform().convertDbObjectName().valueOf(getDeployExecutionDao().getExecutionContainerName()),
                env.getPlatform().convertDbObjectName().valueOf(getDeployExecutionDao().getExecutionAttributeContainerName())
        ));
        ObjectTypeAndNamePredicateBuilder auditTablePredicateBuilder = new ObjectTypeAndNamePredicateBuilder(tablesToExclude.toImmutable(), ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE);
        Predicates<? super ChecksumEntry> auditTablePredicate = auditTablePredicateBuilder.build(ChecksumEntry.TO_OBJECT_TYPE, ChecksumEntry.TO_NAME1);

        return Predicates.and(auditTablePredicate, Predicates.and(schemaObjectNamePredicates));
    }

    public Predicate<ChecksumEntry> getSourceChangesInclusionPredicate(final ImmutableList<Change> sourceChanges) {
        // 3) only include the predicate types that we care about
        ImmutableSet<String> requiredValidationObjectTypes = env.getPlatform().getRequiredValidationObjectTypes();
        ImmutableSet<ChecksumEntryInclusionPredicate> checksumEntryPredicates = requiredValidationObjectTypes.collect(new Function<String, ChecksumEntryInclusionPredicate>() {
            @Override
            public ChecksumEntryInclusionPredicate valueOf(String changeType) {
                return createLookupIndexForObjectType(sourceChanges, changeType);
            }
        });

        return Predicates.or(checksumEntryPredicates);
    }
}
