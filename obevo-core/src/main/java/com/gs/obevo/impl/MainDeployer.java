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
package com.gs.obevo.impl;

import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.DeployExecutionStatus;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ToolVersion;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.util.inputreader.ConsoleInputReader;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.inputreader.UserInputReader;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class orchestrates the db deployment logic for the given platform and environment.
 */
public class MainDeployer<P extends Platform, E extends Environment<P>> {
    private static final Logger LOG = LoggerFactory.getLogger(MainDeployer.class);

    private final UserInputReader userInputReader = new ConsoleInputReader();
    private final ChangeAuditDao artifactDeployerDao;
    private final ChangesetCreator changesetCreator;
    private final PostDeployAction postDeployAction;
    private final DeployMetricsCollector deployMetricsCollector;
    private final DeployExecutionDao deployExecutionDao;
    private final Credential credential;
    private final MainInputReader mainInputReader;


    public MainDeployer(ChangeAuditDao artifactDeployerDao,
            MainInputReader mainInputReader,
            ChangesetCreator changesetCreator,
            PostDeployAction postDeployAction,
            DeployMetricsCollector deployMetricsCollector,
            DeployExecutionDao deployExecutionDao,
            Credential credential
    ) {
        this.artifactDeployerDao = artifactDeployerDao;
        this.mainInputReader = mainInputReader;
        this.changesetCreator = changesetCreator;
        this.postDeployAction = postDeployAction;
        this.deployMetricsCollector = deployMetricsCollector;
        this.deployExecutionDao = deployExecutionDao;
        this.credential = credential;
    }

    protected ChangeAuditDao getArtifactDeployerDao() {
        return artifactDeployerDao;
    }

    protected DeployMetricsCollector getDeployMetricsCollector() {
        return deployMetricsCollector;
    }

    protected DeployExecutionDao getDeployExecutionDao() {
        return deployExecutionDao;
    }

