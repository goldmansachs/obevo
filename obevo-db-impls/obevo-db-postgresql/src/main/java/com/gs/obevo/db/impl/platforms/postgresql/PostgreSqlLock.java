package com.gs.obevo.db.impl.platforms.postgresql;

import java.sql.Connection;

import com.gs.obevo.api.platform.AuditLock;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgreSqlLock implements AuditLock {
    private static final Logger LOG = LoggerFactory.getLogger(PostgreSqlLock.class);
    private JdbcHelper jdbc;
    private final Connection conn;

    PostgreSqlLock(JdbcHelper jdbc, Connection conn) {
        this.jdbc = jdbc;
        this.conn = conn;
    }

    @Override
    public void lock() {
        LOG.info("SHANT LOCK START");
        boolean lockAcquired = false;
        while (!lockAcquired) {
            lockAcquired = jdbc.query(conn, "SELECT pg_try_advisory_lock(123)", new ScalarHandler<Boolean>());
            if (!lockAcquired) {
                LOG.info("SHANT LOCK NOT YET");

                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        LOG.info("SHANT LOCK " + lockAcquired);
    }

    @Override
    public void unlock() {
        LOG.info("SHANT UNLOCK START");
        Boolean query = jdbc.query(conn, "SELECT pg_advisory_unlock(123)", new ScalarHandler<Boolean>());
        LOG.info("SHANT UNLOCK " + query);
    }
}
