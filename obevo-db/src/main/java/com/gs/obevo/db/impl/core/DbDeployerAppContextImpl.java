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
package com.gs.obevo.db.impl.core;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.FileSourceContext;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.impl.core.changeauditdao.NoOpChangeAuditDao;
import com.gs.obevo.db.impl.core.changeauditdao.SameSchemaChangeAuditDao;
import com.gs.obevo.db.impl.core.changeauditdao.SameSchemaDeployExecutionDao;
import com.gs.obevo.db.impl.core.changetypes.CsvStaticDataDeployer;
import com.gs.obevo.db.impl.core.changetypes.DbSimpleArtifactDeployer;
import com.gs.obevo.db.impl.core.changetypes.GrantChangeParser;
import com.gs.obevo.db.impl.core.changetypes.IncrementalDbChangeTypeBehavior;
import com.gs.obevo.db.impl.core.changetypes.RerunnableDbChangeTypeBehavior;
import com.gs.obevo.db.impl.core.changetypes.StaticDataChangeTypeBehavior;
import com.gs.obevo.db.impl.core.checksum.DbChecksumDao;
import com.gs.obevo.db.impl.core.checksum.DbChecksumManager;
import com.gs.obevo.db.impl.core.checksum.DbChecksumManagerImpl;
import com.gs.obevo.db.impl.core.checksum.SameSchemaDbChecksumDao;
import com.gs.obevo.db.impl.core.cleaner.DbEnvironmentCleaner;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.envinfrasetup.NoOpEnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory;
import com.gs.obevo.db.impl.core.jdbc.SingleConnectionDataSource;
import com.gs.obevo.db.impl.core.reader.BaselineTableChangeParser;
import com.gs.obevo.db.impl.core.reader.PrepareDbChangeForDb;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry.ChangeTypeBehaviorRegistryBuilder;
import com.gs.obevo.impl.DeployerPlugin;
import com.gs.obevo.impl.PrepareDbChange;
import com.gs.obevo.impl.changepredicate.ChangeKeyPredicateBuilder;
import com.gs.obevo.impl.context.AbstractDeployerAppContext;
import com.gs.obevo.impl.reader.TableChangeParser.GetChangeType;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;

public abstract class DbDeployerAppContextImpl extends AbstractDeployerAppContext<DbEnvironment, DbDeployerAppContext> implements DbDeployerAppContext {
    private boolean strictSetupEnvInfra = DbDeployerAppContext.STRICT_SETUP_ENV_INFRA_DEFAULT;

    /**
     * Renamed.
     *
     * @deprecated Renamed to {@link #isStrictSetupEnvInfra()}
     */
    @Deprecated
    public boolean isFailOnSetupException() {
        return isStrictSetupEnvInfra();
    }

    private boolean isStrictSetupEnvInfra() {
        return strictSetupEnvInfra;
    }

    @Override
    public DbDeployerAppContext setFailOnSetupException(boolean failOnSetupException) {
        return setStrictSetupEnvInfra(failOnSetupException);
    }

    @Override
    public DbDeployerAppContext setStrictSetupEnvInfra(boolean strictSetupEnvInfra) {
        this.strictSetupEnvInfra = strictSetupEnvInfra;
        return this;
    }

    @Override
    public DbDeployerAppContext buildDbContext() {
        Validate.notNull(env.getPlatform(), "dbType must be populated");

        if (credential == null) {
            if (this.env.getDefaultUserId() != null && this.env.getDefaultPassword() != null) {
                credential = new Credential(this.env.getDefaultUserId(), this.env.getDefaultPassword());
            } else {
                throw new IllegalArgumentException("Cannot build DB context without credential; was not passed in as argument, and no defaultUserId & defaultPassword specified in the environment");
            }
        }

        validateChangeTypes(platform().getChangeTypes(), getChangeTypeBehaviorRegistry());

        getChangesetCreator();
        getEnvironmentCleaner();
        getManagedDataSource();

        return this;
    }

