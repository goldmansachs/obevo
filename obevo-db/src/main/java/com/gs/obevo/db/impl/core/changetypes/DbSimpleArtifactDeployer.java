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
package com.gs.obevo.db.impl.core.changetypes;

import java.sql.Connection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.jdbc.DataAccessException;
import com.gs.obevo.db.impl.core.util.MultiLineStringSplitter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbSimpleArtifactDeployer {
    private static final Logger LOG = LoggerFactory.getLogger(DbSimpleArtifactDeployer.class);

    private final DbPlatform dialect;
    private final SqlExecutor sqlExecutor;

    public DbSimpleArtifactDeployer(DbPlatform dialect, SqlExecutor sqlExecutor) {
        this.dialect = dialect;
        this.sqlExecutor = sqlExecutor;
    }

    public void deployArtifact(Connection conn, Change artifact) {
        MutableList<String> sqls = MultiLineStringSplitter.createSplitterOnSpaceAndLine("GO").valueOf(artifact.getConvertedContent());
        int index = 0;
        for (String sql : sqls) {
            index++;
            if (StringUtils.isBlank(sql)) {
                LOG.debug("Skipping blank sql");
            } else {
                LOG.debug("Executing change #{} in the artifact", index);

                try {
                    dialect.doTryBlockForArtifact(conn, this.sqlExecutor, artifact);
                    this.sqlExecutor.getJdbcTemplate().update(conn, sql);
                } catch (DataAccessException e) {
                    throw new DeployerRuntimeException("Could not execute DDL:\nfor artifact [[["
                            + artifact.getDisplayString() + "]]] from file [[[" + artifact.getFileLocation()
                            + "]]] while executing SQL: [[[\n" + sql + "\n]]]", e);
                } finally {
                    dialect.doFinallyBlockForArtifact(conn, this.sqlExecutor, artifact);
                }
            }
        }
    }
}
