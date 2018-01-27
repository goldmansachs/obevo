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
package com.gs.obevo.db.impl.platforms.oracle;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployMetrics;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.core.envinfrasetup.EnvironmentInfraSetup;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.impl.DeployMetricsCollector;
import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleEnvironmentInfraSetup implements EnvironmentInfraSetup {
    private static final Logger LOG = LoggerFactory.getLogger(OracleEnvironmentInfraSetup.class);
    private final DbEnvironment env;
    private final DeployMetricsCollector deployMetricsCollector;
    private final JdbcHelper jdbc;
    private final DataSource ds;

    public OracleEnvironmentInfraSetup(DbEnvironment env, DataSource ds, DeployMetricsCollector deployMetricsCollector) {
        this.env = env;
        this.jdbc = new JdbcHelper();
        this.ds = ds;
        this.deployMetricsCollector = deployMetricsCollector;
    }

    @Override
    public void setupEnvInfra(boolean failOnSetupException) {
        // will reenable this functionality at another time
        if (true) {
            return;
        }
        Connection conn = null;
        try {
            conn = ds.getConnection();
            for (PhysicalSchema schema : env.getPhysicalSchemas()) {
                createSchema(conn, schema);
            }
        } catch (SQLException e) {
            if (failOnSetupException) {
                throw new DataAccessException(e);
            } else {
                LOG.warn("Env Infra Setup connection failed", e);
                deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".envInfraSetupConnectionFailure.oracle", true);
                return;
            }
        } finally {
            DbUtils.closeQuietly(conn);
        }
    }

    private void createSchema(Connection conn, PhysicalSchema schema) {
        LOG.info("Creating schema {}", schema);
        try {
            jdbc.update(conn, "CREATE USER " + schema.getPhysicalName() + " IDENTIFIED BY schemaPassw0rd QUOTA UNLIMITED ON USERS");
        } catch (Exception e) {
            LOG.debug("Ignoring for now until we find better way to check users: {}", e.getMessage());
        }
        try {
            jdbc.update(conn, "ALTER USER " + schema.getPhysicalName() + " QUOTA UNLIMITED ON USERS");
        } catch (Exception e) {
            LOG.debug("Ignoring for now until we find better way to check users: {}", e.getMessage());
        }
    }
}