    private void validateChangeTypes(ImmutableList<ChangeType> changeTypes, final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        ImmutableList<ChangeType> unenrichedChangeTypes = changeTypes.reject(new Predicate<ChangeType>() {
            @Override
            public boolean accept(ChangeType changeType) {
                return changeTypeBehaviorRegistry.getChangeTypeBehavior(changeType.getName()) != null;
            }
        });
        if (unenrichedChangeTypes.notEmpty()) {
            throw new IllegalStateException("The following change types were not enriched: " + unenrichedChangeTypes);
        }

        CollectionUtil.verifyNoDuplicates(changeTypes, ChangeType.TO_NAME, "Not expecting multiple ChangeTypes with the same name");
    }

    protected DbPlatform platform() {
        return this.env.getPlatform();
    }

    protected DbSimpleArtifactDeployer simpleArtifactDeployer() {
        return new DbSimpleArtifactDeployer(platform(), getSqlExecutor());
    }

    protected ChangeTypeBehaviorRegistryBuilder getChangeTypeBehaviors() {
        ChangeTypeBehaviorRegistryBuilder builder = ChangeTypeBehaviorRegistry.newBuilder();

        PartitionImmutableList<ChangeType> staticDataPartition = platform().getChangeTypes().partition(Predicates.attributeEqual(ChangeType.TO_NAME, ChangeType.STATICDATA_STR));

        for (ChangeType staticDataType : staticDataPartition.getSelected()) {
            StaticDataChangeTypeBehavior behavior = new StaticDataChangeTypeBehavior(env, getSqlExecutor(), simpleArtifactDeployer(), getCsvStaticDataLoader());
            builder.put(staticDataType.getName(), groupSemantic(), behavior);
        }

        PartitionImmutableList<ChangeType> rerunnablePartition = staticDataPartition.getRejected().partition(ChangeType.IS_RERUNNABLE);

        for (ChangeType rerunnableChange : rerunnablePartition.getSelected()) {
            if (!(rerunnableChange instanceof DbChangeType)) {
                throw new IllegalArgumentException("Bad change type " + rerunnableChange.getName() + ":" + rerunnableChange);
            }
            RerunnableDbChangeTypeBehavior behavior = new RerunnableDbChangeTypeBehavior(
                    env, (DbChangeType) rerunnableChange, getSqlExecutor(), simpleArtifactDeployer(), grantChangeParser(), graphEnricher(), platform(), getDbMetadataManager());
            builder.put(rerunnableChange.getName(), rerunnableSemantic(), behavior);
        }
        for (ChangeType incrementalChange : rerunnablePartition.getRejected()) {
            IncrementalDbChangeTypeBehavior behavior = new IncrementalDbChangeTypeBehavior(
                    env, (DbChangeType) incrementalChange, getSqlExecutor(), simpleArtifactDeployer(), grantChangeParser()
            );
            builder.put(incrementalChange.getName(), incrementalSemantic(), behavior);
        }

        return builder;
    }

    protected GrantChangeParser grantChangeParser() {
        return new GrantChangeParser(env, getArtifactTranslators());
    }

    @Override
    public DbDeployerAppContext buildFileContext() {
        Validate.notNull(env.getSourceDirs(), "sourceDirs must be populated");
        Validate.notNull(env.getName(), "name must be populated");
        Validate.notNull(env.getSchemas(), "schemas must be populated");

        getInputReader();

        return this;
    }

    @Override
    public ImmutableList<Change> readChangesFromAudit() {
        return getArtifactDeployerDao().getDeployedChanges();
    }

    @Override
    public ImmutableList<Change> readChangesFromSource() {
        return readChangesFromSource(false);
    }

    @Override
    public ImmutableList<Change> readChangesFromSource(boolean useBaseline) {
        return getInputReader().readInternal(getSourceReaderStrategy(getFileSourceParams(useBaseline)), new MainDeployerArgs().useBaseline(useBaseline));
    }

    @Override
    public void readSource(MainDeployerArgs deployerArgs) {
        getInputReader().readInternal(getSourceReaderStrategy(getFileSourceParams(false)), new MainDeployerArgs().useBaseline(false));
    }

