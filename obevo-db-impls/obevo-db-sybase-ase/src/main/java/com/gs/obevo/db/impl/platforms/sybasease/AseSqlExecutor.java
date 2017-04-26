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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.impl.core.jdbc.DefaultJdbcHandler;
import com.gs.obevo.db.impl.core.jdbc.JdbcHandler;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.db.impl.platforms.AbstractSqlExecutor;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AseSqlExecutor extends AbstractSqlExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(AseSqlExecutor.class);
    // this has state; hence, the instance must be reused
    private final AseJdbcHandler jdbcHandler = new AseJdbcHandler();

    public AseSqlExecutor(DataSource ds) {
        super(ds);
    }

    @Override
    public void setDataSourceSchema(Connection conn, PhysicalSchema schema) {
        JdbcHelper jdbc = this.getJdbcTemplate();
        jdbc.update(conn, "use " + schema.getPhysicalName());
    }

    @Override
    protected JdbcHandler getJdbcHandler() {
        return this.jdbcHandler;
    }

    public static class AseJdbcHandler extends DefaultJdbcHandler {
        private static final int stopLogSpaceThreshold = 85;
        private static final int resumeLogSpaceThreshold = 40;
        private static final int maxLogCounter = 10;
        private AtomicInteger curLogCounter = new AtomicInteger(0);

        @Override
        public void preUpdate(Connection conn, JdbcHelper jdbc) {
            this.waitForLogSpace(conn, jdbc);
        }

        /**
         * Adding this wait as the Sybase ASE logs can fill up quickly if you execute a lot of DDLs
         * Hence, we put in a periodic check (currently going by every "maxLogCounter" updates executed)
         * to see if the log level exceeds a "stopLogSpaceThreshold". If so, we wait till it gets back
         * down to a "resumeLogSpaceThreshold"
         */
        private void waitForLogSpace(Connection conn, JdbcHelper jdbc) {
            this.curLogCounter.incrementAndGet();

            // only trigger the check every "maxLogCounter" checks
            if (this.curLogCounter.get() == maxLogCounter) {
                boolean firstTime = true;

                while (true) {
                    int percentFull = this.getPercentLogFullInDb(conn, jdbc);

                    int thresholdToCheck = firstTime ? stopLogSpaceThreshold : resumeLogSpaceThreshold;
                    firstTime = false;

                    if (percentFull < thresholdToCheck) {
                        break;
                    } else {
                        try {
                            Seconds seconds = Seconds.seconds(3);
                            LOG.info(String
                                    .format("Pausing for %d seconds as the log level hit a high mark of %d; will resume when it gets back to %d",
                                            seconds.getSeconds(), percentFull, resumeLogSpaceThreshold));
                            Thread.sleep(seconds.getSeconds() * 1000);
                        } catch (InterruptedException e) {
                            throw new DeployerRuntimeException(e);
                        }
                    }
                }

                this.curLogCounter.set(0);  // reset the log counter after doing the check
            } else if (this.curLogCounter.get() > maxLogCounter) {
                // in this case, some concurrent execution caused the ID to exceed the maxLogCounter. In this case, just
                // reset the counter to 0 (the thread that has the counter at the right value would execute this code
                this.curLogCounter.set(0);
            }
        }

        private int getPercentLogFullInDb(Connection conn, JdbcHelper jdbc) {
            Map<String, Object> logCheckResults = jdbc.queryForMap(conn, "exec sp_xlogfull");

            if (logCheckResults.get("%Full") == null) {
                throw new IllegalArgumentException(
                        "Did not get back the right results in the sp_xlogfull call; expecting %Full as a column, got back: "
                                + logCheckResults);
            }
            return ((Integer) logCheckResults.get("%Full")).intValue();
        }
    }
}
