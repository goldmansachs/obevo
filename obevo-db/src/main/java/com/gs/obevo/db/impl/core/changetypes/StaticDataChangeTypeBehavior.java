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
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.GroupChange;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.api.platform.ChangeTypeCommandCalculator;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.api.platform.CommandExecutionContext;
import com.gs.obevo.impl.changecalc.ChangeCommandFactory;
import com.gs.obevo.impl.graph.GraphEnricher;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deploying static data files into tables. (or BCP files, or reference data tables, whatever you want to call them).
 *
 * This does not extend AbstractDbChangeType as we can do non-SQL deployments for this. Such implementations would be
 * behind the CsvStaticDataDeployer class
 */
public class StaticDataChangeTypeBehavior implements ChangeTypeBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(StaticDataChangeTypeBehavior.class);
    private static final Pattern INSERT_INTO_PATTERN = Pattern.compile("(?i)^.*insert\\s+into.*$", Pattern.DOTALL);

    private final SqlExecutor sqlExecutor;
    private final DbSimpleArtifactDeployer baseArtifactDeployer;
    private final CsvStaticDataDeployer csvStaticDataDeployer;
    private final GraphEnricher graphEnricher;

    public StaticDataChangeTypeBehavior(SqlExecutor sqlExecutor, DbSimpleArtifactDeployer baseArtifactDeployer, CsvStaticDataDeployer csvStaticDataDeployer, GraphEnricher graphEnricher) {
        this.sqlExecutor = sqlExecutor;
        this.baseArtifactDeployer = baseArtifactDeployer;
        this.csvStaticDataDeployer = csvStaticDataDeployer;
        this.graphEnricher = graphEnricher;
    }

    @Override
    public void deploy(Change change, CommandExecutionContext cec) {
        final ListIterable<Change> staticDatas = getSubChanges(change);

        if (staticDatas.isEmpty()) {
            LOG.info("No changes specified for this command");
            return;
        }

        boolean hasInsertIntoPattern = false;
        boolean hasCsvPattern = false;

        for (Change staticData : staticDatas) {
            if (isInsertModeStaticData(staticData)) {
                hasInsertIntoPattern = true;
            } else {
                hasCsvPattern = true;
            }
        }

        if (hasInsertIntoPattern && hasCsvPattern) {
            throw new IllegalArgumentException("Within a group of staticData changes, " +
                    "we cannot have a mix of CSV data files and InsertInto data files. Please convert all of them to " +
                    "the CSV format: " + staticDatas.collect(Change.TO_DISPLAY_STRING).makeString(", "));
        }

        if (hasCsvPattern) {
            LOG.info("Executing load of csv file/s");
            csvStaticDataDeployer.deployArtifact(staticDatas);
        } else {
            // note - we do not yet support the "multiple csv file group" w/ the "insert into" files; only for CSVs
            // handle this as a regular case
            LOG.info("Executing as regular inserts");
            if (staticDatas.size() > 1) {
                LOG.info("WARNING: If you are managing static data for tables related by foreign keys, " +
                        "you should switch to the CSV format and away from the InsertInto format, " +
                        "as subsequent incremental updates may only be supportable in the Csv format");
            }

            for (final Change staticData : staticDatas) {
                sqlExecutor.executeWithinContext(staticData.getPhysicalSchema(), new Procedure<Connection>() {
                    @Override
                    public void value(Connection conn) {
                        baseArtifactDeployer.deployArtifact(conn, staticData);
                    }
                });
            }
        }
    }

    private static boolean isInsertModeStaticData(Change staticData) {
        return isInsertModeStaticData(staticData.getConvertedContent());
    }

    public static boolean isInsertModeStaticData(String sql) {
        return INSERT_INTO_PATTERN.matcher(sql).matches();
    }

    private ImmutableList<Change> getSubChanges(Change change) {
        return ((GroupChange) change).getChanges();
    }

    @Override
    public ChangeTypeCommandCalculator getChangeTypeCalculator() {
        return new StaticDataChangeTypeCommandCalculator(new ChangeCommandFactory(), graphEnricher);
    }

    @Override
    public String getDefinitionFromEnvironment(Change exampleChange) {
        return null;  // not applicable for static data
    }

    @Override
    public void manage(Change change, ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        for (Change staticData : getSubChanges(change)) {
            changeAuditDao.updateOrInsertChange(staticData, deployExecution);
        }
    }

    @Override
    public void unmanage(Change change, ChangeAuditDao changeAuditDao) {
        changeAuditDao.deleteChange(change);
    }

    @Override
    public void unmanageObject(Change change, ChangeAuditDao changeAuditDao) {
        throw new UnsupportedOperationException("This method is not supported for this object type.");
    }

    @Override
    public void undeploy(Change change) {
        LOG.info("No drops for static data change; we will just unmanage");
    }

    @Override
    public void dropObject(Change change, boolean dropForRecreate) {
        throw new UnsupportedOperationException("This method is not supported for this object type.");
    }
}