    public void execute(final E env, final MainDeployerArgs deployerArgs) {
        StopWatch changeStopWatch = new StopWatch();
        changeStopWatch.start();

        boolean mainDeploymentSuccess = false;
        try {
            executeInternal(env, deployerArgs);
            mainDeploymentSuccess = true;
        } finally {
            changeStopWatch.stop();
            long deployRuntimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.getTime());
            deployMetricsCollector.addMetric("runtimeSeconds", deployRuntimeSeconds);
            deployMetricsCollector.addMetric("success", mainDeploymentSuccess);
        }
    }

    private void executeInternal(final E env, final MainDeployerArgs deployerArgs) {
        LOG.info("Running {} version {}", ToolVersion.getToolName(), ToolVersion.getToolVersion());

        if (deployerArgs.getProductVersion() != null) {
            RollbackDetector rollbackDetector = new DefaultRollbackDetector();

            boolean rollbackDetectedFromAudit = env.isRollbackDetectionEnabled() && rollbackDetector.determineRollback(deployerArgs.getProductVersion(), env.getSchemaNames(), deployExecutionDao);
            if (rollbackDetectedFromAudit) {
                LOG.info("Detected a rollback based on the audit data; will proceed in rollback mode");
                // we only call the setter if we calculate rollback==true so that we don't override an intentional
                // rollback parameter from the client (i.e. in case the audit does not want a rollback). Though this
                // should never be the case
                deployerArgs.setRollback(rollbackDetectedFromAudit);
            } else if (deployerArgs.isRollback()) {
                LOG.warn("You have requested a rollback in your arguments, though your version metadata does not require it.");
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".rollbackInvokedDespiteAuditData", true);
            }
        }

        ImmutableList<Change> sourceChanges = mainInputReader.readInternal(env, deployerArgs);
        OnboardingStrategy onboardingStrategy = getOnboardingStrategy(deployerArgs);
        onboardingStrategy.validateSourceDirs(env.getSourceDirs(), env.getSchemaNames());

        DeployStrategy deployStrategy = getDeployMode(deployerArgs);

        ImmutableCollection<Change> deployedChanges = readDeployedChanges(deployerArgs);
        mainInputReader.logChanges("deployed", deployedChanges);

        Changeset artifactsToProcess = changesetCreator.determineChangeset(deployedChanges, sourceChanges, deployerArgs.isRollback(), deployStrategy.isInitAllowedOnHashExceptions(), deployerArgs.getChangesetPredicate());

        validatePriorToDeployment(env, deployStrategy, sourceChanges, deployedChanges, artifactsToProcess);

        if (this.shouldProceedWithDbChange(artifactsToProcess, deployerArgs)) {
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                initializeSchema(env, schema);
            }

            // Note - only init the audit table if we actually proceed w/ a deploy
            this.deployExecutionDao.init();
            this.artifactDeployerDao.init();

            MapIterable<String, DeployExecution> executionsBySchema = env.getSchemas().toMap(Schema.TO_NAME, new Function<Schema, DeployExecution>() {
                @Override
                public DeployExecution valueOf(final Schema schema) {
                    DeployExecution deployExecution = new DeployExecutionImpl(
                            deployerArgs.getDeployRequesterId(),
                            credential.getUsername(),
                            schema.getName(),
                            ToolVersion.getToolVersion(),
                            new Timestamp(new Date().getTime()),
                            deployerArgs.isPerformInitOnly(),
                            deployerArgs.isRollback(),
                            deployerArgs.getProductVersion(),
                            deployerArgs.getReason(),
                            deployerArgs.getDeployExecutionAttributes()
                    );
                    deployExecution.setStatus(DeployExecutionStatus.IN_PROGRESS);
                    deployExecutionDao.persistNew(deployExecution, env.getPhysicalSchema(schema.getName()));
                    return deployExecution;
                }
            });

            // If there are no deployments required, then just update the artifact tables and return
            if (!artifactsToProcess.isDeploymentNeeded()) {
                LOG.info("No changes detected in the database deployment. Updating Deploy Status");
                for (DeployExecution deployExecution : executionsBySchema.valuesView()) {
                    deployExecution.setStatus(DeployExecutionStatus.SUCCEEDED);
                    this.deployExecutionDao.update(deployExecution);
                }
                return;
            }

            String action = deployerArgs.isRollback() ? "Rollback" : "Deployment";

            boolean mainDeploymentSuccess = false;
            try {
                this.doExecute(artifactsToProcess, deployStrategy, onboardingStrategy, executionsBySchema);
                LOG.info(action + " has Completed Successfully!");
                for (DeployExecution deployExecution : executionsBySchema.valuesView()) {
                    deployExecution.setStatus(DeployExecutionStatus.SUCCEEDED);
                    this.deployExecutionDao.update(deployExecution);
                }

                mainDeploymentSuccess = true;
            } catch (RuntimeException exc) {
                LOG.info(action
                        + " has Failed. We will error out, but first complete the post-deploy step");
                for (DeployExecution deployExecution : executionsBySchema.valuesView()) {
                    deployExecution.setStatus(DeployExecutionStatus.SUCCEEDED);
                    this.deployExecutionDao.update(deployExecution);
                }
                throw exc;
            } finally {
                LOG.info("Executing the post-deploy step");
                try {
                    doPostDeployAction(env, sourceChanges);
                    this.postDeployAction.value(env);
                } catch (RuntimeException exc) {
                    if (mainDeploymentSuccess) {
                        LOG.info("Exception found in the post-deploy step", exc);
                        throw exc;
                    } else {
                        LOG.error("Exception found in the post-deploy step; printing it out here, but there was an exception during the regular deploy as well", exc);
                    }
                }
                LOG.info("Exiting!");
            }
        }
    }

    protected void doPostDeployAction(E env, ImmutableList<Change> sourceChanges) {

    }

    protected void validatePriorToDeployment(E env, DeployStrategy deployStrategy, ImmutableList<Change> sourceChanges, ImmutableCollection<Change> deployedChanges, Changeset artifactsToProcess) {
        printArtifactsToProcessForUser(artifactsToProcess, deployStrategy, env, deployedChanges, sourceChanges);

        logChangeset(artifactsToProcess);
        artifactsToProcess.validateForDeployment();
    }

    private ImmutableCollection<Change> readDeployedChanges(MainDeployerArgs args) {
        ImmutableCollection<Change> deployedChanges = this.artifactDeployerDao.getDeployedChanges()
                .select(args.getChangeInclusionPredicate());

        return deployedChanges;
    }


    private DeployStrategy getDeployMode(MainDeployerArgs deployerArgs) {
        if (deployerArgs.isPerformInitOnly()) {
            return ForceInitDeployStrategy.INSTANCE;
        } else {
            return ExecuteDeployStrategy.INSTANCE;
        }
    }

    private OnboardingStrategy getOnboardingStrategy(MainDeployerArgs deployerArgs) {
        return deployerArgs.isOnboardingMode() ? new EnabledOnboardingStrategy() : new DisabledOnboardingStrategy();
    }

    private static final class FailedChange {
        private final ExecuteChangeCommand changeCommand;
        private final Exception exception;

        public static final Function<FailedChange, ExecuteChangeCommand> TO_CHANGE_COMMAND = new Function<FailedChange, ExecuteChangeCommand>() {
            @Override
            public ExecuteChangeCommand valueOf(FailedChange object) {
                return object.getChangeCommand();
            }
        };

        public FailedChange(ExecuteChangeCommand changeCommand, Exception exception) {
            this.changeCommand = changeCommand;
            this.exception = exception;
        }

        public ExecuteChangeCommand getChangeCommand() {
            return this.changeCommand;
        }

        public Exception getException() {
            return this.exception;
        }
    }

    private void doExecute(Changeset artifactsToProcess, DeployStrategy deployStrategy, OnboardingStrategy onboardingStrategy, MapIterable<String, DeployExecution> executionsBySchema) {
        MutableList<FailedChange> failedChanges = Lists.mutable.empty();
        MutableSet<String> failedDbObjects = UnifiedSet.newSet();
        MutableSet<String> failedDbObjectNames = UnifiedSet.newSet();  // TODO we should merge the failedDbObjects* variables; depends on fixing the EnabledOnboardingStrategy for detecting prior exceptions

        for (AuditChangeCommand auditChangeCommand : artifactsToProcess.getAuditChanges()) {
            auditChangeCommand.markAuditTable(this.artifactDeployerDao, executionsBySchema.get(auditChangeCommand.getSchema()));
        }

        for (ExecuteChangeCommand changeCommand : artifactsToProcess.getInserts()) {
            MutableSet<String> previousFailedObjects = failedDbObjects.intersect(changeCommand.getChanges().toSet()
                    .collect(Change.TO_DB_OBJECT_KEY));
            if (previousFailedObjects.notEmpty()) {
                // We skip subsequent changes in objects that failed as we don't any unexpected activities to happen on
                // a particular DB object
                // (e.g. if one change relied on a previous one, and the previous one failed; what if something goes bad
                // if the first one isn't executed?)
                LOG.info(String.format(
                        "Skipping this artifact as a previous change for these DB objects [%s] has failed: %s",
                        previousFailedObjects.makeString(", "), changeCommand.getCommandDescription()));
                continue;
            }

            LOG.info("Attempting to deploy: " + changeCommand.getCommandDescription());

            StopWatch changeStopWatch = new StopWatch();
            changeStopWatch.start();

            try {
                deployStrategy.deploy(changeCommand);
                changeCommand.markAuditTable(this.artifactDeployerDao, executionsBySchema.get(changeCommand.getSchema()));

                changeStopWatch.stop();
                long runtimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.getTime());
                LOG.info("Successfully " + deployStrategy.getDeployVerbMessage() + " artifact " + changeCommand.getCommandDescription() +
                        ", took " + runtimeSeconds + " seconds");

                for (Change change : changeCommand.getChanges()) {
                    onboardingStrategy.handleSuccess(change);
                }
            } catch (Exception exc) {
                changeStopWatch.stop();
                long runtimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.getTime());

                for (Change change : changeCommand.getChanges()) {
                    onboardingStrategy.handleException(change, exc, failedDbObjectNames);
                }

                LOG.info("Failed to deploy artifact " + changeCommand.getCommandDescription() + ", took "
                        + runtimeSeconds + " seconds");

                LOG.info("We will continue and fail the process at the end. This was the exception: "
                        + ExceptionUtils.getStackTrace(exc));
                failedChanges.add(new FailedChange(changeCommand, exc));

                failedDbObjectNames.withAll(changeCommand.getChanges().collect(Change.objectName()));
                failedDbObjects.withAll(changeCommand.getChanges().collect(Change.TO_DB_OBJECT_KEY));
            }
        }

        if (!failedChanges.isEmpty()) {
            deployMetricsCollector.addMetric("exceptionCount", failedChanges.size());
            String exceptionMessage = "Failed deploying the following artifacts.\n"
                    + failedChanges.collect(FailedChange.TO_CHANGE_COMMAND).collect(ChangeCommand.TO_COMMAND_DESCRIPTION)
                    .makeString("\n");
            LOG.error(exceptionMessage);
            LOG.error("");
            for (FailedChange failedChange : failedChanges) {
                LOG.error("This change failed: " + failedChange.getChangeCommand().getCommandDescription());
                LOG.error("From exception: " + ExceptionUtils.getStackTrace(failedChange.getException()));
                LOG.error("");
                LOG.error("");
            }
            throw new DeployerRuntimeException(exceptionMessage);
        }
    }

    public static void printCommands(RichIterable<? extends ChangeCommand> commands, String message) {
        if (commands.notEmpty()) {
            LOG.info("The following " + message + ":");
            for (ChangeCommand changeCommand : commands) {
                LOG.info("\t" + changeCommand.getCommandDescription());
            }
            LOG.info("");
        } else {
            LOG.info("Nothing applicable for: [" + message + "]");
            LOG.info("");
        }
    }

    private boolean shouldProceedWithDbChange(Changeset artifactsToProcess, MainDeployerArgs args) {
        if (args.isPreview()) {
            LOG.info("We are in PREVIEW mode, so we will not proceed further. Exiting.");
            return false;
        } else if (args.isNoPrompt()) {
            LOG.info("-noPrompt parameter was passed; hence, we can proceed w/out user prompting");
            return true;
        } else if (!artifactsToProcess.isDeploymentNeeded()){
            LOG.info("No artifacts to deploy, hence we can proceed without user prompting.");
            LOG.info("Will only update the Artifact Execution tables ");
            return true;
        } else {
            LOG.info("Deploying to a live environment; hence, we need to prompt user confirmation");
            LOG.info("Are you sure you want to proceed? (Y/N)");

            String input = this.userInputReader.readLine(null);
            if (input.trim().equalsIgnoreCase("Y")) {
                return true;
            }
        }

        LOG.info("Did not pass command line validation input. Will not proceed w/ actual deployment");
        return false;
    }

    protected void initializeSchema(Environment env, PhysicalSchema schema) {}


    protected void printArtifactsToProcessForUser(Changeset artifactsToProcess, DeployStrategy deployStrategy, E env, ImmutableCollection<Change> deployedChanges, ImmutableCollection<Change> sourceChanges) {
        printCommands(artifactsToProcess.getInserts(), "DB Changes are to be " + deployStrategy.getDeployVerbMessage());
        printCommands(artifactsToProcess.getAuditChanges(), "auditTable-only changes are to be deployed");
        printCommands(artifactsToProcess.getChangeWarnings(), "warnings/errors are for your information (no changes " +
                "done)");
        printCommands(artifactsToProcess.getDeferredChanges(), "DB Changes are deferred from deploying here as they have the changeset attribute set and the relevant changeset argument was not provided at input");
    }

    private void logChangeset(Changeset changeset) {
        deployMetricsCollector.addMetric("changeset.executeCount", changeset.getInserts().size());
        deployMetricsCollector.addMetric("changeset.auditCount", changeset.getAuditChanges().size());
        deployMetricsCollector.addMetric("changeset.warningCount", changeset.getChangeWarnings().size());
        deployMetricsCollector.addMetric("changeset.deferredCount", changeset.getDeferredChanges().size());

        MutableBag<String> warningBag = changeset.getChangeWarnings().collect(new Function<ChangeCommandWarning, String>() {
            @Override
            public String valueOf(ChangeCommandWarning warning) {
                return warning.getClass().getName();
            }
        }).toBag();
        warningBag.toMapOfItemToCount().forEachKeyValue(new Procedure2<String, Integer>() {
            @Override
            public void value(String warningClassName, Integer count) {
                deployMetricsCollector.addMetric("changeset.warningTypeCounts." + warningClassName, count);
            }
        });
    }
}
