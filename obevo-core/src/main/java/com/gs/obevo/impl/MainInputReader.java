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

import java.util.concurrent.TimeUnit;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.api.platform.Platform;
import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.collections.impl.block.factory.Predicates.and;

public class MainInputReader<P extends Platform, E extends Environment<P>> {
    private static final Logger LOG = LoggerFactory.getLogger(MainInputReader.class);

    private final SourceChangeReader sourceChangeReader;
    private final Predicate<? super Change> dbChangeFilter;
    private final DeployMetricsCollector deployMetricsCollector;

    public MainInputReader(SourceChangeReader sourceChangeReader, Predicate<? super Change> dbChangeFilter, DeployMetricsCollector deployMetricsCollector) {
        this.sourceChangeReader = sourceChangeReader;
        this.dbChangeFilter = dbChangeFilter;
        this.deployMetricsCollector = deployMetricsCollector;
    }

    public void read(E env, final MainDeployerArgs deployerArgs) {
        StopWatch changeStopWatch = new StopWatch();
        changeStopWatch.start();

        boolean mainDeploymentSuccess = false;
        try {
            readInternal(env, deployerArgs);
            mainDeploymentSuccess = true;
        } finally {
            changeStopWatch.stop();
            long deployRuntimeSeconds = TimeUnit.MILLISECONDS.toSeconds(changeStopWatch.getTime());
            deployMetricsCollector.addMetric("runtimeSeconds", deployRuntimeSeconds);
            deployMetricsCollector.addMetric("success", mainDeploymentSuccess);
        }
    }

    protected DeployMetricsCollector getDeployMetricsCollector() {
        return deployMetricsCollector;
    }

    protected ImmutableList<Change> readInternal(E env, final MainDeployerArgs deployerArgs) {
        validateSetup();
        if (deployerArgs.isRollback()) {
            LOG.info("*** EXECUTING IN ROLLBACK MODE ***");
        }
        LOG.info("Now fetching the changed artifacts");

        logArgumentMetrics(deployerArgs);
        logEnvironment(env);
        logEnvironmentMetrics(env);

        ImmutableList<Change> sourceChanges = sourceChangeReader.readSourceChanges(
                deployerArgs.isUseBaseline(),
                and(this.dbChangeFilter, deployerArgs.getChangeInclusionPredicate()));

        for (Change artf : sourceChanges) {
            artf.setEnvironment(env);
        }

        logChanges("source", sourceChanges);

        logChangeMetrics("source", sourceChanges);

        return sourceChanges;
    }

    public void logChanges(String logType, ImmutableCollection<Change> sourceChanges) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Read {} changes from {}", sourceChanges.size(), logType);
        }
        if (LOG.isDebugEnabled()) {
            for (Change sourceChange : sourceChanges) {
                String locationLog = sourceChange.getFileLocation() != null ? ", File: " + sourceChange.getFileLocation() : "";

                LOG.debug("-> {} Change: {}, Hash: {} {}", logType, sourceChange.getDisplayString(), sourceChange.getContentHash(), locationLog);
            }
        }
    }

    protected void validateSetup() {
    }

    private void logArgumentMetrics(MainDeployerArgs deployerArgs) {
        deployMetricsCollector.addMetric("args.onboardingMode", deployerArgs.isOnboardingMode());
        deployMetricsCollector.addMetric("args.init", deployerArgs.isPerformInitOnly());
        deployMetricsCollector.addMetric("args.rollback", deployerArgs.isRollback());
        deployMetricsCollector.addMetric("args.preview", deployerArgs.isPreview());
        deployMetricsCollector.addMetric("args.useBaseline", deployerArgs.isUseBaseline());
    }

    protected void logEnvironment(E env) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Environment information:");
            LOG.info("Logical schemas [{}]: {}", env.getSchemaNames().size(), env.getSchemaNames().makeString(","));
            LOG.info("Physical schemas [{}]: {}", env.getPhysicalSchemas().size(), env.getPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME).makeString(","));
        }
    }

    protected void logEnvironmentMetrics(E env) {
        deployMetricsCollector.addMetric("platform", env.getPlatform().getName());
        deployMetricsCollector.addMetric("schemaCount", env.getSchemaNames().size());
        deployMetricsCollector.addMetric("schemas", env.getSchemaNames().makeString(","));
        deployMetricsCollector.addMetric("physicalSchemaCount", env.getPhysicalSchemas().size());
        deployMetricsCollector.addMetric("physicalSchemas", env.getPhysicalSchemas().collect(PhysicalSchema.TO_PHYSICAL_NAME).makeString(","));
    }

    private void logChangeMetrics(final String changeSide, ImmutableList<Change> changes) {
        MutableBag<String> changeTypeCounts = changes.collect(Change.TO_CHANGE_TYPE_NAME).toBag();
        changeTypeCounts.toMapOfItemToCount().forEachKeyValue(new Procedure2<String, Integer>() {
            @Override
            public void value(String changeType, Integer count) {
                deployMetricsCollector.addMetric("changes." + changeSide + "." + changeType, count);
            }
        });
    }
}
