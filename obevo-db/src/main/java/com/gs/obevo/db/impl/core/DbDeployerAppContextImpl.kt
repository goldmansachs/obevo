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
package com.gs.obevo.db.impl.core

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.ChangeInput
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection
import com.gs.obevo.api.platform.*
import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.platform.DbChangeType
import com.gs.obevo.db.api.platform.DbDeployerAppContext
import com.gs.obevo.db.api.platform.DbPlatform
import com.gs.obevo.db.impl.core.changeauditdao.NoOpChangeAuditDao
import com.gs.obevo.db.impl.core.changeauditdao.SameSchemaChangeAuditDao
import com.gs.obevo.db.impl.core.changeauditdao.SameSchemaDeployExecutionDao
import com.gs.obevo.db.impl.core.changetypes.*
import com.gs.obevo.db.impl.core.checksum.DbChecksumDao
import com.gs.obevo.db.impl.core.checksum.DbChecksumManager
import com.gs.obevo.db.impl.core.checksum.DbChecksumManagerImpl
import com.gs.obevo.db.impl.core.checksum.SameSchemaDbChecksumDao
import com.gs.obevo.db.impl.core.cleaner.DbEnvironmentCleaner
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup
import com.gs.obevo.db.impl.core.envinfrasetup.NoOpEnvironmentInfraSetup
import com.gs.obevo.db.impl.core.jdbc.DataSourceFactory
import com.gs.obevo.db.impl.core.jdbc.SingleConnectionDataSource
import com.gs.obevo.db.impl.core.reader.BaselineTableChangeParser
import com.gs.obevo.db.impl.core.reader.PrepareDbChangeForDb
import com.gs.obevo.dbmetadata.api.DbMetadataManager
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry.ChangeTypeBehaviorRegistryBuilder
import com.gs.obevo.impl.DeployerPlugin
import com.gs.obevo.impl.PrepareDbChange
import com.gs.obevo.impl.changepredicate.ChangeKeyPredicateBuilder
import com.gs.obevo.impl.context.AbstractDeployerAppContext
import com.gs.obevo.impl.reader.GetChangeType
import com.gs.obevo.util.CollectionUtil
import com.gs.obevo.util.inputreader.Credential
import org.apache.commons.lang3.Validate
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.factory.Predicates
import org.eclipse.collections.impl.factory.Lists
import javax.sql.DataSource

abstract class DbDeployerAppContextImpl : AbstractDeployerAppContext<DbEnvironment, DbDeployerAppContext>(), DbDeployerAppContext {
    private var strictSetupEnvInfra = DbDeployerAppContext.STRICT_SETUP_ENV_INFRA_DEFAULT

    /**
     * Renamed.
     *
     */
    val isFailOnSetupException: Boolean
        @Deprecated("Renamed to {@link #isStrictSetupEnvInfra()}")
        get() = isStrictSetupEnvInfra()

    private val dbChecksumManager: DbChecksumManager
        get() = this.singleton("getDbChecksumManager") {
            DbChecksumManagerImpl(
                    dbMetadataManager,
                    dbChecksumDao,
                    env.physicalSchemas
            )
        }

    private val tableSqlSuffix: String
        get() = platform().getTableSuffixSql(env)

    private val environmentCleaner: DbEnvironmentCleaner
        get() = this.singleton("getEnvironmentCleaner") {
            DbEnvironmentCleaner(env, sqlExecutor, dbMetadataManager,
                    changesetCreator, changeTypeBehaviorRegistry)
        }

    protected abstract val dataSourceFactory: DataSourceFactory

    protected open val environmentInfraSetup: EnvironmentInfraSetup<*>
        get() = NoOpEnvironmentInfraSetup()

    open val csvStaticDataLoader: CsvStaticDataDeployer
        get() = CsvStaticDataDeployer(env, this.sqlExecutor, managedDataSource,
                this.dbMetadataManager, this.platform())

    protected// reserve an extra connection for actions done on the regular DS and not the per-thread DS
    val managedDataSource: DataSource
        get() = this.singleton("getManagedDataSource") { dataSourceFactory.createDataSource(env, credential, numThreads + 1) }

    /**
     * Whether or not the context will force the environment creation by default. Will be false for most environment types,
     * but we will enable it for the unit test DBs.
     */
    protected open val isForceEnvCreation: Boolean
        get() = false

    private fun isStrictSetupEnvInfra(): Boolean {
        return strictSetupEnvInfra
    }

    override fun setFailOnSetupException(failOnSetupException: Boolean): DbDeployerAppContext {
        return setStrictSetupEnvInfra(failOnSetupException)
    }

    override fun setStrictSetupEnvInfra(strictSetupEnvInfra: Boolean): DbDeployerAppContext {
        this.strictSetupEnvInfra = strictSetupEnvInfra
        return this
    }

