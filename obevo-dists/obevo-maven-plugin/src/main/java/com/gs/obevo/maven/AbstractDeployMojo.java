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
package com.gs.obevo.maven;

import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.apps.baselineutil.BaselineValidatorMain;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;

public abstract class AbstractDeployMojo extends AbstractMojo {
    @Parameter(property = "da.env")
    private String env;

    @Parameter(property = "da.user")
    private String user;

    @Parameter(property = "da.password")
    private String password;

    @Parameter(property = "da.keytabPath")
    private String keytabPath;

    @Parameter(property = "da.useKerberosAuth")
    private boolean useKerberosAuth;

    /**
     * Where the source files for this deployment are (required)
     */
    @Parameter(property = "da.sourcePath")
    private String sourcePath;

    /**
     * This is a Boolean object as it can be null - we may choose the default to be false or true
     * depending on the context of the mojo
     */
    @Parameter(property = "da.cleanFirst", defaultValue = "false")
    Boolean cleanFirst;

    @Parameter(property = "da.noPrompt", defaultValue = "false")
    private Boolean noPrompt;

    @Parameter(property = "da.validateBaseline", defaultValue = "false")
    private boolean validateBaseline;

    @Parameter(property = "da.performInitOnly", defaultValue = "false")
    private boolean performInitOnly;

    @Parameter(property = "da.preview", defaultValue = "false")
    private boolean preview;

    @Parameter(property = "da.rollback", defaultValue = "false")
    private boolean rollback;

    /**
     * Only would need to be used for test deployments where we want to guarantee that all are deployed.
     */
    @Parameter(property = "da.allChangesets", defaultValue = "false")
    private boolean allChangesets;

    public void setEnv(String env) {
        this.env = env;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    void setCleanFirst(boolean cleanFirst) {
        this.cleanFirst = cleanFirst;
    }

    void setNoPrompt(boolean noPrompt) {
        this.noPrompt = noPrompt;
    }

    void setPerformInitOnly(boolean performInitOnly) {
        this.performInitOnly = performInitOnly;
    }

    void setPreview(boolean preview) {
        this.preview = preview;
    }

    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    void setAllChangesets(boolean allChangesets) {
        this.allChangesets = allChangesets;
    }

    void validateIsPopulated(Object obj, String message) throws MojoExecutionException {
        if (obj == null) {
            throw new MojoExecutionException("Must pass in the " + message + " parameter");
        }
    }

    void validateIsSourcePathPopulated() throws MojoExecutionException {
        if (this.sourcePath == null) {
            throw new MojoExecutionException("Must pass in the -sourcePath parameter");
        }
    }

    String getSourcePath() {
        return sourcePath;
    }

    public void execute() throws MojoExecutionException {
        try {
            final Credential credential = getCredential();
            if (this.validateBaseline) {
                DbEnvironment dbEnvironment = DbEnvironmentFactory.getInstance().readOneFromSourcePath(getSourcePath(), this.env.split(","));
                DbDeployerAppContext dbDeployerAppContext = dbEnvironment.buildAppContext(credential);
                new BaselineValidatorMain().validateNoBaselineBreaks(dbDeployerAppContext);
            } else {
                MutableCollection<DbEnvironment> dbEnvironments = DbEnvironmentFactory.getInstance().readFromSourcePath(getSourcePath(), this.env.split(","));
                this.getLog().info("Will action these environments: " + dbEnvironments.collect(new Function<DbEnvironment, Object>() {
                    @Override
                    public Object valueOf(DbEnvironment it) {
                        return it.getName();
                    }
                }).makeString(","));
                MainDeployerArgs dbArgs = new MainDeployerArgs()
                        .noPrompt(this.noPrompt != null && this.noPrompt)
                        .performInitOnly(performInitOnly)
                        .preview(preview)
                        .rollback(rollback);

                for (DbEnvironment dbEnvironment : dbEnvironments) {
                    DbDeployerAppContext dbDeployerAppContext = dbEnvironment.buildAppContext(credential);
                    dbDeployerAppContext.setupEnvInfra();
                    if (this.cleanFirst != null && this.cleanFirst) {
                        dbDeployerAppContext.cleanEnvironment();
                    }
                    dbDeployerAppContext.deploy(dbArgs);
                }
            }
        } catch (RuntimeException e) {
            this.getLog().info(e);
            throw e;
        }
    }

    Credential getCredential() {
        String passwordToUse = password;
        if ("BLANK".equals(passwordToUse)) {
            // maven can't handle blank passwords, say for unit test envs. Hence, we allow for this
            getLog().info("BLANK specified as password; hence, passing in the empty string \"\" as the value");
            passwordToUse = "";
        }

        if (user == null && passwordToUse == null) {
            return null;
        } else if (user != null && passwordToUse != null) {
            return new Credential(user, passwordToUse);
        } else if (user != null && keytabPath != null) {
            Credential credential = new Credential();
            credential.setUsername(user);
            credential.setKeytabPath(keytabPath);
            return credential;
        } else if (user != null && useKerberosAuth) {
            Credential credential = new Credential();
            credential.setUsername(user);
            credential.setUseKerberosAuth(useKerberosAuth);
            return credential;
        } else {
            throw new IllegalArgumentException("Must specify either 1) both the user and pass  2) neither. Instead, got user: " + user + ", password: " + passwordToUse);
        }
    }
}
