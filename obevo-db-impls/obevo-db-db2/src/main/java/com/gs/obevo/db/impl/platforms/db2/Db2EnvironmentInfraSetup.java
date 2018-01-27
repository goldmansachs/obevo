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
package com.gs.obevo.db.impl.platforms.db2;

import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that the groups specified in the system configuration actually exist in the database.
 *
 * Note that user verification cannot really be done as DB2 does not define it. http://www.dbforums.com/showthread.php?1645929-Finding-Users-and-Groups
 *
 * For now, we rely on an approximation based on the sysibm.SYSROLES and sysibm.SYSDBAUTH tables.
 */
public class Db2EnvironmentInfraSetup implements EnvironmentInfraSetup {
    private static final Logger LOG = LoggerFactory.getLogger(Db2EnvironmentInfraSetup.class);

    private final DbEnvironment env;
    private final JdbcHelper jdbc;
    private final DeployMetricsCollector deployMetricsCollector;
    private final DataSource ds;

    public Db2EnvironmentInfraSetup(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector) {
        this.env = env;
        this.jdbc = new JdbcHelper();
        this.ds = ds;
        this.deployMetricsCollector = deployMetricsCollector;
    }

    @Override
    public void setupEnvInfra(boolean failOnSetupException) {
        LOG.info("Verifying existence of DB2 groups prior to deployment");

        ImmutableSet<String> existingGroups;
        Connection conn = null;
        try {
            conn = ds.getConnection();
            existingGroups = Sets.immutable
                    .withAll(jdbc.query(conn, "select ROLENAME from sysibm.SYSROLES", new ColumnListHandler<String>()))
                    .newWithAll(jdbc.query(conn, "select GRANTEE from sysibm.SYSDBAUTH", new ColumnListHandler<String>()))
                    .collect(StringFunctions.trim());  // db2 sometimes has whitespace in its return results that needs trimming
        } catch (Exception e) {
            if (failOnSetupException) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            } else {
                LOG.warn("Group validation query failed; continuing w/ deployment per configuration", e);
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".db2GroupValidationQueryFailure", true);
                return;
            }
        } finally {
            DbUtils.closeQuietly(conn);
        }

        LOG.info("Groups from DB: {}", existingGroups);

        ImmutableList<String> groupNames = env.getGroups().collect(Group.TO_NAME);
        LOG.info("Groups from system-config: {}", groupNames);

        // Do difference comparison in a case insensitive manner (convert all to lowercase)
        ImmutableList<String> missingGroups = groupNames.select(Predicates.attributeNotIn(StringFunctions.toLowerCase(), existingGroups.collect(StringFunctions.toLowerCase())));

        if (missingGroups.notEmpty()) {
            String errorMessage = "The following groups were not found in your DB2 server (checked against sysibm.SYSROLES and sysibm.SYSDBAUTH): " + missingGroups;
            if (failOnSetupException) {
                throw new IllegalArgumentException(errorMessage);
            } else {
                LOG.warn(errorMessage);
                LOG.warn("Will proceed with deployment as you have configured this to just be a warning");
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".db2GroupsInConfigButNotInDb", errorMessage);
            }
        }
    }
}
