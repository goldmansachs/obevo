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
package com.gs.obevo.db.unittest;

import java.io.File;
import java.util.Set;

import com.gs.obevo.api.appdata.ChangeKey;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.impl.changepredicate.ChangeKeyPredicateBuilder;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;

/**
 * Helper class for building unit test db environments and contexts, i.e. to not have to remember details for each unit
 * test db type and to easily switch between unit test DBMS's if needed
 *
 * Ultimately, this will delegate to Environment.getAppContextBuilder() for building the context. This class will mainly
 * leverage a client's existing DbEnvironment setup and tweak values accordingly
 *
 * If an existing environment is referenced, a copy of that DbEnvironment will be made by this class so that any changes
 * to that object will not impact anything existing
 */
public class UnitTestDbBuilder {
    public static UnitTestDbBuilder newBuilder() {
        return new UnitTestDbBuilder();
    }

    /**
     * Caching the contexts created so that subsequent operations on this can go quickly (i.e. avoiding the costs of
     * reading the contents from the file system).
     */
    private static final MutableMap<String, DbDeployerAppContext> cachedContexts = Maps.mutable.empty();

    private String sourcePath;
    private String envName;
    private String referenceEnvName;
    private DbPlatform dbPlatform;
    private String dbServer = "test";
    private ImmutableSet<String> tables;
    private ImmutableSet<String> views;
    private boolean persistToFile = false;
    private boolean grantsDisabled = true;  // disabling grants by default for unit tests as in practice, most teams
    private File workDir = new File("./target/unitdb");
    private Credential credential = new Credential("sa", "");

    /**
     * (Required) path to read environment metadata from
     */
    public UnitTestDbBuilder setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    /**
     * Name to give the environment created during this builder
     *
     * If not specified, will use the name and environment specified by the referenceEnvName parameter
     */
    public UnitTestDbBuilder setEnvName(String envName) {
        this.envName = envName;
        return this;
    }

    /**
     * Environment to use as a model for the unit test db environment to create
     *
     * If not specified, then one of the environments read from the source path will be picked arbitrarily. The envName
     * parameter must then be specified to give this unit test db environment a known name
     */
    public UnitTestDbBuilder setReferenceEnvName(String referenceEnvName) {
        this.referenceEnvName = referenceEnvName;
        return this;
    }

    /**
     * dbPlatform to use for the new unit test db
     *
     * If not specified, will default to the dbPlatform of the reference environment
     */
    public UnitTestDbBuilder setDbPlatform(DbPlatform dbPlatform) {
        this.dbPlatform = dbPlatform;
        return this;
    }

    /**
     * Name of the in-memory db to be created to be used when creating the URL, e.g. jdbc:h2:mem:&lt;dbServer&gt;:etc.
     * This is a convenience for clients to not have to remember the URL convention. The resolved URL can be retrieved
     * from the DbEnvironment instance
     *
     * If not specified, will default to "test"
     */
    public UnitTestDbBuilder setDbServer(String dbServer) {
        this.dbServer = dbServer;
        return this;
    }

    /**
     * Specify this to restrict the tables deployed to just the ones provided. If not specified, all tables will be deployed.
     *
     * @deprecated Specify these fields to remove in the {@link DbDeployerAppContext#deploy(MainDeployerArgs)} arguments itself.
     */
    @Deprecated
    public UnitTestDbBuilder setTables(Set<String> tables) {
        this.tables = Sets.immutable.withAll(tables);
        return this;
    }

    /**
     * Specify this to restrict the views deployed to just the ones provided. If not specified, all views will be deployed.
     *
     * @deprecated Specify these fields to remove in the {@link DbDeployerAppContext#deploy(MainDeployerArgs)} arguments itself.
     */
    @Deprecated
    public UnitTestDbBuilder setViews(Set<String> views) {
        this.views = Sets.immutable.withAll(views);
        return this;
    }

    /**
     * If true, will generate the file-persistent URL for the in-memory database. If false, goes w/ the in-memory URL
     *
     * Defaults to false (in-memory)
     */
    public UnitTestDbBuilder setPersistToFile(boolean persistToFile) {
        this.persistToFile = persistToFile;
        return this;
    }

