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
package com.gs.obevo.db.impl.platforms.postgresql

import com.gs.obevo.api.platform.AuditLock
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper
import org.apache.commons.dbutils.handlers.ScalarHandler
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.Duration

class PostgreSqlLock internal constructor(
        private val jdbc: JdbcHelper,
        private val conn: Connection
) : AuditLock {
    private val defaultRetryDelay = Duration.ofSeconds(5L)
    private val lockId = 5749832  // using random integer as the lock ID for the pg advisory lock to prevent collisions with others

    override fun lock() {
        var lockAcquired = false

        while (!lockAcquired) {
            val sql = "SELECT pg_try_advisory_lock($lockId)"
            LOG.info("Attempting to acquire Postgres server lock via {}", sql)
            lockAcquired = jdbc.query(conn, sql, ScalarHandler())

            if (!lockAcquired) {
                LOG.info("Lock not yet available; waiting for {} seconds", defaultRetryDelay.seconds)

                Thread.sleep(defaultRetryDelay.toMillis())
            }
        }
    }

    override fun unlock() {
        val lockReleased = jdbc.query(conn, "SELECT pg_advisory_unlock($lockId)", ScalarHandler<Boolean>())
        LOG.info("Postgres lock has been {} released", if (lockReleased) "successfully" else "unsuccessfully")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PostgreSqlLock::class.java)
    }
}
