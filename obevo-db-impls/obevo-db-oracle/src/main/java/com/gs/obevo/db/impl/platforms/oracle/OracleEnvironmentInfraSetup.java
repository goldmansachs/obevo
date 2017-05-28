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
