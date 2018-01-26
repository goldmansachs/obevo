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

import com.gs.obevo.api.platform.CommandExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deployment behavior when initializing the schema, i.e. only create and populate the audit table and not execute any
 * deployments. We are lenient on finding differences in hashes, as we may have changes already in the Audit table, and
 * we may explicitly want to correct them (e.g. for subsequent initializations).
 */
class ForceInitDeployStrategy implements DeployStrategy {
    public static final ForceInitDeployStrategy INSTANCE = new ForceInitDeployStrategy();

    private static final Logger LOG = LoggerFactory.getLogger(ForceInitDeployStrategy.class);

    protected ForceInitDeployStrategy() {
    }

    @Override
    public String getDeployVerbMessage() {
        return "inited (not actually deployed, just marked in the audit table)";
    }

    @Override
    public void deploy(ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry, ExecuteChangeCommand changeCommand, CommandExecutionContext cec) {
        LOG.info("* Not actually deploying this change as we are in INIT mode (just " +
                "marking the audit table)");
    }

    @Override
    public boolean isInitAllowedOnHashExceptions() {
        return true;
    }
}