    @Override
    protected FileSourceContext getDefaultFileSourceContext() {
        final ChangeType fkChangeType = env.getPlatform().getChangeType(ChangeType.FOREIGN_KEY_STR);
        final ChangeType triggerChangeType = env.getPlatform().getChangeType(ChangeType.TRIGGER_INCREMENTAL_OLD_STR);
        DbGetChangeType getChangeType = new DbGetChangeType(fkChangeType, triggerChangeType);
        BaselineTableChangeParser baselineTableChangeParser = new BaselineTableChangeParser(fkChangeType, triggerChangeType);

        return new ReaderContext(env, deployStatsTracker(), getChangeType, baselineTableChangeParser).getDefaultFileSourceContext();
    }

    public static class DbGetChangeType implements GetChangeType {
        private final ChangeType fkChangeType;
        private final ChangeType triggerChangeType;

        public DbGetChangeType(ChangeType fkChangeType, ChangeType triggerChangeType) {
            this.fkChangeType = fkChangeType;
            this.triggerChangeType = triggerChangeType;
        }

        private static final String TOGGLE_FK = "FK";
        private static final String TOGGLE_TRIGGER = "TRIGGER";

        @Override
        public ChangeType getChangeType(TextMarkupDocumentSection section, ChangeType tableChangeType) {
            if (section.isTogglePresent(TOGGLE_FK)) {
                return fkChangeType;
            } else {
                if (section.isTogglePresent(TOGGLE_TRIGGER)) {
                    return triggerChangeType;
                } else {
                    return tableChangeType;
                }
            }
        }
    }

    @Override
    protected ImmutableList<PrepareDbChange> getArtifactTranslators() {
        return Lists.mutable
                .<PrepareDbChange>with(new PrepareDbChangeForDb())
                .withAll(this.env.getDbTranslationDialect().getAdditionalTranslators())
                .toImmutable();
    }

    public ChangeAuditDao getArtifactDeployerDao() {
        return this.singleton("getArtifactDeployerDao", new Function0<ChangeAuditDao>() {
            @Override
            public ChangeAuditDao value() {
                if (env.isDisableAuditTracking()) {
                    return new NoOpChangeAuditDao();
                } else {
                    return new SameSchemaChangeAuditDao(env, getSqlExecutor(), getDbMetadataManager(), credential.getUsername(), getDeployExecutionDao(), getChangeTypeBehaviorRegistry());
                }
            }
        });
    }

    @Override
    public DbMetadataManager getDbMetadataManager() {
        return this.singleton("getDbMetadataManager", new Function0<DbMetadataManager>() {
            @Override
            public DbMetadataManager value() {
                DbMetadataManager dbMetadataManager = env.getPlatform().getDbMetadataManager();
                dbMetadataManager.setDataSource(getManagedDataSource());
                return dbMetadataManager;
            }
        });
    }

    private DbChecksumManager getDbChecksumManager() {
        return this.singleton("getDbChecksumManager", new Function0<DbChecksumManager>() {
            @Override
            public DbChecksumManager value() {
                return new DbChecksumManagerImpl(
                        getDbMetadataManager(),
                        getDbChecksumDao(),
                        env.getPhysicalSchemas()
                );
            }
        });
    }

    @Override
    public DeployExecutionDao getDeployExecutionDao() {
        return this.singleton("deployExecutionDao", new Function0<DeployExecutionDao>() {
            @Override
            public DeployExecutionDao value() {
                return new SameSchemaDeployExecutionDao(
                        getSqlExecutor(),
                        getDbMetadataManager(),
                        platform(),
                        env.getPhysicalSchemas(),
                        getTableSqlSuffix(),
                        env,
                        getChangeTypeBehaviorRegistry()
                );
            }
        });
    }