    private boolean isPersistToFile() {
        // We support this system property here to facilitate easy debugging for
        // unit tests (i.e. if we don't want to change the code accidentally when running/debugging)
        // But we can still set this programatically anyway
        if (System.getProperty("debugDbUnit") == null) {
            return this.persistToFile;
        } else {
            return "true".equalsIgnoreCase(System.getProperty("debugDbUnit"));
        }
    }

    /**
     * @deprecated Clients should move off this. Leverage the groups and users sections in your DbEnvironment configuration to have the grants created
     */
    @Deprecated
    public UnitTestDbBuilder setGrantsDisabled(boolean grantsDisabled) {
        this.grantsDisabled = grantsDisabled;
        return this;
    }

    /**
     * The workDir to use for any temporary files used for the db deployment
     *
     * If not specified, defaults to "./target/unitdb" (i.e. relative to your working directory)
     */
    public UnitTestDbBuilder setWorkDir(File workDir) {
        this.workDir = workDir;
        return this;
    }

    /**
     * (optional) credential defaults to username==sa and password==&lt;blank&gt;, per the default convention of h2/hsql
     */
    public UnitTestDbBuilder setCredential(Credential credential) {
        this.credential = credential;
        return this;
    }

    private void validateBuilder() {
        if (envName == null && referenceEnvName == null) {
            throw new IllegalArgumentException("One of envName or referenceEnvName must be populated");
        }
        Validate.notNull(sourcePath);
        Validate.notNull(credential);
    }

    public DbDeployerAppContext buildContext() {
        validateBuilder();

        String instanceLookupKey = instanceLookupKey();

        DbDeployerAppContext baseContext = cachedContexts.get(instanceLookupKey);

        if (baseContext == null) {
            baseContext = buildContextUncached();
            cachedContexts.put(instanceLookupKey, baseContext);
        }

        // set the arguments that should be used as defined in this builder class, e.g. for limiting by specific tables
        return new UnitTestDbDeployerAppContext(baseContext, getMainDeployerArgs());
    }

    /**
     * Builds the deployer context. See the class Javadoc for more information
     */
    private DbDeployerAppContext buildContextUncached() {
        validateBuilder();

        String[] envsToRequest = this.referenceEnvName != null ? new String[] { this.referenceEnvName } : new String[0];
        DbEnvironment referenceEnv = DbEnvironmentFactory.getInstance().readFromSourcePath(this.sourcePath, envsToRequest)
                .getFirst();

        DbEnvironment env = referenceEnv.createCopy();

        if (this.envName != null) {
            env.setName(this.envName);
        }

        env.setDisableAuditTracking(true);
        env.setPersistToFile(this.isPersistToFile());

        if (this.dbPlatform != null) {
            env.setPlatform(this.dbPlatform);
        }

        if (this.dbServer != null) {
            env.setDbServer(this.dbServer);
        }

        if (this.grantsDisabled) {
            env.setPermissions(Lists.immutable.<Permission>empty());
        }

        env.setDefaultUserId(credential.getUsername());
        env.setDefaultPassword(credential.getPassword());

        return env.getAppContextBuilder()
                .setWorkDir(workDir)
                .build();
    }

    private MainDeployerArgs getMainDeployerArgs() {
        MainDeployerArgs args = new MainDeployerArgs();

        if (this.tables != null || this.views != null) {
            MutableList<Predicate<? super ChangeKey>> predicates = Lists.mutable.empty();
            if (this.tables != null) {
                predicates.add(ChangeKeyPredicateBuilder.newBuilder()
                        .setChangeTypes(ChangeType.TABLE_STR, ChangeType.FOREIGN_KEY_STR, ChangeType.TRIGGER_INCREMENTAL_OLD_STR, ChangeType.STATICDATA_STR)
                        .setObjectNames(Sets.immutable.withAll(this.tables))
                        .build());
            }
            if (this.views != null) {
                predicates.add(ChangeKeyPredicateBuilder.newBuilder()
                        .setChangeTypes(ChangeType.VIEW_STR)
                        .setObjectNames(Sets.immutable.withAll(this.views))
                        .build());
            }

            args.setChangeInclusionPredicate(Predicates.or(predicates));
        }

        args.setAllChangesets(true);  // for unit tests, we always want all changes to deploy
        return args;
    }

    private String instanceLookupKey() {
        String envNameToLookup = envName != null ? envName : referenceEnvName;

        return this.sourcePath + ":" + envNameToLookup;
    }
}
