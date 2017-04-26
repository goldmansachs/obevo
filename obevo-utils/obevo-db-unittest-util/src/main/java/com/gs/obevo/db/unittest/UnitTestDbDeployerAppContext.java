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
package com.gs.obevo.db.unittest;

import java.io.File;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.checksum.DbChecksumDao;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Context that takes in a {@link MainDeployerArgs} instance and uses it as the default arguments to pass to the
 * deploy command. This is to let us reuse some immutable contexts while modifying the args, notably on limiting the
 * tables/views read in.
 *
 * Most methods will just delegate to the incoming DbDeployerAppContext; a few are explicitly defined.
 * @deprecated Should be removed once clients move off setTables and setViews in UnitTestDbBuilder
 */
@Deprecated
class UnitTestDbDeployerAppContext implements DbDeployerAppContext {
    private final DbDeployerAppContext delegate;
    private final MainDeployerArgs defaultArgs;

    public UnitTestDbDeployerAppContext(DbDeployerAppContext delegate, MainDeployerArgs defaultArgs) {
        this.delegate = delegate;
        this.defaultArgs = defaultArgs;
    }

    /**
     * non-delegate method; calls deploy w/ the passed-in args.
     */
    @Override
    public DbDeployerAppContext deploy() {
        return delegate.deploy(defaultArgs);
    }

    /**
     * non-delegate method; must refer to the deploy method in this class.
     */
    @Override
    public DbDeployerAppContext cleanAndDeploy() {
        this.cleanEnvironment();
        this.deploy();
        return this;
    }

    /**
     * non-delegate method; must refer to the deploy method in this class.
     */
    @Override
    public DbDeployerAppContext setupAndCleanAndDeploy() {
        this.setupEnvInfra();
        this.cleanAndDeploy();
        return this;
    }

    @Override
    public DbEnvironment getEnvironment() {
        return delegate.getEnvironment();
    }

    @Override
    public File getWorkDir() {
        return delegate.getWorkDir();
    }

    @Override
    public DbDeployerAppContext buildDbContext() {
        return delegate.buildDbContext();
    }

    @Override
    public DbDeployerAppContext buildFileContext() {
        return delegate.buildFileContext();
    }

    @Override
    public ImmutableList<Change> readChangesFromAudit() {
        return delegate.readChangesFromAudit();
    }

    @Override
    public ImmutableList<Change> readChangesFromSource() {
        return delegate.readChangesFromSource();
    }

    @Override
    public ImmutableList<Change> readChangesFromSource(boolean useBaseline) {
        return delegate.readChangesFromSource(useBaseline);
    }

    @Override
    public DbMetadataManager getDbMetadataManager() {
        return delegate.getDbMetadataManager();
    }

    @Override
    public SqlExecutor getSqlExecutor() {
        return delegate.getSqlExecutor();
    }

    @Override
    public DataSource getDataSource() {
        return delegate.getDataSource();
    }

    @Override
    public DbDeployerAppContext setupEnvInfra() {
        return delegate.setupEnvInfra();
    }

    @Override
    public DbDeployerAppContext cleanEnvironment() {
        return delegate.cleanEnvironment();
    }

    @Override
    public DbDeployerAppContext setEnvironment(DbEnvironment env) {
        return delegate.setEnvironment(env);
    }

    @Override
    public DbDeployerAppContext setCredential(Credential credential) {
        return delegate.setCredential(credential);
    }

    @Override
    public DbDeployerAppContext setWorkDir(File workDir) {
        return delegate.setWorkDir(workDir);
    }

    @Override
    public DbDeployerAppContext build() {
        return delegate.build();
    }

    @Override
    public DbDeployerAppContext deploy(MainDeployerArgs deployerArgs) {
        return delegate.deploy(deployerArgs);
    }

    @Override
    public void readSource(MainDeployerArgs deployerArgs) {
        delegate.readSource(deployerArgs);
    }

    @Override
    public DeployExecutionDao getDeployExecutionDao() {
        return delegate.getDeployExecutionDao();
    }

    @Override
    public DbChecksumDao getDbChecksumDao() {
        return delegate.getDbChecksumDao();
    }

    @Override
    public DeployMetrics getDeployMetrics() {
        return delegate.getDeployMetrics();
    }

    @Override
    public DbDeployerAppContext setFailOnSetupException(boolean failOnSetupException) {
        return delegate.setFailOnSetupException(failOnSetupException);
    }

    @Override
    public DbDeployerAppContext setupEnvInfra(boolean failOnSetupException) {
        return delegate.setupEnvInfra(failOnSetupException);
    }
}
