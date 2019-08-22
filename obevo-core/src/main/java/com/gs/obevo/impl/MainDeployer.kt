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

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.ChangeIncremental
import com.gs.obevo.api.appdata.ChangeInput
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.ChangeRerunnable
import com.gs.obevo.api.appdata.CodeDependency
import com.gs.obevo.api.appdata.DeployExecution
import com.gs.obevo.api.appdata.DeployExecutionImpl
import com.gs.obevo.api.appdata.DeployExecutionStatus
import com.gs.obevo.api.appdata.Environment
import com.gs.obevo.api.factory.PlatformConfiguration
import com.gs.obevo.api.platform.ChangeAuditDao
import com.gs.obevo.api.platform.ChangeCommand
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.CommandExecutionContext
import com.gs.obevo.api.platform.DeployExecutionDao
import com.gs.obevo.api.platform.DeployExecutionException
import com.gs.obevo.api.platform.DeployMetrics
import com.gs.obevo.api.platform.FailedChange
import com.gs.obevo.api.platform.GraphExportFormat
import com.gs.obevo.api.platform.MainDeployerArgs
import com.gs.obevo.api.platform.Platform
import com.gs.obevo.impl.graph.GraphEnricher
import com.gs.obevo.impl.graph.GraphUtil
import com.gs.obevo.impl.text.TextDependencyExtractableImpl
import com.gs.obevo.impl.text.TextDependencyExtractor
import com.gs.obevo.util.inputreader.ConsoleInputReader
import com.gs.obevo.util.inputreader.Credential
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.StopWatch
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.block.function.Function
import org.eclipse.collections.api.collection.ImmutableCollection
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.factory.Predicates
import org.eclipse.collections.impl.block.factory.StringFunctions
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Sets
import org.slf4j.LoggerFactory
import java.sql.Timestamp
import java.util.Date
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

        val newChangeInputSetMap = mutableMapOf<ChangeInput, Set<CodeDependency>>()
        val packageChanges = changeInputs.filter { it.objectKey.changeType.name == ChangeType.PACKAGE_STR || it.objectKey.changeType.name == ChangeType.PACKAGE_BODY }
                .map { it.objectKey.objectName }.toSet()
        changeInputSetMap.onEach { entry ->
            val change = entry.key
            val dependencies = entry.value
            if (change.objectKey.changeType.name == ChangeType.PACKAGE_BODY) {
                newChangeInputSetMap.put(change, dependencies.filterNot { packageChanges.contains(it.target) }.toSet())
            } else {
                newChangeInputSetMap.put(change, dependencies)
            }
        }

        var sourceChanges = changeInputs.collect { input ->
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
                change.isKeepIncrementalOrder = input.isKeepIncrementalOrder
            }

            change.metadataSection = input.metadataSection

            // TODO should not need separate converted*Content fields in Change. Should only be in ChangeInput - see GITHUB#191
            change.convertedContent = input.convertedContent
            change.rollbackContent = input.rollbackContent
            change.convertedRollbackContent = input.convertedRollbackContent

            change.changeInput = input
            change.orderWithinObject = input.orderWithinObject

            change.order = input.order
            change.applyGrants = input.applyGrants
            change.changeset = input.changeset

            change.codeDependencies = Sets.immutable.withAll(
                    newChangeInputSetMap.get(input)  // option 1 - use the inputs extracted from the next if possible
                            ?: input.codeDependencies  // option 2 - use the pre-populated codeDependencies value
                            ?: emptySet()  // fallback - default to empty set
            )

            change.dropContent = input.dropContent
            change.permissionScheme = input.permissionScheme

            return@collect change
        }

        // add rollback scripts here

        val changePairs = ChangesetCreator.getChangePairs(deployedChanges, sourceChanges)

        if (deployerArgs.isRollback) {
            // Add back rollback changes to the sourceList so that they can take part in the change calculation
            val rollbacksToAddBack = changePairs
                    .filter { !it.changeKey.changeType.isRerunnable && it.sourceChange == null && it.deployedChange != null }
                    .map { it.deployedChange as ChangeIncremental }

            rollbacksToAddBack.forEach { it.isRollbackActivated = true }

            sourceChanges = sourceChanges.newWithAll(rollbacksToAddBack)
        }

        // TODO refactor into separate method
        if (env.platform.isDropOrderRequired) {
            // In this block, we set the "dependentChanges" field on the drop objects to ensure they can be sorted for dependencies later on
            val dropsToEnrich = changePairs
                    .filter { it.changeKey.changeType.isRerunnable && it.sourceChange == null && it.deployedChange != null }
                    .map { it.deployedChange!! }

            val dropsByObjectName = dropsToEnrich.associateBy { env.platform.convertDbObjectName().valueOf(it.objectName) }

            val dropsForTextProcessing = dropsToEnrich.map { drop ->
                val sql = changeTypeBehaviorRegistry.getChangeTypeBehavior(drop.changeType).getDefinitionFromEnvironment(drop);
                LOG.debug("Found the sql from the DB for dropping: {}", sql)
                TextDependencyExtractableImpl(drop.objectName, sql ?: "", drop)
            }

            val dropDependencies = this.textDependencyExtractor.calculateDependencies(dropsForTextProcessing)

            dropsForTextProcessing.forEach { it.codeDependencies = Sets.immutable.ofAll(dropDependencies.get(it)) }

            for (drop in dropsForTextProcessing) {
                drop.codeDependencies?.let { deps ->
                    if (deps.notEmpty()) {
                        drop.payload.dependentChanges = Sets.immutable.ofAll(deps.map { dropsByObjectName[it.target] })
                    }
                }
            }
        }


        val dependencyGraph = graphEnricher.createDependencyGraph(sourceChanges, deployerArgs.isRollback)

        deployerArgs.sourceGraphExportFile?.let { sourceGraphOutputFile ->
            val exporterFormat = deployerArgs.sourceGraphExportFormat ?: GraphExportFormat.DOT
            // TODO undo this change
//            val exporterFunc = getExporterFunc(exporterFormat)
//            FileWriter(sourceGraphOutputFile).use { exporterFunc(it, dependencyGraph) }
        }

        sourceChanges.each { it.dependentChanges = GraphUtil.getDependencyNodes(dependencyGraph, it) }

        val artifactsToProcess = changesetCreator.determineChangeset(changePairs, sourceChanges, deployStrategy.isInitAllowedOnHashExceptions)
                .applyDeferredPredicate(deployerArgs.changesetPredicate)

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

