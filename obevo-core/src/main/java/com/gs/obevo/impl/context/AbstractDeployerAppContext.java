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
package com.gs.obevo.impl.context;

import java.io.File;
import java.util.Collection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeSemantic;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.FileSourceContext;
import com.gs.obevo.api.platform.FileSourceParams;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry.ChangeTypeBehaviorRegistryBuilder;
import com.gs.obevo.impl.ChangesetCreator;
import com.gs.obevo.impl.DefaultDeployerPlugin;
import com.gs.obevo.impl.DeployMetricsCollector;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.impl.DeployerPlugin;
import com.gs.obevo.impl.FileSourceReaderStrategy;
import com.gs.obevo.impl.InputSourceReaderStrategy;
import com.gs.obevo.impl.MainDeployer;
import com.gs.obevo.impl.MainInputReader;
import com.gs.obevo.impl.NoOpPostDeployAction;
import com.gs.obevo.impl.PostDeployAction;
import com.gs.obevo.impl.PrepareDbChange;
import com.gs.obevo.impl.changecalc.ChangesetCreatorImpl;
import com.gs.obevo.impl.changesorter.ChangeCommandSorter;
import com.gs.obevo.impl.changesorter.ChangeCommandSorterImpl;
import com.gs.obevo.impl.changetypes.GroupChangeTypeSemantic;
import com.gs.obevo.impl.changetypes.IncrementalChangeTypeSemantic;
import com.gs.obevo.impl.changetypes.RerunnableChangeTypeSemantic;
import com.gs.obevo.impl.graph.GraphEnricher;
import com.gs.obevo.impl.graph.GraphEnricherImpl;
import com.gs.obevo.impl.reader.CachedDbChangeReader;
import com.gs.obevo.impl.reader.DbChangeFileParser;
import com.gs.obevo.impl.reader.DbDirectoryChangesetReader;
import com.gs.obevo.impl.reader.TableChangeParser.GetChangeType;
import com.gs.obevo.impl.reader.TextMarkupDocumentReader;
import com.gs.obevo.impl.text.TextDependencyExtractor;
import com.gs.obevo.impl.text.TextDependencyExtractorImpl;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;

/**
 * Base implementation class for a DeployerAppContext to facilitate creating other new {@link Platform} implementations.
 */
public abstract class AbstractDeployerAppContext<E extends Environment, Self extends DeployerAppContext<E, Self>> implements DeployerAppContext<E, Self> {
    private final MutableMap<String, Object> singletonBeans = Maps.mutable.empty();

    protected Credential credential;
    private File workDir;
    protected E env;
    private FileSourceContext fileSourceContext;

    @Override
    public final Self build() {
        buildDbContext();
        buildFileContext();

        // then everything else, which is already setup by default

        return (Self) this;
    }

    @Override
    public E getEnvironment() {
        return this.env;
    }

    @Override
    public Self setEnvironment(E env) {
        this.env = env;
        return (Self) this;
    }

    private Credential getCredential() {
        return this.credential;
    }

    @Override
    public Self setCredential(Credential credential) {
        this.credential = credential;
        return (Self) this;
    }

    @Override
    public File getWorkDir() {
        return this.workDir;
    }

    @Override
    public Self setWorkDir(File workDir) {
        this.workDir = workDir;
        return (Self) this;
    }

    @Override
    public Self setFileSourceContext(FileSourceContext fileSourceContext) {
        this.fileSourceContext = fileSourceContext;
        return (Self) this;
    }

    @Override
    public Self deploy() {
        return this.deploy(new MainDeployerArgs());
    }

    @Override
    public Self deploy(MainDeployerArgs deployerArgs) {
        FileSourceParams params = getFileSourceParams(deployerArgs.isUseBaseline());
        getDeployer().execute(env, getSourceReaderStrategy(params), deployerArgs);
        return (Self) this;
    }

    protected FileSourceParams getFileSourceParams(boolean baseline) {
        return FileSourceParams.newBuilder()
                    .setSchemaNames(env.getSchemaNames())
                    .setFiles(env.getSourceDirs())
                    .setBaseline(baseline)
                    .setAcceptedExtensions(env.getAcceptedExtensions())
                    .setChangeTypes(env.getPlatform().getChangeTypes())
                    .setDefaultSourceEncoding(env.getSourceEncoding())
                    .setLegacyDirectoryStructureEnabled(env.isLegacyDirectoryStructureEnabled())
                    .build();
    }

    protected FileSourceReaderStrategy getSourceReaderStrategy(FileSourceParams params) {
        FileSourceContext source = fileSourceContext != null ? fileSourceContext : getDefaultFileSourceContext();
        return new FileSourceReaderStrategy(source, params);
    }

    @Override
    public Self deploy(Collection<Change> changes) {
        return this.deploy(changes, new MainDeployerArgs());
    }

    @Override
    public Self deploy(Collection<Change> changes, MainDeployerArgs deployerArgs) {
        getDeployer().execute(env, new InputSourceReaderStrategy(CollectionAdapter.wrapList(changes).toImmutable()), deployerArgs);
        return (Self) this;
    }

