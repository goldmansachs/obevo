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
package com.gs.obevo.db.impl.core.envinfrasetup;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.ServerDirectory;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.api.platform.DbChangeTypeBehavior;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaDirectory;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setup mode:
 * - do setups and fail
 * - do setups and don't fail if error
 * - only check and fail if missing
 * - onoly check and don't fail if missing
 */
public class AbstractEnvironmentInfraSetup implements EnvironmentInfraSetup<DbEnvironment> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractEnvironmentInfraSetup.class);
    protected final DbEnvironment env;
    private final DeployMetricsCollector deployMetricsCollector;
    protected final JdbcHelper jdbc;
    protected final DataSource ds;
    protected final DbMetadataManager dbMetadataManager;
    private final ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry;

    public AbstractEnvironmentInfraSetup(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector, DbMetadataManager dbMetadataManager, ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry) {
        this.env = Objects.requireNonNull(env);
        this.jdbc = new JdbcHelper();
        this.ds = Objects.requireNonNull(ds);
        this.deployMetricsCollector = Objects.requireNonNull(deployMetricsCollector);
        this.dbMetadataManager = Objects.requireNonNull(dbMetadataManager);
        this.changeTypeBehaviorRegistry = Objects.requireNonNull(changeTypeBehaviorRegistry);
    }

    @Override
    public final void setupEnvInfra(boolean failOnSetupException, boolean forceCreation) {
        try (Connection conn = ds.getConnection()) {
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                DaCatalog schemaValue = dbMetadataManager.getDatabaseOptional(schema);
                if (schemaValue == null) {
                    if (forceCreation) {
                        LOG.info("Creating schema/database {}", schema.getPhysicalName());
                        createSchema(conn, schema);
                    } else if (failOnSetupException) {
                        handleException(failOnSetupException, "Failed to create schema " + schema, "schemaCreationFailed");
                    }
                }
            }

            if (env.getPlatform().isSubschemaSupported()) {
                for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                    handleGroups(conn, schema, failOnSetupException, forceCreation);
                    handleUsers(conn, schema, failOnSetupException, forceCreation);
                }
            }

            if (!env.getPlatform().isSubschemaSupported()) {
                handleGroups(conn, null, failOnSetupException, forceCreation);
                handleUsers(conn, null, failOnSetupException, forceCreation);

                handleDirectories(conn, failOnSetupException, forceCreation);
            }
        } catch (SQLException e) {
            handleException(failOnSetupException, "Failed to open connection", "openConnectionFailed", e);
        }
    }

    private void handleGroups(Connection conn, PhysicalSchema physicalSchema, boolean failOnSetupException, boolean forceCreation) {
        handleObject(conn, physicalSchema, failOnSetupException, forceCreation,
                "group", schema -> dbMetadataManager.getGroupNamesOptional(schema),
                name -> name,
                DbEnvironment::getGroups, Group::getName,
                this::createGroup
        );
    }

    private void handleUsers(Connection conn, PhysicalSchema physicalSchema, boolean failOnSetupException, boolean forceCreation) {
        handleObject(conn, physicalSchema, failOnSetupException, forceCreation,
                "user", schema -> dbMetadataManager.getUserNamesOptional(schema),
                name -> name,
                DbEnvironment::getUsers, User::getName,
                this::createUser
        );
    }

    private void handleDirectories(Connection conn, boolean failOnSetupException, boolean forceCreation) {
        String changeTypeName = ChangeType.DIRECTORY;

        handleObject(conn, null, failOnSetupException, forceCreation,
                changeTypeName, schema -> dbMetadataManager.getDirectoriesOptional(),
                DaDirectory::getName,
                DbEnvironment::getServerDirectories, ServerDirectory::getName,
                this::createDirectory
        );

        DbChangeTypeBehavior tableChangeType = (DbChangeTypeBehavior) changeTypeBehaviorRegistry.getChangeTypeBehavior(changeTypeName);

        for (ServerDirectory serverDirectory : env.getServerDirectories()) {
            tableChangeType.applyGrants(conn, null, serverDirectory.getName(), env.getPermissions(changeTypeName));
        }
    }

    /**
     * General pattern for creating DB objects during infrastructure setup.
     */
    private <SRCOBJ, DBOBJ> void handleObject(Connection conn, PhysicalSchema physicalSchema, boolean failOnSetupException, boolean forceCreation, String objectTypeName, Function<PhysicalSchema, RichIterable<DBOBJ>> getDbObjects, Function<DBOBJ, String> getDbObjectName, Function<DbEnvironment, ImmutableList<SRCOBJ>> getSourceObjects, Function<SRCOBJ, String> getSourceObjectName, CreateObject<SRCOBJ> createObject) {
        RichIterable<DBOBJ> existingObjects;
        try {
            existingObjects = getDbObjects.valueOf(physicalSchema);
            if (existingObjects == null) {
                LOG.debug("{} setup is not supported in this implementation; skipping...", objectTypeName);
                return;
            }
        } catch (DataAccessException e) {
            handleException(failOnSetupException, objectTypeName + " validation query failed", objectTypeName + "QueryFailed", e);
            return;
        }

        LOG.debug("{} objects existing in DB: {}", objectTypeName, existingObjects);

        ImmutableSet<String> existingObjectNames = existingObjects.collect(getDbObjectName).collect(String::toLowerCase).toSet().toImmutable();

        ImmutableList<SRCOBJ> sourceObjects = getSourceObjects.valueOf(env);
        LOG.debug("{} objects from configuration: {}", objectTypeName, sourceObjects);

        ImmutableList<SRCOBJ> missingObjects = sourceObjects.reject(object -> existingObjectNames.contains(getSourceObjectName.valueOf(object).toLowerCase()));

        if (forceCreation) {
            for (SRCOBJ missingObject : missingObjects) {
                LOG.info("Creating missing {} {}", objectTypeName, missingObject);
                try {
                    createObject.create(conn, missingObject, physicalSchema);
                } catch (RuntimeException e) {
                    handleException(failOnSetupException,
                            "Failed to create " + objectTypeName + " " + missingObject,
                            objectTypeName + "CreationFailure",
                            e
                    );
                }
            }
        } else if (missingObjects.notEmpty()) {
            handleException(failOnSetupException,
                    "The following " + objectTypeName + " objects were not found in your environment: " + missingObjects,
                    objectTypeName + "InConfigButNotInDb"
            );
        }
    }

    private void handleException(boolean failOnSetupException, String errorMessage, String metricsMessage) {
        handleException(failOnSetupException, errorMessage, metricsMessage, null);
    }

    private void handleException(boolean failOnSetupException, String errorMessage, String metricsMessage, Exception e) {
        if (failOnSetupException) {
            throw new IllegalArgumentException(errorMessage, e);
        } else {
            LOG.warn(errorMessage + ": {}", e.getMessage());
            LOG.warn("Will proceed with deployment as you have configured this to just be a warning");
            deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".envSetup." + metricsMessage, errorMessage);
        }
    }

    protected void createSchema(Connection conn, PhysicalSchema schema) {
        LOG.info("Schema creation is not supported; skipping this step");
    }

    private interface CreateObject<SourceObject> {
        void create(Connection conn, SourceObject group, PhysicalSchema physicalSchema);
    }

    protected void createGroup(Connection conn, Group group, PhysicalSchema physicalSchema) {
        LOG.info("Group creation is not supported; skipping this step");
    }

    protected void createUser(Connection conn, User user, PhysicalSchema physicalSchema) {
        LOG.info("User creation is not supported; skipping this step");
    }

    protected void createDirectory(Connection conn, ServerDirectory directory, PhysicalSchema physicalSchema) {
        LOG.info("Directory creation is not supported; skipping this step");
    }
}
