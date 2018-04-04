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
package com.gs.obevo.db.cmdline;

import java.util.Date;

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.cmdline.AbstractMain;
import com.gs.obevo.cmdline.DeployerArgs;
import com.gs.obevo.impl.changepredicate.ChangeKeyPredicateBuilder;
import com.gs.obevo.util.inputreader.ConsoleInputReader;
import com.gs.obevo.util.inputreader.UserInputReader;
import org.apache.commons.lang.Validate;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbDeployerMain extends AbstractMain<Environment> {
    private static final Logger LOG = LoggerFactory.getLogger(DbDeployerMain.class);
    private final UserInputReader userInputReader = new ConsoleInputReader();

    public void start(DeployerAppContext ctxt, DeployerArgs args) {
        Environment env = ctxt.getEnvironment();

        LOG.info("Setting up the the environment infrastructure for {}; will {} on setup exception", env.getName(), args.isStrictSetupEnvInfra() ? "fail" : "warn");
        if (args.getForceEnvSetup() != null && args.getForceEnvSetup().booleanValue()) {
            ctxt.setupEnvInfra(args.isStrictSetupEnvInfra(), args.getForceEnvSetup().booleanValue());
        } else {
            ctxt.setupEnvInfra(args.isStrictSetupEnvInfra());
        }

        if (args.shouldExecuteClean()) {
            this.checkIfCleanBuildShouldProceed(env, args.isNoPrompt());
            LOG.info("Now cleaning the environment " + env.getName()
                    + ", as specified at the command line");
            ctxt.cleanEnvironment();
            LOG.info("Done cleaning the environment");
        }

        if (args.shouldExecuteDeploy()) {
            LOG.info("Starting deployment for [" + env.getDisplayString() + "] at time [" + new Date() + "]");
            MainDeployerArgs dbArgs = new MainDeployerArgs();
            // Note - while the API-side arg objects default to noPrompt == true, the command-line API will default it to false
            // (using appropriate defaults for each mechanism)
            dbArgs.setNoPrompt(args.isNoPrompt());
            dbArgs.setPerformInitOnly(args.isPerformInitOnly());
            dbArgs.setPreview(args.isPreview());
            dbArgs.setRollback(args.isRollback());
            dbArgs.setUseBaseline(args.isUseBaseline());
            dbArgs.setOnboardingMode(args.isOnboardingMode());
            dbArgs.setProductVersion(args.getProductVersion());
            if (args.getChangesets() != null && args.getChangesets().length > 0) {
                dbArgs.setChangesetNames(ArrayAdapter.adapt(args.getChangesets()).toSet().toImmutable());
            }
            if (args.isAllChangesets()) {
                dbArgs.setAllChangesets(args.isAllChangesets());
            }
            if (args.getChangeCriteria() != null) {
                dbArgs.setChangeInclusionPredicate(ChangeKeyPredicateBuilder.parseFullPredicate(args.getChangeCriteria()));
            }

            ctxt.deploy(dbArgs);
        }
    }

    private void checkIfCleanBuildShouldProceed(Environment env, boolean noPrompt) {
        LOG.info("Request was made to clean the environment...");

        Validate.isTrue(env.isCleanBuildAllowed(), "Clean build not allowed for this environment [" + env.getName()
                + "] ! Exiting...");

        if (!noPrompt) {
            LOG.info("WARNING - This will wipe the whole environment!!! Are you sure you want to proceed? (Y/N)");

            String input = this.userInputReader.readLine(null);
            Validate.isTrue(input.trim().equalsIgnoreCase("Y"), "User did not enter Y. Hence, we will exit from here.");
        }
    }
}
