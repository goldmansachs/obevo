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
package com.gs.obevo.cmdline;

import java.io.File;
import java.util.Arrays;

import com.gs.obevo.util.inputreader.Credential;
import com.sampullara.cli.Argument;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.block.factory.primitive.BooleanPredicates;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public class DeployerArgs {
    private String[] envNames;
    private String[] actions;
    private boolean setupEnvInfra = false;
    private boolean setupEnvInfraOnly = false;
    private boolean cleanFirst = false;
    private boolean cleanOnly = false;
    private boolean noPrompt = false;
    private boolean useBaseline = false;
    private String includeFile = null;
    private String password;
    private boolean useKerberosAuth;
    private String keytabPath;
    private String sourcePath;
    private File workDir;
    private String deployUserId;
    private boolean performInitOnly = false;
    private boolean preview = false;
    private boolean rollback = false;
    private boolean persistJilJobLoadOrder = false;
    private boolean onboardingMode;
    private String changeCriteria;
    private String[] changesets;
    private boolean allChangesets;
    private String productVersion;
    private boolean lenientSetupEnvInfra = false;

    @Argument(value = "env", required = false)
    public void setEnvNames(String[] envNames) {
        this.envNames = envNames;
    }

    public String[] getEnvNames() {
        return this.envNames;
    }

    public String[] getActions() {
        return actions;
    }

    @Argument(value = "action", required = false,
            description = "3 possible arguments as a comma-separated list - deploy, clean (wipes all db objects), and setup (DEPRECATED). Defaults to deploy."
    )
    public void setActions(String[] actions) {
        this.actions = actions;
    }

    public boolean isSetupEnvInfra() {
        return this.setupEnvInfra;
    }

    @Argument(value = "setupEnvInfra", description = "DEPRECATED")
    public void setSetupEnvInfra(boolean setupEnvInfra) {
        this.setupEnvInfra = setupEnvInfra;
    }

    public boolean isSetupEnvInfraOnly() {
        return this.setupEnvInfraOnly;
    }

    @Argument(value = "setupEnvInfraOnly", description = "DEPRECATED")
    public void setSetupEnvInfraOnly(boolean setupEnvInfraOnly) {
        this.setupEnvInfraOnly = setupEnvInfraOnly;
    }

    public boolean isCleanFirst() {
        return this.cleanFirst;
    }

    @Argument(
            value = "cleanFirst",
            description = "DEPRECATED - use the -action argument. Indicates if the environments should be wiped prior to development. Only allowed for certain environments, per the metadata setup")
    public void setCleanFirst(boolean cleanFirst) {
        this.cleanFirst = cleanFirst;
    }

    public boolean isCleanOnly() {
        return this.cleanOnly;
    }

    @Argument(
            value = "cleanOnly",
            description = "DEPRECATED - use the -action argument. Indicates if the environments should be wiped prior to development and nothing else done. " +
                    "Only allowed for certain environments, per the metadata setup")
    public void setCleanOnly(boolean cleanOnly) {
        this.cleanOnly = cleanOnly;
    }

    public boolean isUseBaseline() {
        return this.useBaseline;
    }

    public String getIncludeFile() {
        return this.includeFile;
    }

    @Argument(value = "useBaseline", description = "If set, then will use the baseline files for db table deployments. Only to be used in a test schema")
    public void setUseBaseline(boolean useBaseline) {
        this.useBaseline = useBaseline;
    }

    @Argument(value = "includeFile", description = "If set, then will use the include files for db objects deployments.")
    public void setIncludeFile(String includeFile) {
        this.includeFile = includeFile;
    }

    public boolean isNoPrompt() {
        return this.noPrompt;
    }

    @Argument(value = "noPrompt", description = "If set, then the deployment will proceed without user prompting")
    public void setNoPrompt(boolean noPrompt) {
        this.noPrompt = noPrompt;
    }

    public String getPassword() {
        return this.password;
    }

    @Argument(value = "password", required = false, description = "kerberos password for the deployUserId")
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseKerberosAuth() {
        return this.useKerberosAuth;
    }

    @Argument(value = "useKerberosAuth", required = false, description = "true if an existing kerberos ticket should be used for authentication; defaults to false (i.e. needing a password)")
    public void setUseKerberosAuth(boolean useKerberosAuth) {
        this.useKerberosAuth = useKerberosAuth;
    }

    public String getKeytabPath() {
        return keytabPath;
    }

    @Argument(value = "keytabPath", required = false, description = "path to the keytab to authenticate it; only supported in Sybase IQ ODBC drivers currently")
    public void setKeytabPath(String keytabPath) {
        this.keytabPath = keytabPath;
    }

    public String getSourcePath() {
        return this.sourcePath;
    }

    @Argument(value = "sourcePath", required = false)
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Credential getCredential() {
        ImmutableBooleanList chosenCredentialModes = BooleanLists.immutable.with(password != null, useKerberosAuth, keytabPath != null)
                .select(BooleanPredicates.isTrue());

        if (deployUserId == null) {
            if (password == null) {
                return null;
            } else {
                throw new IllegalArgumentException("Cannot provide password without deployUserId; provide both or neither (neither will cause a prompt)");
            }
        } else if (chosenCredentialModes.size() > 2) {
            throw new IllegalArgumentException("Cannot provide more than one of password or useKerberosAuth flag or keytabPath; pick one");
        } else if (chosenCredentialModes.size() == 0) {
            throw new IllegalArgumentException("Cannot provide deployUserId without password or useKerberosAuth flag or keytabPath");
        } else if (password != null) {
            return new Credential(deployUserId, password);
        } else if (keytabPath != null) {
            Credential credential = new Credential();
            credential.setUsername(deployUserId);
            credential.setKeytabPath(keytabPath);
            return credential;
        } else {
            Credential credential = new Credential();
            credential.setUsername(deployUserId);
            credential.setUseKerberosAuth(true);
            return credential;
        }
    }

    public String getDeployUserId() {
        return this.deployUserId;
    }

    @Argument(value = "deployUserId", required = false, description = "Kerberos of deployer (for auditing)")
    public void setDeployUserId(String deployUserId) {
        this.deployUserId = deployUserId;
    }

    public File getWorkDir() {
        return this.workDir;
    }

    @Argument(value = "workDir", required = false, description = "only used for unit tests or special cases")
    public void setWorkDir(File workDir) {
        this.workDir = workDir;
    }

    public void validate() {
    }

    public boolean isPerformInitOnly() {
        return this.performInitOnly;
    }

    @Argument(value = "performInitOnly", required = false, description = "performInitOnly")
    public void setPerformInitOnly(boolean performInitOnly) {
        this.performInitOnly = performInitOnly;
    }

    public boolean isPreview() {
        return this.preview;
    }

    @Argument(value = "preview", required = false, description = "Will only preview the changes and not proceed with the actual deployment (only for deployers that support it, e.g. Database)")
    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean isPersistJilJobLoadOrder() {
        return this.persistJilJobLoadOrder;
    }

    @Argument(value = "persistJilJobLoadOrder", required = false, description = "Will persist the job load order defined in the jils")
    public void setPersistJilJobLoadOrder(boolean persistJilJobLoadOrder) {
        this.persistJilJobLoadOrder = persistJilJobLoadOrder;
    }

    public boolean isRollback() {
        return this.rollback;
    }

    @Argument(value = "rollback", required = false, description = "Will carry out the deployment in rollback mode (assumes that you are already pointing to the old location)")
    public void setRollback(boolean rollback) {
        this.rollback = rollback;
    }

    private boolean isActionContains(String command) {
        return this.actions != null && UnifiedSet.newSetWith(this.actions).collect(StringFunctions.toLowerCase()).contains(command);
    }

    public boolean shouldExecuteSetup() {
        return this.setupEnvInfra
                || this.setupEnvInfraOnly
                || isActionContains("setup");
    }

    public boolean shouldExecuteClean() {
        return this.cleanFirst
                || this.cleanOnly
                || isActionContains("clean");
    }

    public boolean shouldExecuteDeploy() {
        return (isActionContains("deploy") || actions == null)
                && !(this.setupEnvInfraOnly || this.cleanOnly);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeployerArgs [");
        if (envNames != null)
            builder.append("envNames=").append(Arrays.toString(envNames)).append(", ");
        if (actions != null)
            builder.append("actions=").append(Arrays.toString(actions)).append(", ");
        builder.append("setupEnvInfra=").append(setupEnvInfra).append(", setupEnvInfraOnly=").append(setupEnvInfraOnly)
                .append(", cleanFirst=").append(cleanFirst).append(", cleanOnly=").append(cleanOnly)
                .append(", noPrompt=").append(noPrompt).append(", useBaseline=").append(useBaseline)
                .append(", onboardingMode=").append(onboardingMode)
                .append(", ");
        if (includeFile != null)
            builder.append("includeFile=").append(includeFile).append(", ");
        builder.append("useKerberosAuth=").append(useKerberosAuth).append(", ");
        if (sourcePath != null)
            builder.append("sourcePath=").append(sourcePath).append(", ");
        if (workDir != null)
            builder.append("workDir=").append(workDir).append(", ");
        if (deployUserId != null)
            builder.append("deployUserId=").append(deployUserId).append(", ");
        builder.append("performInitOnly=").append(performInitOnly).append(", preview=").append(preview)
                .append(", rollback=").append(rollback).append(", persistJilJobLoadOrder=")
                .append(persistJilJobLoadOrder).append(", ");
        builder.append("]");
        return builder.toString();
    }

    public boolean isOnboardingMode() {
        return onboardingMode;
    }

    @Argument(value = "onboardingMode", description = "Enable the mode to help when initially onboarding an existing reverse-engineered schema")
    public void setOnboardingMode(boolean onboardingMode) {
        this.onboardingMode = onboardingMode;
    }

    public String getChangeCriteria() {
        return changeCriteria;
    }

    @Argument(value = "changeCriteria", description = "Criteria string to only deploy a subset of changes; see documentation for more info")
    public void setChangeCriteria(String changeCriteria) {
        this.changeCriteria = changeCriteria;
    }

    public String[] getChangesets() {
        return changesets;
    }

    @Argument(value = "changesets", description = "Changeset names to deploy; defaults to empty, which means to only deploy the default changeset")
    public void setChangesets(String[] changesets) {
        this.changesets = changesets;
    }

    public boolean isAllChangesets() {
        return allChangesets;
    }

    @Argument(value = "allChangesets", description = "Whether to force deployments of all changesets (both default and named changesets); defaults to false (only to deploy the default changeset)")
    public void setAllChangesets(boolean allChangesets) {
        this.allChangesets = allChangesets;
    }

    public String getProductVersion() {
        return productVersion;
    }

    @Argument(value = "productVersion", description = "(Optional, defaults to null) The version number that the user assigns to a product. Meant to help w/ audit trail and special features like rollback detection.")
    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public boolean isLenientSetupEnvInfra() {
        return lenientSetupEnvInfra;
    }

    @Argument(value = "lenientSetupEnvInfra", description = "Whether the environment setup should fail in case of missing groups/users")
    public void setLenientSetupEnvInfra(boolean lenientSetupEnvInfra) {
        this.lenientSetupEnvInfra = lenientSetupEnvInfra;
    }
}
