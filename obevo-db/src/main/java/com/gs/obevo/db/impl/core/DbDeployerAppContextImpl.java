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

import java.io.File;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.DeployMetrics;
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
import com.gs.obevo.db.impl.core.reader.CachedDbChangeReader;
import com.gs.obevo.db.impl.core.reader.DbChangeReader;
import com.gs.obevo.db.impl.core.reader.DbDirectoryChangesetReader;
import com.gs.obevo.db.impl.core.reader.PrepareDbChange;
import com.gs.obevo.db.impl.core.reader.PrepareDbChangeForDb;
import com.gs.obevo.db.impl.core.reader.SourceChangeReaderImpl;
import com.gs.obevo.db.impl.core.reader.TextMarkupDocumentReader;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangesetCreator;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.impl.MainDeployer;
import com.gs.obevo.impl.NoOpPostDeployAction;
import com.gs.obevo.impl.PostDeployAction;
import com.gs.obevo.impl.SourceChangeReader;
import com.gs.obevo.impl.changecalc.ChangesetCreatorImpl;
import com.gs.obevo.impl.changepredicate.ChangeKeyPredicateBuilder;
import com.gs.obevo.impl.changesorter.ChangeCommandSorter;
import com.gs.obevo.impl.changesorter.ChangeCommandSorterImpl;
import com.gs.obevo.impl.graph.GraphEnricher;
import com.gs.obevo.impl.graph.GraphEnricherImpl;
import com.gs.obevo.impl.text.TextDependencyExtractor;
import com.gs.obevo.impl.text.TextDependencyExtractorImpl;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;

