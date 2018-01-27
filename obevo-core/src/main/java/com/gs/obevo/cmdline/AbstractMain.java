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
package com.gs.obevo.cmdline;

import java.io.File;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.factory.Obevo;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.util.FileUtilsCobra;
import com.gs.obevo.util.RegexUtil;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.inputreader.CredentialReader;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMain<EnvType extends Environment> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMain.class);
    private final CredentialReader credentialReader = new CredentialReader();

    private MutableCollection<EnvType> getRequestedSystem(String sourcePath) {
        return Obevo.<EnvType>readEnvironments(sourcePath).toList();
    }

    private RichIterable<EnvType> getRequestedEnvironments(String sourcePath, String... envNames) {
        MutableCollection<EnvType> environments = getRequestedSystem(sourcePath);

        MutableList<EnvType> requestedEnvs = Lists.mutable.empty();

        if (envNames == null || envNames.length == 0) {
            requestedEnvs.add(readSingleEnvironment(environments, sourcePath));
        } else {
            for (EnvType sysEnv : environments) {
                for (String envPattern : envNames) {
                    if (Pattern.compile(RegexUtil.convertWildcardPatternToRegex(envPattern))
                            .matcher(sysEnv.getName())
                            .matches()) {
                        requestedEnvs.add(sysEnv);
                    }
                }
            }
        }

        if (requestedEnvs.isEmpty()) {
            throw new IllegalArgumentException("No environment with name/s "
                    + Lists.mutable.with(envNames).makeString("(", ",", ")") + " found");
        }

        return requestedEnvs;
    }

    private EnvType readSingleEnvironment(MutableCollection<EnvType> environments, String checkoutFolder) {
        if (environments.size() == 1) {
            return environments.getFirst();
        } else if (environments.size() == 0) {
            throw new IllegalArgumentException("No environments found at this directory: " + checkoutFolder);
        } else {
            throw new IllegalArgumentException(
                    "Multiple environments of names ["
                            + environments.collect(Environment.TO_NAME)
                            + "] found at: "
                            + checkoutFolder
                            + "\n"
                            + "We only expect one env to exist. Please correct your config setup, or pass in the env variable to explicitly pick an evironment");
        }
    }

    private DeployerAppContext createRuntimeContext(EnvType env, DeployerArgs args) {
        final File workDir;
        if (args.getWorkDir() == null) {
            workDir = FileUtilsCobra.createTempDir("deploy-" + env.getName());
        } else {
            workDir = args.getWorkDir();
        }

        LOG.info("Using working directory for env {} as: {}", env.getName(), workDir);

        Credential credential = this.credentialReader.getCredential(args.getDeployUserId(), args.getPassword(),
                args.isUseKerberosAuth(), args.getKeytabPath(), env.getDefaultUserId(), env.getDefaultPassword());

        LOG.info("Running deployment as user {}", credential.getUsername());

        return createRuntimeContext(env, workDir, credential);
    }

    private DeployerAppContext createRuntimeContext(EnvType env, File workDir, Credential credential) {
        return (DeployerAppContext) env.getAppContextBuilder()
                .setEnvironment(env)
                .setCredential(credential)
                .setWorkDir(workDir)
                .build();
    }

    public void start(final DeployerArgs args) {
        RichIterable<EnvType> requestedEnvs = getRequestedEnvironments(args.getSourcePath(), args.getEnvNames());

        RichIterable<DeployerAppContext> deployContexts = requestedEnvs.collect(new Function<EnvType, DeployerAppContext>() {
            @Override
            public DeployerAppContext valueOf(EnvType environment) {
                return createRuntimeContext(environment, args);
            }
        });

        for (DeployerAppContext ctxt : deployContexts) {
            this.start(ctxt, args);
        }
    }

    protected abstract void start(DeployerAppContext ctxt, DeployerArgs args);
}
