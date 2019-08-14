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
package com.gs.obevo.db.apps.reveng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.changetypes.CsvStaticDataReader;
import com.gs.obevo.dbmetadata.api.DaNamedObject;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.reader.TextMarkupDocumentReader;
import com.gs.obevo.util.FileUtilsCobra;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.inputreader.CredentialReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

class CsvStaticDataWriter {
    private static final CredentialReader credentialReader = new CredentialReader();
    private final SqlExecutor sqlExecutor;
    private final DbMetadataManager metadataManager;

    public static final String STATIC_DATA_TABLES_FILE_NAME = "static-data-tables.txt";

    public static void start(final AquaRevengArgs args, File workDir) {
        final DbEnvironment env = new DbEnvironment();
        DbPlatform platform = (DbPlatform) args.getPlatform();
        env.setPlatform(platform);
        env.setSystemDbPlatform(platform);
        if (args.getJdbcUrl() != null) {
            env.setJdbcUrl(args.getJdbcUrl());
        } else {
            env.setDbHost(args.getDbHost());
            env.setDbPort(args.getDbPort());
            env.setDbServer(args.getDbServer());
        }
        if (args.getDriverClass() != null) {
            env.setDriverClassName(args.getDriverClass());
        }
        final PhysicalSchema physicalSchema = PhysicalSchema.parseFromString(args.getDbSchema());
        final Schema schema = new Schema(physicalSchema.getPhysicalName());  // use the physical name as the schema name for the reverse-engineering
        env.setSchemas(Sets.immutable.with(schema));

        Credential credential = credentialReader.getCredential(args.getUsername(), args.getPassword(), false, null, null,
                null);

        DbDeployerAppContext ctxt = env.getAppContextBuilder().setCredential(credential).setWorkDir(workDir).buildDbContext();
        final CsvStaticDataWriter mw = new CsvStaticDataWriter(ctxt.getSqlExecutor(),
                ctxt.getDbMetadataManager());

        final MutableList<String> dataTables;
        if (args.getTables() != null && args.getTables().length > 0) {
            dataTables = Lists.mutable.with(args.getTables());
        } else {
            dataTables = FileUtilsCobra.readLines(new File(args.getInputDir(), STATIC_DATA_TABLES_FILE_NAME));
        }

        for (String table : dataTables) {
            System.out.println("Working on table " + table + " at " + new Date());

            CSVFormat csvFormat = CsvStaticDataReader.getCsvFormat(env.getDataDelimiter(), env.getNullToken()).withQuoteMode(QuoteMode.NON_NUMERIC);
            mw.writeTable(env.getPlatform(), physicalSchema, table.trim(),
                    new File(args.getOutputPath(), env.getPlatform().getChangeType(ChangeType.STATICDATA_STR).getDirectoryName()),
                    args.getUpdateTimeColumns(), csvFormat);
        }
    }

    private CsvStaticDataWriter(SqlExecutor sqlExecutor, DbMetadataManager metadataManager) {
        this.sqlExecutor = sqlExecutor;
        this.metadataManager = metadataManager;
    }

    private void writeTable(DbPlatform dbtype, PhysicalSchema schema, String tableName, File directory,
            MutableSet<String> updateTimeColumns, final CSVFormat csvFormat) {
        directory.mkdirs();
        DaTable table = this.metadataManager.getTableInfo(schema, tableName, new DaSchemaInfoLevel().setRetrieveTableColumns(true));
        if (table == null) {
            System.out.println("No data found for table " + tableName);
            return;
        }
        MutableList<String> columnNames = table.getColumns().collect(DaNamedObject.TO_NAME).toList();
        final String updateTimeColumnForTable = updateTimeColumns == null ? null : updateTimeColumns.detect(Predicates
                .in(columnNames));
        if (updateTimeColumnForTable != null) {
            columnNames.remove(updateTimeColumnForTable);
            System.out.println("Will mark " + updateTimeColumnForTable + " as an updateTimeColumn on this table");
        }

        final File tableFile = new File(directory, tableName + ".csv");
        final String selectSql = String.format("SELECT %s FROM %s%s", columnNames.makeString(", "), dbtype.getSchemaPrefix(schema), tableName);

        // using the jdbcTempate and ResultSetHandler to avoid sql-injection warnings in findbugs
        sqlExecutor.executeWithinContext(schema, new Procedure<Connection>() {
            @Override
            public void value(Connection conn) {
                sqlExecutor.getJdbcTemplate().query(conn, selectSql, new ResultSetHandler<Void>() {
                    @Override
                    public Void handle(ResultSet rs) throws SQLException {
                        CSVPrinter writer = null;
                        try {
                            FileWriter fw = new FileWriter(tableFile);
                            writer = new CSVPrinter(fw, csvFormat);

                            if (updateTimeColumnForTable != null) {
                                String metadataLine = String.format("//// METADATA %s=\"%s\"",
                                        TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN, updateTimeColumnForTable);
                                fw.write(metadataLine + "\n");  // writing using the FileWriter directly to avoid having the quotes
                                // delimited
                            }

                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                            int columnCount = rs.getMetaData().getColumnCount();

                            // print headers
                            for (int i = 1; i <= columnCount; ++i) {
                                writer.print(rs.getMetaData().getColumnName(i));
                            }
                            writer.println();

                            while (rs.next()) {
                                for (int i = 1; i <= columnCount; ++i) {
                                    Object object = rs.getObject(i);
                                    if (object != null) {
                                        switch (rs.getMetaData().getColumnType(i)) {
                                        case Types.DATE:
                                            object = dateFormat.format(object);
                                            break;
                                        case Types.TIMESTAMP:
                                            object = dateTimeFormat.format(object);
                                            break;
                                        case Types.LONGVARCHAR:
                                        case Types.VARCHAR:
                                        case Types.CHAR:
                                            // escape the string text if declared so that the input CSV can also handle the escapes
                                            if (csvFormat.getEscapeCharacter() != null && object instanceof String) {
                                                object = ((String) object).replace("" + csvFormat.getEscapeCharacter(), "" + csvFormat.getEscapeCharacter() + csvFormat.getEscapeCharacter());
                                            }
                                            break;
                                        }
                                    }
                                    writer.print(object);
                                }

                                writer.println();
                            }

                            writer.flush();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } finally {
                            IOUtils.closeQuietly(writer);
                        }

                        return null;
                    }
                });
            }
        });

        int blankFileSize = updateTimeColumnForTable == null ? 1 : 2;

        if (!tableFile.canRead() || FileUtilsCobra.readLines(tableFile).size() <= blankFileSize) {
            System.out.println("No data found for table " + tableName + "; will clean up file");
            FileUtils.deleteQuietly(tableFile);
        }
    }
}