    override fun buildDbContext(): DbDeployerAppContext {
        Validate.notNull(platform(), "dbType must be populated")

        if (credential == null) {
            if (this.env.defaultUserId != null && this.env.defaultPassword != null) {
                credential = Credential(this.env.defaultUserId, this.env.defaultPassword)
            } else {
                throw IllegalArgumentException("Cannot build DB context without credential; was not passed in as argument, and no defaultUserId & defaultPassword specified in the environment")
            }
        }

        validateChangeTypes(platform().changeTypes, changeTypeBehaviorRegistry)

        // ensure that these values are initialized
        changesetCreator
        environmentCleaner
        managedDataSource

        return this
    }

    private fun validateChangeTypes(changeTypes: ImmutableList<ChangeType>, changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry) {
        val unenrichedChangeTypes = changeTypes.reject { changeType -> changeTypeBehaviorRegistry.getChangeTypeBehavior(changeType.name) != null }
        if (unenrichedChangeTypes.notEmpty()) {
            throw IllegalStateException("The following change types were not enriched: $unenrichedChangeTypes")
        }

        CollectionUtil.verifyNoDuplicates(changeTypes, { changeType -> changeType.name }, "Not expecting multiple ChangeTypes with the same name")
    }

    protected fun platform(): DbPlatform {
        return env.platform
    }

    protected fun simpleArtifactDeployer(): DbSimpleArtifactDeployer {
        return DbSimpleArtifactDeployer(platform(), sqlExecutor)
    }

    override fun getChangeTypeBehaviors(): ChangeTypeBehaviorRegistryBuilder {
        val builder = ChangeTypeBehaviorRegistry.newBuilder()

        val staticDataPartition = platform().changeTypes.partition { it.name == ChangeType.STATICDATA_STR }

        for (staticDataType in staticDataPartition.selected) {
            val behavior = StaticDataChangeTypeBehavior(env, sqlExecutor, simpleArtifactDeployer(), csvStaticDataLoader)
            builder.put(staticDataType.name, groupSemantic(), behavior)
        }

        val rerunnablePartition = staticDataPartition.rejected.partition { changeType -> changeType.isRerunnable }

        for (rerunnableChange in rerunnablePartition.selected) {
            if (rerunnableChange !is DbChangeType) {
                throw IllegalArgumentException("Bad change type " + rerunnableChange.name + ":" + rerunnableChange)
            }
            val behavior = RerunnableDbChangeTypeBehavior(
                    env, rerunnableChange, sqlExecutor, simpleArtifactDeployer(), grantChangeParser(), graphEnricher(), platform(), dbMetadataManager)
            builder.put(rerunnableChange.getName(), rerunnableSemantic(), behavior)
        }
        for (incrementalChange in rerunnablePartition.rejected) {
            val behavior = IncrementalDbChangeTypeBehavior(
                    env, incrementalChange as DbChangeType, sqlExecutor, simpleArtifactDeployer(), grantChangeParser()
            )
            builder.put(incrementalChange.getName(), incrementalSemantic(), behavior)
        }

        return builder
    }

    protected fun grantChangeParser(): GrantChangeParser {
        return GrantChangeParser(env, artifactTranslators)
    }

    override fun buildFileContext(): DbDeployerAppContext {
        Validate.notNull(env.sourceDirs, "sourceDirs must be populated")
        Validate.notNull(env.name, "name must be populated")
        Validate.notNull(env.schemas, "schemas must be populated")

        // initialize this variable here
        inputReader

        return this
    }

    override fun readChangesFromAudit(): ImmutableList<Change> {
        return artifactDeployerDao.deployedChanges
    }

    override fun readChangesFromSource(): ImmutableList<ChangeInput> {
        return readChangesFromSource(false)
    }

    override fun readChangesFromSource(useBaseline: Boolean): ImmutableList<ChangeInput> {
        return inputReader.readInternal(getSourceReaderStrategy(getFileSourceParams(useBaseline)), MainDeployerArgs().useBaseline(useBaseline)) as ImmutableList<ChangeInput>
    }

    override fun readSource(deployerArgs: MainDeployerArgs) {
        inputReader.readInternal(getSourceReaderStrategy(getFileSourceParams(false)), MainDeployerArgs().useBaseline(false))
    }

    override fun getDefaultFileSourceContext(): FileSourceContext {
        val fkChangeType = platform().getChangeType(ChangeType.FOREIGN_KEY_STR)
        val triggerChangeType = platform().getChangeType(ChangeType.TRIGGER_INCREMENTAL_OLD_STR)
        val getChangeType = DbGetChangeType(fkChangeType, triggerChangeType)
        val baselineTableChangeParser = BaselineTableChangeParser(fkChangeType, triggerChangeType)

        return AbstractDeployerAppContext.ReaderContext(env, deployStatsTracker(), getChangeType, baselineTableChangeParser).defaultFileSourceContext
    }

    class DbGetChangeType(private val fkChangeType: ChangeType, private val triggerChangeType: ChangeType) : GetChangeType {