    protected <T> T singleton(String beanName, Function0<T> func) {
        Object bean = this.singletonBeans.get(beanName);
        if (bean == null) {
            bean = func.value();
            this.singletonBeans.put(beanName, bean);
        }
        return (T) bean;
    }

    protected PostDeployAction getPostDeployAction() {
        return new NoOpPostDeployAction();
    }

    private ChangeCommandSorter changeCommandSorter() {
        return new ChangeCommandSorterImpl(env.getPlatform(), getDefinitionFromEnvironmentFunction());
    }

    private Function<Change, String> getDefinitionFromEnvironmentFunction() {
        return new Function<Change, String>() {
            @Override
            public String valueOf(Change object) {
                return null;
            }
        };
    }

    protected ChangesetCreator getChangesetCreator() {
        return this.singleton("getChangesetCreator", new Function0<ChangesetCreator>() {
            @Override
            public ChangesetCreator value() {
                return new ChangesetCreatorImpl(changeCommandSorter(), getChangeTypeBehaviorRegistry());
            }
        });
    }

    protected ChangeTypeBehaviorRegistry getChangeTypeBehaviorRegistry() {
        return this.singleton("getDbChangeReader", new Function0<ChangeTypeBehaviorRegistry>() {
            @Override
            public ChangeTypeBehaviorRegistry value() {
                return getChangeTypeBehaviors().build();
            }
        });
    }

    protected DeployMetricsCollector deployStatsTracker() {
        return this.singleton("deployStatsTracker", new Function0<DeployMetricsCollector>() {
            @Override
            public DeployMetricsCollector value() {
                return new DeployMetricsCollectorImpl();
            }
        });
    }

    @Override
    public DeployMetrics getDeployMetrics() {
        return deployStatsTracker().getMetrics();
    }

    protected abstract ChangeTypeBehaviorRegistryBuilder getChangeTypeBehaviors();

    private MainDeployer getDeployer() {
        return this.singleton("deployer", new Function0<MainDeployer>() {
            @Override
            public MainDeployer value() {
                return new MainDeployer(
                        getArtifactDeployerDao()
                        , getInputReader()
                        , getChangeTypeBehaviorRegistry()
                        , getChangesetCreator()
                        , getPostDeployAction()
                        , deployStatsTracker()
                        , getDeployExecutionDao()
                        , getCredential()
                        , getTextDependencyExtractor()
                        , getDeployerPlugin()
                );
            }
        });
    }

    private TextDependencyExtractor getTextDependencyExtractor() {
        return new TextDependencyExtractorImpl(env.getPlatform().convertDbObjectName());
    }

    protected abstract FileSourceContext getDefaultFileSourceContext();

    protected DeployerPlugin getDeployerPlugin() {
        return new DefaultDeployerPlugin();
    }

    protected abstract ChangeAuditDao getArtifactDeployerDao();

    protected abstract DeployExecutionDao getDeployExecutionDao();

    protected Predicate<? super Change> getDbChangeFilter() {
        return Predicates.alwaysTrue();
    }

    protected ChangeTypeSemantic rerunnableSemantic() {
        return new RerunnableChangeTypeSemantic(graphEnricher());
    }

    protected ChangeTypeSemantic incrementalSemantic() {
        return new IncrementalChangeTypeSemantic(getNumThreads());
    }

    protected ChangeTypeSemantic groupSemantic() {
        return new GroupChangeTypeSemantic(graphEnricher());
    }

    protected int getNumThreads() {
        return 5;
    }

    protected GraphEnricher graphEnricher() {
        return new GraphEnricherImpl(env.getPlatform().convertDbObjectName());
    }

    public static class ReaderContext {
        private final Environment env;
        private final DeployMetricsCollector deployStatsTracker;
        private final GetChangeType getChangeType;
        private final DbChangeFileParser baselineTableChangeParser;

        public ReaderContext(Environment env, DeployMetricsCollector deployStatsTracker, GetChangeType getChangeType, DbChangeFileParser baselineTableChangeParser) {
            this.env = env;
            this.deployStatsTracker = deployStatsTracker;
            this.getChangeType = getChangeType;
            this.baselineTableChangeParser = baselineTableChangeParser;
        }

        public FileSourceContext getDefaultFileSourceContext() {
            int metadataLineReaderVersion = env.getMetadataLineReaderVersion();
            TextMarkupDocumentReader textMarkupDocumentReader = new TextMarkupDocumentReader(metadataLineReaderVersion < 3);  // legacy mode is 2 and below

            FileSourceContext underlyingChangesetReader = new DbDirectoryChangesetReader(env.getPlatform().convertDbObjectName(), env, deployStatsTracker, true, textMarkupDocumentReader, baselineTableChangeParser, getChangeType);

            return new CachedDbChangeReader(underlyingChangesetReader);
        }
    }

    protected ImmutableList<PrepareDbChange> getArtifactTranslators() {
        return Lists.immutable.empty();
    }

    protected final MainInputReader getInputReader() {
        return new MainInputReader(env, getDbChangeFilter(), getArtifactTranslators(), deployStatsTracker());
    }
}