public abstract class DbDeployerAppContextImpl implements DbDeployerAppContext {
    protected Credential credential;
    private File workDir;
    protected DbEnvironment env;
    private boolean strictSetupEnvInfra = DbDeployerAppContext.STRICT_SETUP_ENV_INFRA_DEFAULT;
    protected ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    /**
     * Renamed.
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
    public DbEnvironment getEnvironment() {
        return this.env;
    }

    @Override
    public DbDeployerAppContextImpl setEnvironment(DbEnvironment env) {
        this.env = env;
        return this;
    }

    public Credential getCredential() {
        return this.credential;
    }

    @Override
    public DbDeployerAppContextImpl setCredential(Credential credential) {
        this.credential = credential;
        return this;
    }

    @Override
    public File getWorkDir() {
        return this.workDir;
    }

    @Override
    public DbDeployerAppContextImpl setWorkDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    @Override
    public final DbDeployerAppContextImpl build() {
        buildDbContext();
        buildFileContext();

        // then everything else, which is already setup by default

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

        this.changeTypeBehaviorRegistry = new ChangeTypeBehaviorRegistry(getChangeTypeBehaviors());

        validateChangeTypes(platform().getChangeTypes(), changeTypeBehaviorRegistry);

        getEnvironmentCleaner();
        getManagedDataSource();

        return this;
    }

    protected void validateChangeTypes(ImmutableList<ChangeType> changeTypes, final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
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

    protected MutableMap<String, ChangeTypeBehavior> getChangeTypeBehaviors() {
        MutableMap<String, ChangeTypeBehavior> behaviors = Maps.mutable.<String, ChangeTypeBehavior>empty();

        PartitionImmutableList<ChangeType> staticDataPartition = platform().getChangeTypes().partition(Predicates.attributeEqual(ChangeType.TO_NAME, ChangeType.STATICDATA_STR));

        for (ChangeType staticDataType : staticDataPartition.getSelected()) {
            StaticDataChangeTypeBehavior behavior = new StaticDataChangeTypeBehavior(getSqlExecutor(), simpleArtifactDeployer(), getCsvStaticDataLoader(), graphEnricher());
            behaviors.put(staticDataType.getName(), behavior);
        }

        PartitionImmutableList<ChangeType> rerunnablePartition = staticDataPartition.getRejected().partition(ChangeType.IS_RERUNNABLE);

        for (ChangeType rerunnableChange : rerunnablePartition.getSelected()) {
            if (!(rerunnableChange instanceof DbChangeType)) {
                throw new IllegalArgumentException("Bad change type " + rerunnableChange.getName() + ":" + rerunnableChange);
            }
            RerunnableDbChangeTypeBehavior behavior = new RerunnableDbChangeTypeBehavior(
                    env, (DbChangeType) rerunnableChange, getSqlExecutor(), simpleArtifactDeployer(), grantChangeParser(), graphEnricher(), platform(), getDbMetadataManager());
            behaviors.put(rerunnableChange.getName(), behavior);
        }
        for (ChangeType incrementalChange : rerunnablePartition.getRejected()) {
            IncrementalDbChangeTypeBehavior behavior =  new IncrementalDbChangeTypeBehavior(
                    env, (DbChangeType) incrementalChange, getSqlExecutor(), simpleArtifactDeployer(), grantChangeParser(), deployStatsTracker(), getNumThreads()
            );
            behaviors.put(incrementalChange.getName(), behavior);
        }

        return behaviors;
    }

    protected GrantChangeParser grantChangeParser() {
        return new GrantChangeParser(env, getArtifactTranslators());
    }

    @Override
    public DbDeployerAppContext buildFileContext() {
        Validate.notNull(env.getSourceDirs(), "sourceDirs must be populated");
        Validate.notNull(env.getName(), "name must be populated");
        Validate.notNull(env.getSchemas(), "schemas must be populated");

        getDbChangeReader();
        getChangesetCreator();

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
        return getDbChangeReader().readChanges(useBaseline);
    }

    public DbChangeReader getDbChangeReader() {
        return this.singleton("getDbChangeReader", new Function0<DbChangeReader>() {
            @Override
            public DbChangeReader value() {
                DbDirectoryChangesetReader underlyingChangesetReader = new DbDirectoryChangesetReader(getEnvironment().getPlatform().convertDbObjectName(), getEnvironment(), deployStatsTracker(), true, textMarkupDocumentReader());
                return new CachedDbChangeReader(underlyingChangesetReader);
            }
        });
    }

    private TextMarkupDocumentReader textMarkupDocumentReader() {
        return this.singleton("textMarkupDocumentReader", new Function0<TextMarkupDocumentReader>() {
            @Override
            public TextMarkupDocumentReader value() {
                int metadataLineReaderVersion = env.getMetadataLineReaderVersion();
                return new TextMarkupDocumentReader(metadataLineReaderVersion < 3);  // legacy mode is 2 and below
            }
        });
    }

    public DeployMetricsCollector deployStatsTracker() {
        return this.singleton("deployStatsTracker", new Function0<DeployMetricsCollector>() {
            @Override
            public DeployMetricsCollector value() {
                return new DeployMetricsCollectorImpl();
            }
        });
    }

    public ChangesetCreator getChangesetCreator() {
        return this.singleton("getChangesetCreator", new Function0<ChangesetCreator>() {
            @Override
            public ChangesetCreator value() {
                return new ChangesetCreatorImpl(changeCommandSorter(), changeTypeBehaviorRegistry);
            }
        });
    }

    private ChangeCommandSorter changeCommandSorter() {
        return new ChangeCommandSorterImpl(env.getPlatform());
    }

    protected GraphEnricher graphEnricher() {
        return new GraphEnricherImpl(env.getPlatform().convertDbObjectName());
    }

    private TextDependencyExtractor getTextDependencyExtractor() {
        return new TextDependencyExtractorImpl(env.getPlatform().convertDbObjectName());
    }

    public ChangeAuditDao getArtifactDeployerDao() {
        return this.singleton("getArtifactDeployerDao", new Function0<ChangeAuditDao>() {
            @Override
            public ChangeAuditDao value() {
                if (env.isDisableAuditTracking()) {
                    return new NoOpChangeAuditDao();
                } else {
                    return new SameSchemaChangeAuditDao(env, getSqlExecutor(), getDbMetadataManager(), credential.getUsername(), getDeployExecutionDao(), changeTypeBehaviorRegistry);
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
                        changeTypeBehaviorRegistry
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
                        changeTypeBehaviorRegistry
                );
            }
        });
    }

    private String getTableSqlSuffix() {
        return env.getPlatform().getTableSuffixSql(env);
    }

    public MainDeployer getDeployer() {
        return this.singleton("deployer", new Function0<MainDeployer>() {
            @Override
            public MainDeployer value() {
                return new DbDeployer(
                        getArtifactDeployerDao()
                        , getInputReader()
                        , changeTypeBehaviorRegistry
                        , getChangesetCreator()
                        , getPostDeployAction()
                        , getDbMetadataManager()
                        , getSqlExecutor()
                        , deployStatsTracker()
                        , getDbChecksumManager()
                        , getDeployExecutionDao()
                        , getCredential()
                );
            }
        });
    }

    public final DbInputReader getInputReader() {
        return this.singleton("getInputReader", new Function0<DbInputReader>() {
            @Override
            public DbInputReader value() {
                return new DbInputReader(getSourceChangeReader(), getDbChangeFilter(), deployStatsTracker());
            }
        });
    }

    public final SourceChangeReader getSourceChangeReader() {
        return this.singleton("getSourceChangeReader", new Function0<SourceChangeReader>() {
            @Override
            public SourceChangeReader value() {
                return new SourceChangeReaderImpl(env, getDbChangeReader(), getTextDependencyExtractor(), getArtifactTranslators());
            }
        });
    }

    public final DbEnvironmentCleaner getEnvironmentCleaner() {
        return this.singleton("getEnvironmentCleaner", new Function0<DbEnvironmentCleaner>() {
            @Override
            public DbEnvironmentCleaner value() {
                return new DbEnvironmentCleaner(env, getSqlExecutor(), getDbMetadataManager(),
                        getChangesetCreator(), changeTypeBehaviorRegistry);
            }
        });
    }

    protected abstract DataSourceFactory getDataSourceFactory();

    public EnvironmentInfraSetup getEnvironmentInfraSetup() {
        return new NoOpEnvironmentInfraSetup();
    }

    public Predicate<? super Change> getDbChangeFilter() {
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

    public PostDeployAction getPostDeployAction() {
        return new NoOpPostDeployAction();
    }

    protected ImmutableList<PrepareDbChange> getArtifactTranslators() {
        return Lists.mutable
                .<PrepareDbChange>with(new PrepareDbChangeForDb())
                .withAll(this.env.getDbTranslationDialect().getAdditionalTranslators())
                .toImmutable();
    }

    private final MutableMap<String, Object> singletonBeans = Maps.mutable.empty();

    public <T> T singleton(String beanName, Function0<T> func) {
        Object bean = this.singletonBeans.get(beanName);
        if (bean == null) {
            bean = func.value();
            this.singletonBeans.put(beanName, bean);
        }
        return (T) bean;
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

    private int getNumThreads() {
        return 5;
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
    public DbDeployerAppContextImpl deploy() {
        return this.deploy(new MainDeployerArgs());
    }

    @Override
    public DbDeployerAppContextImpl deploy(MainDeployerArgs deployerArgs) {
        getDeployer().execute(env, deployerArgs);
        return this;
    }

    @Override
    public void readSource(MainDeployerArgs deployerArgs) {
        getInputReader().read(env, deployerArgs);
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

    @Override
    public DeployMetrics getDeployMetrics() {
        return deployStatsTracker().getMetrics();
    }
}