        override fun getChangeType(section: TextMarkupDocumentSection, tableChangeType: ChangeType): ChangeType {
            return if (section.isTogglePresent(TOGGLE_FK)) {
                fkChangeType
            } else {
                if (section.isTogglePresent(TOGGLE_TRIGGER)) {
                    triggerChangeType
                } else {
                    tableChangeType
                }
            }
        }

        companion object {

            private val TOGGLE_FK = "FK"
            private val TOGGLE_TRIGGER = "TRIGGER"
        }
    }

    override fun getArtifactTranslators(): ImmutableList<PrepareDbChange<in DbEnvironment>> {
        return this.singleton("getArtifactTranslators") {
            Lists.mutable
                    .with<PrepareDbChange<in DbEnvironment>>(PrepareDbChangeForDb())
                    .withAll(this.env.dbTranslationDialect.additionalTranslators)
                    .toImmutable()
        }
    }

    public override fun getArtifactDeployerDao(): ChangeAuditDao {
        return this.singleton("getArtifactDeployerDao") {
            if (env.isDisableAuditTracking) {
                NoOpChangeAuditDao()
            } else {
                SameSchemaChangeAuditDao(env, sqlExecutor, dbMetadataManager, credential.username, deployExecutionDao, changeTypeBehaviorRegistry)
            }
        }
    }

    override fun getDbMetadataManager(): DbMetadataManager {
        return this.singleton("getDbMetadataManager") {
            val dbMetadataManager = platform().dbMetadataManager
            dbMetadataManager.setDataSource(managedDataSource)
            dbMetadataManager
        }
    }

    override fun getDeployExecutionDao(): DeployExecutionDao {
        return this.singleton("deployExecutionDao") {
            SameSchemaDeployExecutionDao(
                    sqlExecutor,
                    dbMetadataManager,
                    platform(),
                    env.physicalSchemas,
                    tableSqlSuffix,
                    env,
                    changeTypeBehaviorRegistry
            )
        }
    }

    override fun getDbChecksumDao(): DbChecksumDao {
        return this.singleton("getDbChecksumDao") {
            SameSchemaDbChecksumDao(
                    sqlExecutor,
                    dbMetadataManager,
                    platform(),
                    env.physicalSchemas,
                    tableSqlSuffix,
                    changeTypeBehaviorRegistry
            )
        }
    }

    override fun getDeployerPlugin(): DeployerPlugin<*> {
        return this.singleton("getDeployerPlugin") {
            DbDeployer(
                    artifactDeployerDao, dbMetadataManager, sqlExecutor, deployStatsTracker(), dbChecksumManager, deployExecutionDao
            )
        }
    }

    override fun getDbChangeFilter(): Predicate<in ChangeKey> {
        val disabledChangeTypeNames = this.env.dbTranslationDialect.disabledChangeTypeNames
        if (disabledChangeTypeNames.isEmpty) {
            return Predicates.alwaysTrue()
        }

        val disabledChangeTypePredicate = ChangeKeyPredicateBuilder.newBuilder().setChangeTypes(disabledChangeTypeNames).build()

        return Predicates.not(disabledChangeTypePredicate)  // we return the opposite of what is disabled
    }

    /**
     * For backwards-compatibility, we keep this data source as a single connection for clients.
     */
    override fun getDataSource(): DataSource {
        return this.singleton("getDataSource") { SingleConnectionDataSource(dataSourceFactory.createDataSource(env, credential, 1)) }
    }

    override fun setupEnvInfra(): DbDeployerAppContext {
        return setupEnvInfra(isStrictSetupEnvInfra())
    }

    override fun setupEnvInfra(strictSetupEnvInfra: Boolean): DbDeployerAppContext {
        return setupEnvInfra(strictSetupEnvInfra, null)
    }

    override fun setupEnvInfra(strictSetupEnvInfra: Boolean, forceEnvCreation: Boolean?): DbDeployerAppContext {
        val willForceCreation = isWillForceCreation(forceEnvCreation)
        environmentInfraSetup.setupEnvInfra(strictSetupEnvInfra, willForceCreation)
        return this
    }

    private fun isWillForceCreation(forceEnvCreation: Boolean?): Boolean {
        return forceEnvCreation ?: if (env.forceEnvInfraSetup != null) {
            env.forceEnvInfraSetup!!
        } else {
            isForceEnvCreation
        }
    }

    override fun cleanEnvironment(): DbDeployerAppContextImpl {
        environmentCleaner.cleanEnvironment(MainDeployerArgs.DEFAULT_NOPROMPT_VALUE_FOR_API)
        return this
    }

    override fun cleanAndDeploy(): DbDeployerAppContextImpl {
        cleanEnvironment()
        deploy()
        return this
    }

    override fun setupAndCleanAndDeploy(): DbDeployerAppContextImpl {
        setupEnvInfra()
        cleanAndDeploy()
        return this
    }
}
