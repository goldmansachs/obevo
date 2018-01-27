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
package com.gs.obevo.db.impl.platforms.sybaseiq.iqload;

import java.io.File;
import java.sql.Connection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.changetypes.CsvStaticDataDeployer;
import com.gs.obevo.db.impl.core.changetypes.StaticDataChangeRows;
import com.gs.obevo.db.impl.core.changetypes.StaticDataInsertRow;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For this subclass of AbstractCsvStaticDataLoader, we do a bulk delete/insert of the data coming from the input CSV
 * data. Specificaly, we leverage the IQ bulk-load feature
 */
public class IqBulkLoadCsvStaticDataDeployer extends CsvStaticDataDeployer {
    private static final Logger LOG = LoggerFactory.getLogger(IqBulkLoadCsvStaticDataDeployer.class);
    private final IqLoadMode iqLoadMode;
    private final File workDir;

    public IqBulkLoadCsvStaticDataDeployer(DbEnvironment env, SqlExecutor sqlExecutor, DataSource ds, DbMetadataManager metadataManager,
            DbPlatform dbPlatform, IqLoadMode iqLoadMode, File workDir) {
        super(env, sqlExecutor, ds, metadataManager, dbPlatform);
        this.iqLoadMode = iqLoadMode;
        this.workDir = workDir;
    }

    @Override
    protected void executeInserts(Connection conn, StaticDataChangeRows changeRows) {
        PhysicalSchema schema = changeRows.getSchema();
        DaTable table = changeRows.getTable();
        ImmutableList<StaticDataInsertRow> inserts = changeRows.getInsertRows();
        if (inserts.isEmpty()) {
            return;
        }

        MutableList<FieldToColumnMapping> mappings = inserts.getFirst().getInsertColumns().collect(
                new Function<String, FieldToColumnMapping>() {
                    @Override
                    public FieldToColumnMapping valueOf(String s) {
                        return new FieldToColumnMapping(s, s);
                    }
                }).toList();

        IqLoadFileCreator loadFileCreator = new IqLoadFileCreator(table.getName(), mappings, new File(this.workDir,
                "iqload"), "loadFile",
                this.iqLoadMode, new DataExtractor() {
            @Override
            public Object extractValue(Object obj, String fieldName) {
                return ((StaticDataInsertRow) obj).getParams().get(fieldName);
            }
        });

        loadFileCreator.setRowDel("####");
        loadFileCreator.setColDel("!~!~");
        loadFileCreator.openFile();
        LOG.info("Writing the file");
        loadFileCreator.writeToFile(inserts);
        loadFileCreator.closeFile();

        LOG.info("Executing the SQL");

        String mysql = loadFileCreator.getIdLoadCommand(schema.getPhysicalName());

        this.jdbcTemplate.update(conn, mysql);
    }
}
