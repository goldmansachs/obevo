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
package com.gs.obevo.db.impl.platforms.sybasease;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setup the ASE schema group and users. Groups are created if they do not already exist
 *
 *
 * See this link for more info on the system metadata tables:
 * - http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc36274.1600/doc/html/san1393052489790.html
 */
public class AseEnvironmentInfraSetup implements EnvironmentInfraSetup {
    private static final Logger LOG = LoggerFactory.getLogger(AseEnvironmentInfraSetup.class);
    private final DbEnvironment env;
    private final DeployMetricsCollector deployMetricsCollector;
    private final JdbcHelper jdbc;
    private final DataSource ds;

    public AseEnvironmentInfraSetup(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector) {
        this.env = env;
        this.jdbc = new JdbcHelper();
        this.ds = ds;
        this.deployMetricsCollector = deployMetricsCollector;
    }

    @Override
    public void setupEnvInfra(boolean failOnSetupException) {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                LOG.info("Verifying existence of Sybase groups and users in database {} prior to deployment", schema);
                setupGroups(conn, schema, failOnSetupException);
                setupUsers(conn, schema, failOnSetupException);
            }
        } catch (SQLException e) {
            if (failOnSetupException) {
                throw new DataAccessException(e);
            } else {
                LOG.warn("Env Infra Setup connection failed", e);
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".aseEnvInfraSetupConnectionFailure", true);
                return;
            }
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private void setupGroups(Connection conn, PhysicalSchema schema, boolean failOnSetupException) {
        MutableSet<String> existingGroups;
        try {
            existingGroups = ListAdapter.adapt(jdbc.query(conn, schema.getPhysicalName() + "..sp_helpgroup", new ColumnListHandler<String>("Group_name"))).toSet();
        } catch (DataAccessException e) {
            if (failOnSetupException) {
                throw e;
            } else {
                LOG.warn("Group validation query failed; continuing w/ deployment per configuration", e);
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".aseGroupValidationQueryFailure", true);
                return;
            }
        }

        ImmutableList<Group> missingGroups = env.getGroups().select(Predicates.attributeNotIn(Group.TO_NAME, existingGroups));
        MutableList<String> failedGroups = Lists.mutable.empty();
        for (Group group : missingGroups) {
            LOG.info("Group " + group.getName() + " doesn't exist in database " + schema.getPhysicalName() + "; creating it now");
            try {
                jdbc.update(conn, schema.getPhysicalName() + "..sp_addgroup " + group.getName());
            } catch (DataAccessException sqlExc) {
                if (failOnSetupException) {
                    throw new DeployerRuntimeException("Failed to create group " + group.getName() + " in database " + schema.getPhysicalName() + " during setup; exiting the deploy", sqlExc);
                } else {
                    LOG.warn("Group creation failed for group {} in database {}; continuing w/ deployment per configuration", group, schema, sqlExc);
                    failedGroups.add(group.getName());
                }
            }
        }

        if (failedGroups.notEmpty()) {
            deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".aseGroupCreationFailure", "Failed creating groups " + failedGroups + " in database " + schema);
        }
    }

    private void setupUsers(Connection conn, PhysicalSchema schema, boolean failOnSetupException) {
        MutableSet<String> existingUsers;
        try {
            existingUsers = ListAdapter.adapt(jdbc.query(conn, schema.getPhysicalName() + "..sp_helpuser", new ColumnListHandler<String>("Users_name"))).toSet();
        } catch (DataAccessException e) {
            if (failOnSetupException) {
                throw e;
            } else {
                LOG.warn("User validation query failed; continuing w/ deployment per configuration", e);
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".aseUserValidationQueryFailure", true);
                return;
            }
        }

        ImmutableList<User> missingUsers = env.getUsers().select(Predicates.attributeNotIn(User.TO_NAME, existingUsers));
        if (missingUsers.notEmpty()) {
            String errorMessage = "Specified users " + missingUsers.collect(User.TO_NAME).makeString("[", ",", "]") + " do not exist in database " + schema.getPhysicalName() + "; please create the users or remove from your configuration (or rely on groups instead for permissions)";
            if (failOnSetupException) {
                throw new IllegalArgumentException(errorMessage);
            } else {
                LOG.warn(errorMessage);
                LOG.warn("Will proceed with deployment as you have configured this to just be a warning");
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".usersInConfigButNotInDb", errorMessage);
            }
        }
    }
}
