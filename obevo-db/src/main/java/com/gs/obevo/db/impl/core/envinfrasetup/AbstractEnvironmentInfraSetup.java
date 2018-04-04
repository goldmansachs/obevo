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

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.DeployMetricsCollector;
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

    public AbstractEnvironmentInfraSetup(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector, DbMetadataManager dbMetadataManager) {
        this.env = env;
        this.jdbc = new JdbcHelper();
        this.ds = ds;
        this.deployMetricsCollector = deployMetricsCollector;
        this.dbMetadataManager = dbMetadataManager;
    }

    @Override
    public final void setupEnvInfra(boolean failOnSetupException, boolean forceCreation) {
        try (Connection conn = ds.getConnection()) {
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                DaCatalog schemaValue = dbMetadataManager.getDatabaseOptional(schema);
                if (schemaValue == null) {
                    if (forceCreation) {
                        LOG.info("Creating schema {}", schema.getPhysicalName());
                        createSchema(conn, schema);
                    } else if (failOnSetupException) {
                        handleException(failOnSetupException, "Failed to create schema " + schema, "schemaCreationFailed");
                    }
                }

                if (env.getPlatform().isSubschemaSupported()) {
                    handleGroups(conn, schema, failOnSetupException, forceCreation);
                    handleUsers(conn, schema, failOnSetupException, forceCreation);
                }
            }

            if (!env.getPlatform().isSubschemaSupported()) {
                handleGroups(conn, null, failOnSetupException, forceCreation);
                handleUsers(conn, null, failOnSetupException, forceCreation);
            }
        } catch (SQLException e) {
            handleException(failOnSetupException, "Failed to open connection", "openConnectionFailed", e);
        }
    }

    private void handleGroups(Connection conn, PhysicalSchema physicalSchema, boolean failOnSetupException, boolean forceCreation) {
        ImmutableSet<String> existingGroupsRaw;
        try {
            existingGroupsRaw = dbMetadataManager.getGroupNamesOptional(physicalSchema);
            if (existingGroupsRaw == null) {
                LOG.debug("Group setup is not supported in this implementation; skipping...");
                return;
            }
        } catch (DataAccessException e) {
            handleException(failOnSetupException, "Group validation query failed", "groupQueryFailed", e);
            return;
        }

        ImmutableSet<String> existingGroups = existingGroupsRaw.collect(String::toLowerCase);

        ImmutableList<Group> missingGroups = env.getGroups().reject(group -> existingGroups.contains(group.getName().toLowerCase()));

        LOG.info("Groups from configuration: {}", env.getGroups());

        if (forceCreation) {
            for (Group missingGroup : missingGroups) {
                LOG.info("Creating missing group {}", missingGroup.getName());
                try {
                    createGroup(conn, missingGroup, physicalSchema);
                } catch (RuntimeException e) {
                    handleException(failOnSetupException,
                            "Failed to create group " + missingGroup,
                            "groupCreationFailure",
                            e
                            );
                }
            }
        } else if (missingGroups.notEmpty()) {
            handleException(failOnSetupException,
                    "The following groups were not found in your environment: " + missingGroups,
                    "groupsInConfigButNotInDb"
                    );
        }
    }

    private void handleUsers(Connection conn, PhysicalSchema physicalSchema, boolean failOnSetupException, boolean forceCreation) {
        ImmutableSet<String> existingUsersRaw = dbMetadataManager.getUserNamesOptional(physicalSchema);
        if (existingUsersRaw == null) {
            LOG.debug("User setup is not supported in this implementation; skipping...");
            return;
        }

        ImmutableSet<String> existingUsers = existingUsersRaw.collect(String::toLowerCase);

        ImmutableList<User> missingUsers = env.getUsers().reject(user -> existingUsers.contains(user.getName().toLowerCase()));

        if (forceCreation) {
            for (User missingUser : missingUsers) {
                LOG.info("Creating user {}", missingUser.getName());
                try {
                    createUser(conn, missingUser, physicalSchema);
                } catch (RuntimeException e) {
                    handleException(failOnSetupException,
                            "Failed to create user " + missingUser,
                            "userCreationFailure",
                            e
                    );
                }
            }
        } else if (missingUsers.notEmpty()) {
            handleException(failOnSetupException,
                    "The following users were not found in your environment: " + missingUsers,
                    "usersInConfigButNotInDb"
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
            LOG.warn(errorMessage);
            LOG.warn("Will proceed with deployment as you have configured this to just be a warning");
            deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".envSetup." + metricsMessage, errorMessage);
        }
    }

    protected void createSchema(Connection conn, PhysicalSchema schema) {
        LOG.info("Schema creation is not supported; skipping this step");
    }

    protected void createGroup(Connection conn, Group group, PhysicalSchema physicalSchema) {
        LOG.info("Group creation is not supported; skipping this step");
    }

    protected void createUser(Connection conn, User user, PhysicalSchema physicalSchema) {
        LOG.info("User creation is not supported; skipping this step");
    }
}
