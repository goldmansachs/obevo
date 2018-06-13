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
package com.gs.obevo.impl

import com.gs.obevo.api.appdata.*
import com.gs.obevo.api.factory.PlatformConfiguration
import com.gs.obevo.api.platform.*
import com.gs.obevo.impl.graph.GraphEnricher
import com.gs.obevo.impl.graph.GraphUtil
import com.gs.obevo.impl.text.TextDependencyExtractor
import com.gs.obevo.util.inputreader.ConsoleInputReader
import com.gs.obevo.util.inputreader.Credential
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.StopWatch
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.block.function.Function
import org.eclipse.collections.api.block.procedure.Procedure
import org.eclipse.collections.api.collection.ImmutableCollection
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.factory.Predicates
import org.eclipse.collections.impl.block.factory.StringFunctions
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Sets
import org.eclipse.collections.impl.set.mutable.SetAdapter
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This class orchestrates the db deployment logic for the given platform and environment.
 */
class MainDeployer<P : Platform, E : Environment<P>>(
        private val artifactDeployerDao: ChangeAuditDao,
        private val mainInputReader: MainInputReader<E>,
        private val changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry,
        private val changesetCreator: ChangesetCreator,
        private val postDeployAction: PostDeployAction<E>,
        private val deployMetricsCollector: DeployMetricsCollector,
        private val deployExecutionDao: DeployExecutionDao,
        private val credential: Credential,
        private val textDependencyExtractor: TextDependencyExtractor,
        private val deployerPlugin: DeployerPlugin<E>,
        private val graphEnricher: GraphEnricher) {

    private val userInputReader = ConsoleInputReader()

    fun execute(env: E, sourceReaderStrategy: SourceReaderStrategy, deployerArgs: MainDeployerArgs) {
        val changeStopWatch = StopWatch()
        changeStopWatch.start()

        var mainDeploymentSuccess = false
        try {
            executeInternal(env, sourceReaderStrategy, deployerArgs)
            mainDeploymentSuccess = true
        } finally {
            changeStopWatch.stop()
            val deployRuntimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.time)
            deployMetricsCollector.addMetric("runtimeSeconds", deployRuntimeSeconds)
            deployMetricsCollector.addMetric("success", mainDeploymentSuccess)
        }
    }

    private fun executeInternal(env: E, sourceReaderStrategy: SourceReaderStrategy, deployerArgs: MainDeployerArgs) {
        LOG.info("Running {} version {}", PlatformConfiguration.getInstance().toolName, PlatformConfiguration.getInstance().toolVersion)

        if (env.schemas.isEmpty) {
            throw IllegalArgumentException("Environment needs schemas populated")
        }

        if (deployerArgs.productVersion != null) {
            val rollbackDetector = DefaultRollbackDetector()

            val rollbackDetectedFromAudit = env.isRollbackDetectionEnabled && rollbackDetector.determineRollback(deployerArgs.productVersion, env.schemaNames, deployExecutionDao)
            if (rollbackDetectedFromAudit) {
                LOG.info("Detected a rollback based on the audit data; will proceed in rollback mode")
                // we only call the setter if we calculate rollback==true so that we don't override an intentional
                // rollback parameter from the client (i.e. in case the audit does not want a rollback). Though this
                // should never be the case
                deployerArgs.isRollback = rollbackDetectedFromAudit
            } else if (deployerArgs.isRollback) {
                LOG.warn("You have requested a rollback in your arguments, though your version metadata does not require it.")
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".rollbackInvokedDespiteAuditData", true)
            }
        }

        deployerPlugin.validateSetup()
        if (deployerArgs.isRollback) {
            LOG.info("*** EXECUTING IN ROLLBACK MODE ***")
        }

        logArgumentMetrics(deployerArgs)
        logEnvironment(env)
        logEnvironmentMetrics(env)
        deployerPlugin.logEnvironmentMetrics(env)

        val changeInputs = mainInputReader.readInternal(sourceReaderStrategy, deployerArgs)

        val onboardingStrategy = getOnboardingStrategy(deployerArgs)
        onboardingStrategy.validateSourceDirs(env.sourceDirs, env.schemaNames)

        val deployStrategy = getDeployMode(deployerArgs)

        val deployedChanges = readDeployedChanges(deployerArgs)
        mainInputReader.logChanges("deployed", deployedChanges)

        // TODO ensure that we've handled the split between static data and others properly
        val changeInputSetMap = this.textDependencyExtractor.calculateDependencies(
                changeInputs.filter { it.changeKey.changeType.isEnrichableForDependenciesInText }
        )

        val sourceChanges = changeInputs.collect { input ->
            val change: Change
            if (input.isRerunnable) {
                change = ChangeRerunnable(input.changeKey, input.contentHash, input.content)
            } else {
                change = ChangeIncremental(
                        input.changeKey,
                        input.orderWithinObject,
                        input.contentHash,
                        input.content,
                        input.rollbackIfAlreadyDeployedContent,
                        input.isActive
                )
                change.isDrop = input.isDrop
                change.baselinedChanges = input.baselinedChanges
                change.parallelGroup = input.parallelGroup
                // TODO not yet complete GITHUB#158 GITHUB#153
//                (change as ChangeIncremental).setManuallyCodedDrop(false)
            }

            change.metadataSection = input.metadataSection

            // TODO should not need separate converted*Content fields in Change. Should only be in ChangeInput
            change.convertedContent = input.convertedContent
            change.setConvertedRollbackContent(input.convertedRollbackContent)

            change.changeInput = input
            change.orderWithinObject = input.orderWithinObject

            change.order = input.order
            change.applyGrants = input.applyGrants
            change.changeset = input.changeset

            // TODO GITHUB#153 improve on this
            change.codeDependencies = SetAdapter.adapt(changeInputSetMap.get(input)
                    ?: input?.codeDependencies?.castToSet() ?: Sets.mutable.empty()).toImmutable()

            change.dropContent = input.dropContent
            change.permissionScheme = input.permissionScheme

            // TODO not yet complete GITHUB#158 GITHUB#153
            change
        }
        val dependencyGraph = graphEnricher.createDependencyGraph(sourceChanges, deployerArgs.isRollback)
        sourceChanges.forEach(Procedure { each ->
            val dependencyNodes = GraphUtil.getDependencyNodes(dependencyGraph, each)
            each.dependentChanges = dependencyNodes
        })


        val artifactsToProcess = changesetCreator.determineChangeset(deployedChanges, sourceChanges, deployerArgs.isRollback, deployStrategy.isInitAllowedOnHashExceptions, deployerArgs.changesetPredicate)

        validatePriorToDeployment(env, deployStrategy, sourceChanges, deployedChanges, artifactsToProcess)
        deployerPlugin.validatePriorToDeployment(env, deployStrategy, sourceChanges, deployedChanges, artifactsToProcess)

        if (this.shouldProceedWithDbChange(artifactsToProcess, deployerArgs)) {
            for (schema in env.physicalSchemas) {
                deployerPlugin.initializeSchema(env, schema)
            }

            // Note - only init the audit table if we actually proceed w/ a deploy
            this.deployExecutionDao.init()
            this.artifactDeployerDao.init()

            val executionsBySchema = env.schemas.associateBy({it.name}, { schema ->
                val deployExecution = DeployExecutionImpl(
                        deployerArgs.deployRequesterId,
                        credential.username,
                        schema.name,
                        PlatformConfiguration.getInstance().toolVersion,
                        Timestamp(Date().time),
                        deployerArgs.isPerformInitOnly,
                        deployerArgs.isRollback,
                        deployerArgs.productVersion,
                        deployerArgs.reason,
                        deployerArgs.deployExecutionAttributes
                )
                deployExecution.status = DeployExecutionStatus.IN_PROGRESS
                deployExecutionDao.persistNew(deployExecution, env.getPhysicalSchema(schema.name))
                deployExecution
            })

            // If there are no deployments required, then just update the artifact tables and return
            if (!artifactsToProcess.isDeploymentNeeded) {
                LOG.info("No changes detected in the database deployment. Updating Deploy Status")
                executionsBySchema.values.forEach { deployExecution ->
                    deployExecution.status = DeployExecutionStatus.SUCCEEDED
                    this.deployExecutionDao.update(deployExecution)
                }
                return
            }

            val action = if (deployerArgs.isRollback) "Rollback" else "Deployment"

            var mainDeploymentSuccess = false
            val cec = CommandExecutionContext()
            try {
                this.doExecute(artifactsToProcess, deployStrategy, onboardingStrategy, executionsBySchema, cec)
                LOG.info("$action has Completed Successfully!")
                executionsBySchema.values.forEach { deployExecution ->
                    deployExecution.status = DeployExecutionStatus.SUCCEEDED
                    this.deployExecutionDao.update(deployExecution)
                }

                mainDeploymentSuccess = true
            } catch (exc: RuntimeException) {
                LOG.info("$action has Failed. We will error out, but first complete the post-deploy step")
                executionsBySchema.values.forEach { deployExecution ->
                    deployExecution.status = DeployExecutionStatus.FAILED
                    this.deployExecutionDao.update(deployExecution)
                }
                throw exc
            } finally {
                LOG.info("Executing the post-deploy step")
                try {
                    deployerPlugin.doPostDeployAction(env, sourceChanges)
                    this.postDeployAction.value(env)
                } catch (exc: RuntimeException) {
                    if (mainDeploymentSuccess) {
                        LOG.info("Exception found in the post-deploy step", exc)
                        throw exc
                    } else {
                        LOG.error("Exception found in the post-deploy step; printing it out here, but there was an exception during the regular deploy as well", exc)
                    }
                }

                LOG.info("Post-deploy step completed")

                val warnings = cec.warnings
                if (warnings.notEmpty()) {
                    LOG.info("")
                    LOG.info("Summary of warnings from this deployment; please address:\n{}", warnings.collect(StringFunctions.prepend("    ")).makeString("\n"))
                }

                LOG.info("Deploy complete!")
            }
        }
    }

    private fun logArgumentMetrics(deployerArgs: MainDeployerArgs) {
        deployMetricsCollector.addMetric("args.onboardingMode", deployerArgs.isOnboardingMode)
        deployMetricsCollector.addMetric("args.init", deployerArgs.isPerformInitOnly)
        deployMetricsCollector.addMetric("args.rollback", deployerArgs.isRollback)
        deployMetricsCollector.addMetric("args.preview", deployerArgs.isPreview)
        deployMetricsCollector.addMetric("args.useBaseline", deployerArgs.isUseBaseline)
    }

    private fun logEnvironment(env: E) {
        if (LOG.isInfoEnabled) {
            LOG.info("Environment information:")
            LOG.info("Logical schemas [{}]: {}", env.schemaNames.size(), env.schemaNames.makeString(","))
            LOG.info("Physical schemas [{}]: {}", env.physicalSchemas.size(), env.physicalSchemas.collect { (physicalName) -> physicalName }.makeString(","))
        }
    }

    private fun logEnvironmentMetrics(env: E) {
        deployMetricsCollector.addMetric("platform", env.platform.name)
        deployMetricsCollector.addMetric("schemaCount", env.schemaNames.size())
        deployMetricsCollector.addMetric("schemas", env.schemaNames.makeString(","))
        deployMetricsCollector.addMetric("physicalSchemaCount", env.physicalSchemas.size())
        deployMetricsCollector.addMetric("physicalSchemas", env.physicalSchemas.collect { (physicalName) -> physicalName }.makeString(","))
    }

    private fun validatePriorToDeployment(env: E, deployStrategy: DeployStrategy, sourceChanges: ImmutableList<Change>, deployedChanges: ImmutableCollection<Change>, artifactsToProcess: Changeset) {
        printArtifactsToProcessForUser(artifactsToProcess, deployStrategy, env, deployedChanges, sourceChanges)
        deployerPlugin.printArtifactsToProcessForUser2(artifactsToProcess, deployStrategy, env, deployedChanges, sourceChanges)

        logChangeset(artifactsToProcess)
        artifactsToProcess.validateForDeployment()
    }

    private fun readDeployedChanges(args: MainDeployerArgs): ImmutableCollection<Change> {

        return artifactDeployerDao.deployedChanges
                .select(Predicates.attributePredicate(Function { it -> it.changeKey }, args.changeInclusionPredicate))
    }

    private fun getDeployMode(deployerArgs: MainDeployerArgs): DeployStrategy {
        return if (deployerArgs.isPerformInitOnly) {
            ForceInitDeployStrategy.INSTANCE
        } else {
            ExecuteDeployStrategy.INSTANCE
        }
    }

    private fun getOnboardingStrategy(deployerArgs: MainDeployerArgs): OnboardingStrategy {
        return if (deployerArgs.isOnboardingMode) EnabledOnboardingStrategy() else DisabledOnboardingStrategy()
    }

    private fun doExecute(artifactsToProcess: Changeset, deployStrategy: DeployStrategy, onboardingStrategy: OnboardingStrategy, executionsBySchema: Map<String, DeployExecution>, cec: CommandExecutionContext) {
        val failedChanges = Lists.mutable.empty<FailedChange>()
        val failedObjectNames = Sets.mutable.empty<String>()  // to handle use case of table failing and prevent subsequent CSV from getting deployed
        val failedChangeKeys = Sets.mutable.empty<ChangeKey>()  // to handle all other cases; should move the CSV case into this one

        for (auditChangeCommand in artifactsToProcess.auditChanges) {
            auditChangeCommand.markAuditTable(changeTypeBehaviorRegistry, this.artifactDeployerDao, executionsBySchema.get(auditChangeCommand.schema))
        }

        for (changeCommand in artifactsToProcess.inserts) {
            val previousFailedObjects = failedObjectNames.intersect(changeCommand.changes.toSet().collect { change2 -> change2.dbObjectKey })
            if (previousFailedObjects.notEmpty()) {
                // We skip subsequent changes in objects that failed as we don't any unexpected activities to happen on
                // a particular DB object
                // (e.g. if one change relied on a previous one, and the previous one failed; what if something goes bad
                // if the first one isn't executed?)
                LOG.info("Skipping artifact [{}] as these objects previously failed deploying: {}",
                        changeCommand.commandDescription, previousFailedObjects.makeString(", "))
                continue
            }
            val dependencyChangeKeys = changeCommand.changes.flatMap { it.dependentChanges ?: Sets.mutable.empty() }.map { it.changeKey }
            val failedCommandKeys = dependencyChangeKeys.intersect(failedChangeKeys)

            if (failedCommandKeys.isNotEmpty()) {
                // We skip subsequent changes in objects that failed as we don't any unexpected activities to happen on
                // a particular DB object
                // (e.g. if one change relied on a previous one, and the previous one failed; what if something goes bad
                // if the first one isn't executed?)
                LOG.info("Skipping artifact [{}] as these changes previously failed deploying: {}",
                        changeCommand.commandDescription, failedCommandKeys.joinToString(", "))
                continue
            }

            LOG.info("Attempting to deploy: " + changeCommand.commandDescription)

            val changeStopWatch = StopWatch()
            changeStopWatch.start()

            try {
                deployStrategy.deploy(changeTypeBehaviorRegistry, changeCommand, cec)
                changeCommand.markAuditTable(changeTypeBehaviorRegistry, this.artifactDeployerDao, executionsBySchema.get(changeCommand.schema))

                changeStopWatch.stop()
                val runtimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.time)
                LOG.info("Successfully " + deployStrategy.deployVerbMessage + " artifact " + changeCommand.commandDescription +
                        ", took " + runtimeSeconds + " seconds")

                for (change in changeCommand.changes) {
                    if (change.changeInput != null) {
                        // TODO GITHUB#153 why check for null?
                        onboardingStrategy.handleSuccess(change.changeInput)
                    }
                }
            } catch (exc: Exception) {
                try {
                    changeStopWatch.stop()
                } catch (e234: Exception) {
                    // TODO GITHUB#153 fix this
                    e234.printStackTrace();
                }
                val runtimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.time)

                for (change in changeCommand.changes) {
                    onboardingStrategy.handleException(change.changeInput, exc)
                }

                LOG.info("Failed to deploy artifact " + changeCommand.commandDescription + ", took "
                        + runtimeSeconds + " seconds")

                LOG.info("We will continue and fail the process at the end. This was the exception: " + ExceptionUtils.getStackTrace(exc))
                failedChanges.add(FailedChange(changeCommand, exc))

                failedObjectNames.withAll(changeCommand.changes.collect { change1 -> change1.dbObjectKey })
                failedChangeKeys.withAll(changeCommand.changes.collect { change -> change.changeKey })
            }

        }

        if (!failedChanges.isEmpty) {
            deployMetricsCollector.addMetric("exceptionCount", failedChanges.size)
            val changeMessages = failedChanges.collect {
                (it.changeCommand.commandDescription + "\n"
                        + "    Root Exception Message: " + ExceptionUtils.getRootCauseMessage(it.exception))
            }

            val exceptionMessage = "Failed deploying the following artifacts.\n" + changeMessages
                    .makeString("\n")
            LOG.error(exceptionMessage)
            LOG.error("")
            for (failedChange in failedChanges) {
                LOG.error("This change failed: " + failedChange.changeCommand.commandDescription)
                LOG.error("From exception: " + ExceptionUtils.getStackTrace(failedChange.exception))
                LOG.error("")
                LOG.error("")
            }
            throw DeployExecutionException(exceptionMessage, failedChanges)
        }
    }

    private fun shouldProceedWithDbChange(artifactsToProcess: Changeset, args: MainDeployerArgs): Boolean {
        if (args.isPreview) {
            LOG.info("We are in PREVIEW mode, so we will not proceed further. Exiting.")
            return false
        } else if (args.isNoPrompt) {
            LOG.info("-noPrompt parameter was passed; hence, we can proceed w/out user prompting")
            return true
        } else if (!artifactsToProcess.isDeploymentNeeded) {
            LOG.info("No artifacts to deploy, hence we can proceed without user prompting.")
            LOG.info("Will only update the Artifact Execution tables ")
            return true
        } else {
            LOG.info("Deploying to a live environment; hence, we need to prompt user confirmation")
            LOG.info("Are you sure you want to proceed? (Y/N)")

            val input = this.userInputReader.readLine(null)
            if (input.trim { it <= ' ' }.equals("Y", ignoreCase = true)) {
                return true
            }
        }

        LOG.info("Did not pass command line validation input. Will not proceed w/ actual deployment")
        return false
    }

    private fun printArtifactsToProcessForUser(artifactsToProcess: Changeset, deployStrategy: DeployStrategy, env: E, deployedChanges: ImmutableCollection<Change>, sourceChanges: ImmutableCollection<Change>) {
        printCommands(artifactsToProcess.inserts, "DB Changes are to be " + deployStrategy.deployVerbMessage)
        printCommands(artifactsToProcess.auditChanges, "auditTable-only changes are to be deployed")
        printCommands(artifactsToProcess.changeWarnings, "warnings/errors are for your information (no changes " + "done)")
        printCommands(artifactsToProcess.deferredChanges, "DB Changes are deferred from deploying here as they have the changeset attribute set and the relevant changeset argument was not provided at input")
    }

    private fun logChangeset(changeset: Changeset) {
        deployMetricsCollector.addMetric("changeset.executeCount", changeset.inserts.size())
        deployMetricsCollector.addMetric("changeset.auditCount", changeset.auditChanges.size())
        deployMetricsCollector.addMetric("changeset.warningCount", changeset.changeWarnings.size())
        deployMetricsCollector.addMetric("changeset.deferredCount", changeset.deferredChanges.size())

        val warningBag = changeset.changeWarnings.collect { warning -> warning.javaClass.name }.toBag()
        warningBag.toMapOfItemToCount().forEachKeyValue { warningClassName, count -> deployMetricsCollector.addMetric("changeset.warningTypeCounts.$warningClassName", count) }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainDeployer::class.java)

        private fun printCommands(commands: RichIterable<out ChangeCommand>, message: String) {
            if (commands.notEmpty()) {
                LOG.info("The following $message:")
                for (changeCommand in commands) {
                    LOG.info("\t" + changeCommand.commandDescription)
                }
                LOG.info("")
            } else {
                LOG.info("Nothing applicable for: [$message]")
                LOG.info("")
            }
        }
    }
}