    @Override
    public DbChecksumDao getDbChecksumDao() {
        return this.singleton("getDbChecksumDao", new Function0<DbChecksumDao>() {
            @Override
            public DbChecksumDao value() {
                return new SameSchemaDbChecksumDao(
                        getSqlExecutor(),
                        getDbMetadataManager(),
                        platform(),
                        env.getPhysicalSchemas(),
                        getTableSqlSuffix(),
                        getChangeTypeBehaviorRegistry()
                );
            }
        });
    }

    private String getTableSqlSuffix() {
        return env.getPlatform().getTableSuffixSql(env);
    }

    @Override
    protected DeployerPlugin getDeployerPlugin() {
        return this.singleton("getDeployerPlugin", new Function0<DeployerPlugin>() {
            @Override
            public DeployerPlugin value() {
                return new DbDeployer(
                        getArtifactDeployerDao()
                        , getDbMetadataManager()
                        , getSqlExecutor()
                        , deployStatsTracker()
                        , getDbChecksumManager()
                        , getDeployExecutionDao()
                );
            }
        });
    }

    private DbEnvironmentCleaner getEnvironmentCleaner() {
        return this.singleton("getEnvironmentCleaner", new Function0<DbEnvironmentCleaner>() {
            @Override
            public DbEnvironmentCleaner value() {
                return new DbEnvironmentCleaner(env, getSqlExecutor(), getDbMetadataManager(),
                        getChangesetCreator(), getChangeTypeBehaviorRegistry());
            }
        });
    }

    protected abstract DataSourceFactory getDataSourceFactory();

    protected EnvironmentInfraSetup getEnvironmentInfraSetup() {
        return new NoOpEnvironmentInfraSetup();
    }

    protected Predicate<? super Change> getDbChangeFilter() {
        ImmutableSet<String> disabledChangeTypeNames = this.env.getDbTranslationDialect().getDisabledChangeTypeNames();
        if (disabledChangeTypeNames.isEmpty()) {
            return Predicates.alwaysTrue();
        }

        Predicate<? super Change> disabledChangeTypePredicate = ChangeKeyPredicateBuilder.newBuilder().setChangeTypes(disabledChangeTypeNames).build();

        return Predicates.not(disabledChangeTypePredicate);  // we return the opposite of what is disabled
    }

    public CsvStaticDataDeployer getCsvStaticDataLoader() {
        return new CsvStaticDataDeployer(env, this.getSqlExecutor(), getManagedDataSource(),
                this.getDbMetadataManager(), this.env.getPlatform());
    }

    /**
     * For backwards-compatibility, we keep this data source as a single connection for clients.
     */
    @Override
    public final DataSource getDataSource() {
        return this.singleton("getDataSource", new Function0<DataSource>() {
            @Override
            public DataSource value() {
                return new SingleConnectionDataSource(getDataSourceFactory().createDataSource(env, credential, 1));
            }
        });
    }

    protected final DataSource getManagedDataSource() {
        return this.singleton("getManagedDataSource", new Function0<DataSource>() {
            @Override
            public DataSource value() {
                return getDataSourceFactory().createDataSource(env, credential, getNumThreads() + 1);   // reserve an extra connection for actions done on the regular DS and not the per-thread DS
            }
        });
    }

    public DbDeployerAppContext setupEnvInfra() {
        return setupEnvInfra(isStrictSetupEnvInfra());
    }

    @Override
    public DbDeployerAppContext setupEnvInfra(boolean strictSetupEnvInfra) {
        getEnvironmentInfraSetup().setupEnvInfra(strictSetupEnvInfra);
        return this;
    }

    @Override
    public DbDeployerAppContextImpl cleanEnvironment() {
        getEnvironmentCleaner().cleanEnvironment(MainDeployerArgs.DEFAULT_NOPROMPT_VALUE_FOR_API);
        return this;
    }

    @Override
    public DbDeployerAppContextImpl cleanAndDeploy() {
        cleanEnvironment();
        deploy();
        return this;
    }

    @Override
    public DbDeployerAppContextImpl setupAndCleanAndDeploy() {
        setupEnvInfra();
        cleanAndDeploy();
        return this;
    }
}
