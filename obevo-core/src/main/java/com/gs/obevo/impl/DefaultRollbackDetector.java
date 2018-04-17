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
package com.gs.obevo.impl;

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.util.VisibleForTesting;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Stacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to encapsulate the logic of detecting rollbacks of deployment executions. This is based on having the
 * product version metadata persisted in the target environment itself and comparing via the client product version.
 */
public class DefaultRollbackDetector implements RollbackDetector {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRollbackDetector.class);

    /**
     * Returns true/false if all the schemas in the environment either need rollback (true) or don't (false).
     *
     * If some do and some don't, an exception is thrown.
     */
    @Override
    public boolean determineRollback(final String productVersion, final ImmutableSet<String> schemas, final DeployExecutionDao deployExecutionDao) {
        MutableMap<String, Boolean> rollbackFlags = schemas.toMap(
                Functions.getPassThru(),
                schema -> {
                    LOG.info("Checking rollback status on Product Version {} and Schema {}", productVersion, schema);
                    return determineRollbackForSchema(productVersion, deployExecutionDao.getDeployExecutions(schema));
                }
        );

        MutableSet<Boolean> values = rollbackFlags.valuesView().toSet();
        if (values.size() > 1) {
            MutableSetMultimap<Boolean, String> schemasByRollbackFlag = rollbackFlags.flip();
            MutableSet<String> rollbackSchemas = schemasByRollbackFlag.get(Boolean.TRUE);
            MutableSet<String> nonrollbackSchemas = schemasByRollbackFlag.get(Boolean.FALSE);

            throw new IllegalArgumentException("The following schemas were calculated for rollback [" + rollbackSchemas + "], though the rest were not [" + nonrollbackSchemas + "]; cannot proceed in this mixed mode");
        }

        return values.iterator().next().booleanValue();
    }

    @VisibleForTesting
    boolean determineRollbackForSchema(final String deployVersion, ImmutableCollection<DeployExecution> deployExecutions) {
        logDeployExecutions(deployExecutions, "deploy executions");

        ImmutableList<DeployExecution> activeDeployments = getActiveDeployments(deployExecutions);

        logDeployExecutions(activeDeployments, "filtered active deploy executions");

        if (activeDeployments == null || activeDeployments.isEmpty()) {
            return false;
        }

        if (getDeployVersion(activeDeployments.getLast()).equals(deployVersion)) {
            return false;
        }

        ImmutableList<DeployExecution> deploymentsExcludingTheLast = activeDeployments.subList(0, activeDeployments.size() - 1);

        ImmutableList<DeployExecution> rollbackIndicativeDeployments = deploymentsExcludingTheLast.select(new Predicate<DeployExecution>() {
            @Override
            public boolean accept(DeployExecution pastDeployment) {
                return getDeployVersion(pastDeployment).equals(deployVersion);
            }
        });
        logDeployExecutions(rollbackIndicativeDeployments, "deploy executions that indicate a rollback");
        return rollbackIndicativeDeployments.notEmpty();
    }

    private void logDeployExecutions(ImmutableCollection<DeployExecution> deployExecutions, String message) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Found {} {} for this schema", deployExecutions.size(), message);
            if (LOG.isDebugEnabled()) {
                for (DeployExecution deployExecution : deployExecutions.toSortedListBy(DeployExecution::getId)) {
                    LOG.debug("Execution ID={}, Version Name={}, Deploy Time={}, Rollback={}",
                            deployExecution.getId(), getDeployVersion(deployExecution), deployExecution.getDeployTime(), deployExecution.isRollback());
                }
            }
        }
    }

    /**
     * Returns the active deployments from the given list, i.e. removing the impact of those deployments rolled back.
     *
     * Logic:
     * -Play through the history of the DeployExecutions sorting by the ID field
     * -If a regular deploy is found, push it to the stack
     * -If a rollback is found, pop items from the stack until we find the corresponding version to be rolled back, and
     * then add the (new) rollback version to the stack. We assume the prior version must exist; if not, an exception is
     * thrown.
     */
    @VisibleForTesting
    ImmutableList<DeployExecution> getActiveDeployments(ImmutableCollection<DeployExecution> deployExecutions) {
        if (deployExecutions == null) {
            return Lists.immutable.empty();
        }

        MutableList<DeployExecution> idSortedExecutions = deployExecutions.toSortedListBy(DeployExecution::getId);
        MutableStack<DeployExecution> executionStack = Stacks.mutable.empty();

        for (DeployExecution currentExecution : idSortedExecutions) {
            if (!currentExecution.isRollback()) {
                executionStack.push(currentExecution);
            } else {
                while (true) {
                    if (executionStack.isEmpty()) {
                        throw new IllegalStateException("Found a rollback deployment without the corresponding version: " + getDeployVersion(currentExecution) + ", " + currentExecution);
                    } else {
                        DeployExecution previousExecution = executionStack.pop();
                        if (getDeployVersion(previousExecution).equals(getDeployVersion(currentExecution))) {
                            executionStack.push(currentExecution);
                            break;
                        }
                    }
                }
            }
        }

        return executionStack.toList().reverseThis().toImmutable();
    }

    private String getDeployVersion(DeployExecution deployExecution) {
        return ObjectUtils.defaultIfNull(deployExecution.getProductVersion(), "no-version-available");
    }
}