//    private fun getExporterFunc(exporterFormat: Enum<GraphExportFormat>): (Writer, Graph<Change, DefaultEdge>) -> Unit {
//        val vertexNameProvider : ComponentNameProvider<Change> = ComponentNameProvider {
//            change : Change -> change.objectName + "." + change.changeName
//        }
//
//        // TODO Temporary - undo this change!
//        when (exporterFormat) {
//            GraphExportFormat.DOT -> return { writer: Writer, graph: Graph<Change, DefaultEdge> ->
//                DOTExporter<Change, DefaultEdge>(IntegerComponentNameProvider<Change>(), vertexNameProvider, null).export(writer, graph)
//            }
//            GraphExportFormat.GML -> return { writer: Writer, graph: Graph<Change, DefaultEdge> ->
//                GmlExporter<Change, DefaultEdge>(IntegerComponentNameProvider<Change>(), vertexNameProvider, IntegerEdgeNameProvider<DefaultEdge>(), null).export(writer, graph)
//            }
//            GraphExportFormat.GRAPHML -> return { writer: Writer, graph: Graph<Change, DefaultEdge> ->
//                GraphMLExporter<Change, DefaultEdge>(IntegerComponentNameProvider<Change>(), vertexNameProvider, IntegerEdgeNameProvider<DefaultEdge>(), null).export(writer, graph)
//            }
//            GraphExportFormat.MATRIX -> return { writer: Writer, graph: Graph<Change, DefaultEdge> ->
//                MatrixExporter<Change, DefaultEdge>().exportAdjacencyMatrix(writer, graph)
//            }
//            else -> throw IllegalArgumentException("Export Format $exporterFormat is not supported here")
//        }
//    }

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
        printArtifactsToProcessForUser(artifactsToProcess, deployStrategy)
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
                    // changeInput may be null due to manufactured changes e.g. object drops
                    change.changeInput?.let { onboardingStrategy.handleSuccess(it) }
                }
            } catch (exc: Exception) {
                changeStopWatch.stop()

                val runtimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.time)

                for (change in changeCommand.changes) {
                    if (change.changeInput != null) {
                        onboardingStrategy.handleException(change.changeInput, exc)
                    }
                }

                LOG.info("Failed to deploy artifact " + changeCommand.commandDescription + ", took "
                        + runtimeSeconds + " seconds")

                LOG.info("We will continue and fail the process at the end. This was the exception: " + ExceptionUtils.getStackTrace(exc))
                failedChanges.add(FailedChange(changeCommand, exc))

                failedObjectNames.withAll(changeCommand.changes.collect { it.dbObjectKey })
                failedChangeKeys.withAll(changeCommand.changes.collect { it.changeKey })
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
            if (input.trim().equals("Y", ignoreCase = true)) {
                return true
            }
        }

        LOG.info("Did not pass command line validation input. Will not proceed w/ actual deployment")
        return false
    }

    private fun printArtifactsToProcessForUser(artifactsToProcess: Changeset, deployStrategy: DeployStrategy) {
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

    companion object {
        private val LOG = LoggerFactory.getLogger(MainDeployer::class.java)
    }
}
