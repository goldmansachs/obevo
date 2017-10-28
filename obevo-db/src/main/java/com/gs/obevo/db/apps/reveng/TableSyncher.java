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
package com.gs.obevo.db.apps.reveng;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Driver;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaColumnDataType;
import com.gs.obevo.dbmetadata.api.DaColumnImpl;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaIndexType;
import com.gs.obevo.dbmetadata.api.DaNamedObject;
import com.gs.obevo.dbmetadata.api.DaPrimaryKey;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaTableImpl;
import com.gs.obevo.dbmetadata.api.DbMetadataComparisonUtil;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.dbmetadata.deepcompare.CompareBreak;
import com.gs.obevo.dbmetadata.deepcompare.FieldCompareBreak;
import com.gs.obevo.dbmetadata.deepcompare.ObjectCompareBreak;
import com.gs.obevo.util.ArgsParser;
import com.gs.obevo.util.inputreader.Credential;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See http://infocenter.sybase.com/archive/index.jsp?topic=/com.sybase.help.ase_15.0.tables/html/tables/tables25.htm
 * for metadata query details.
 */
public class TableSyncher {
    private static final Logger LOG = LoggerFactory.getLogger(TableSyncher.class);

    public static void main(String[] argsArr) {
        DbFileMergerArgs args = new ArgsParser().parse(argsArr, new DbFileMergerArgs());
        new TableSyncher().execute(args);
    }

    public void execute(DbFileMergerArgs args) {
        Configuration config;
        try {
            config = new PropertiesConfiguration(args.getDbMergeConfigFile());
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
        MutableCollection<DbMergeInfo> dbMergeInfos = DbMergeInfo.parseFromProperties(config);

        MutableCollection<TableSyncSide> tableSyncSides = dbMergeInfos.collect(new Function<DbMergeInfo, TableSyncSide>() {
            @Override
            public TableSyncSide valueOf(DbMergeInfo dbMergeInfo) {
                DataSource ds = ds(dbMergeInfo.getDriverClassName(), dbMergeInfo.getUrl(), dbMergeInfo.getUsername(),
                        dbMergeInfo.getPassword());
                return new TableSyncSide(ds, PhysicalSchema.parseFromString(dbMergeInfo.getPhysicalSchema()));
            }
        });

        this.syncSchemaTables(DbPlatformConfiguration.getInstance().valueOf(config.getString("dbType")), tableSyncSides, args.getOutputDir());
    }

    private static DataSource ds(String driverClassName, String url, String username, String password) {
        Class<? extends Driver> driverClass;
        try {
            driverClass = (Class<? extends Driver>) Class.forName(driverClassName);
            return JdbcDataSourceFactory.createFromJdbcUrl(driverClass, url, new Credential(username, password));
        } catch (ClassNotFoundException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    public static class TableSyncSide {
        private final DataSource dataSource;
        private final PhysicalSchema schema;

        private ImmutableCollection<DaTable> tables;

        public TableSyncSide(DataSource dataSource, PhysicalSchema schema) {
            this.dataSource = dataSource;
            this.schema = schema;
        }

        public DataSource getDataSource() {
            return this.dataSource;
        }

        public PhysicalSchema getSchema() {
            return this.schema;
        }

        public void enrichTables(DbPlatform dbPlatform) {
            this.tables = getTables(dbPlatform, this);
        }

        private static ImmutableCollection<DaTable> getTables(DbPlatform dbPlatform, TableSyncSide side) {
            DbMetadataManager metadataManager = dbPlatform.getDbMetadataManager();
            metadataManager.setDataSource(side.getDataSource());

            DaCatalog database = metadataManager.getDatabase(side.getSchema(), new DaSchemaInfoLevel().setRetrieveTableAndColumnDetails(), true, false);
            return database.getTables().reject(DaTable.IS_VIEW);
        }

        public static final Function<TableSyncSide, ImmutableCollection<DaTable>> TO_TABLES = new Function<TableSyncSide, ImmutableCollection<DaTable>>() {
            @Override
            public ImmutableCollection<DaTable> valueOf(TableSyncSide object) {
                return object.getTables();
            }
        };

        public ImmutableCollection<DaTable> getTables() {
            return this.tables;
        }
    }

    public void syncSchemaTables(DbPlatform dbPlatform, RichIterable<TableSyncSide> syncSides, File outputDir) {
        for (TableSyncSide syncSide : syncSides) {
            syncSide.enrichTables(dbPlatform);
        }

        RichIterable<DaTable> idealTables = this.createIdealTables(syncSides);
        MutableMap<String, DaTable> idealTablesMap = idealTables.toMap(DaNamedObject.TO_NAME, Functions.<DaTable>getPassThru());

        System.out.println("Starting the alters");
        for (TableSyncSide syncSide : syncSides) {
            for (DaTable table : syncSide.getTables()) {
                File outputFile = new File(new File(outputDir, syncSide.getSchema().getPhysicalName()), table.getName() + ".sql");
                this.generateDiffsToTable(table, idealTablesMap.get(table.getName()), outputFile);
            }
        }
    }

    private void generateDiffsToTable(DaTable table, DaTable idealTable, File outputFile) {
        MutableCollection<CompareBreak> compareBreaks = dbMetadataComparisonUtil.compareTables(table, idealTable);
        this.handleBreaks(table, idealTable, compareBreaks, outputFile);
        /*
generate merges against the ideal table (if possible - change may be incompatible)
         */
    }

    /**
     * http://infocenter.sybase.com/archive/index.jsp?topic=/com.sybase.help.ase_15.0.tables/html/tables/tables25.htm
     */
    private RichIterable<DaTable> createIdealTables(RichIterable<TableSyncSide> syncSides) {
        Multimap<String, DaTable> tableMap = syncSides.flatCollect(TableSyncSide.TO_TABLES).groupBy(DaNamedObject.TO_NAME);

        return tableMap.keyMultiValuePairsView().collect(new Function<Pair<String, RichIterable<DaTable>>, DaTable>() {
            @Override
            public DaTable valueOf(Pair<String, RichIterable<DaTable>> pair) {
                RichIterable<DaTable> tables = pair.getTwo();

                final DaTable table = tables.getFirst();
                DaTableImpl idealTable = new DaTableImpl(table.getSchema(), table.getName());
                idealTable.setPrimaryKey(table.getPrimaryKey());

                idealTable.setColumns(createIdealColumns(tables).toList().toImmutable());

                MutableList<DaIndex> indices = Lists.mutable.empty();
                for (DaIndex idealColumn : TableSyncher.this.createIdealIndices(tables)) {
                    if (idealColumn instanceof DaPrimaryKey) {
                        idealTable.setPrimaryKey((DaPrimaryKey) idealColumn);
                    } else {
                        indices.add(idealColumn);
                    }
                }

                idealTable.setIndices(indices.toImmutable());

                return idealTable;
            }
        });
    }

    private RichIterable<DaIndex> createIdealIndices(RichIterable<DaTable> tables) {
        Multimap<String, DaIndex> indexMap = tables.flatCollect(DaTable.TO_INDICES).groupBy(
                new Function<DaIndex, String>() {
                    @Override
                    public String valueOf(DaIndex index) {
                        return index.getName() + ":" + index.getParent().getName();
                    }
                }
        );
        return indexMap.multiValuesView().collect(new Function<RichIterable<DaIndex>, DaIndex>() {
            @Override
            public DaIndex valueOf(RichIterable<DaIndex> indices) {
                if (indices.size() == 1) {
                    return indices.getFirst();
                }
                DaIndex candidate = indices.detect(DaIndex.IS_UNIQUE);
                if (candidate != null) {
                    return candidate;
                }

                candidate = indices.detect(Predicates.attributeEqual(DaIndex.TO_INDEX_TYPE, DaIndexType.CLUSTERED));
                if (candidate != null) {
                    return candidate;
                }

                return indices.getFirst();
            }
        });
    }

    private RichIterable<DaColumn> createIdealColumns(RichIterable<DaTable> tables) {
        Multimap<String, DaColumn> columnMap = tables.flatCollect(DaTable.TO_COLUMNS).groupBy(DaNamedObject.TO_NAME);

        return columnMap.multiValuesView().collect(new Function<RichIterable<DaColumn>, DaColumn>() {
            @Override
            public DaColumn valueOf(RichIterable<DaColumn> columns) {
                DaColumn representative = columns.getFirst();
                MutableSet<DaColumnDataType> columnDataTypes = columns.collect(DaColumn.TO_COLUMN_DATA_TYPE).toSet();
                if (columnDataTypes.size() > 1) {
                    System.out.println("Found multiple data types for column " + representative.getName() + ": " + columnDataTypes.makeString(", "));
                    return representative;
                }

                Integer decimalDigits = extractIntValue(columns, DaColumn.TO_DECIMAL_DIGITS);
                Integer size = extractIntValue(columns, DaColumn.TO_SIZE);
//                Integer width = extractIntValue(columns, DaColumn.TO_WIDTH);

                boolean nullable = extractBooleanValue(columns, DaColumn.TO_NULLABLE, true);
                String defaultValue = extractStringValue(columns, DaColumn.TO_DEFAULT_VALUE);

                DaColumnImpl newcol = new DaColumnImpl(representative.getParent(), representative.getName());
                newcol.setColumnDataType(columnDataTypes.getFirst());
                newcol.setSize(size);
                newcol.setDecimalDigits(decimalDigits);
                newcol.setNullable(nullable);
                newcol.setDefaultValue(defaultValue);

                return newcol;
            }

            private String extractStringValue(RichIterable<DaColumn> columns, Function<DaColumn, String> func) {
                DaColumn representative = columns.getFirst();
                RichIterable<String> values = columns.collect(func).toSet();
                if (values.size() > 1) {
                    System.out.println("Found multiple values for column " + representative.getName());
                    return values.getFirst();
                }
                return values.isEmpty() ? null : values.max();
            }

            private Integer extractIntValue(RichIterable<DaColumn> columns, Function<DaColumn, Integer> func) {
                RichIterable<Integer> values = columns.collect(func).toSet();
                return values.isEmpty() ? null : values.max();
            }

            private boolean extractBooleanValue(RichIterable<DaColumn> columns, Function<DaColumn, Boolean> func, boolean defaultValue) {
                RichIterable<Boolean> values = columns.collect(func).toSet();
                if (values.size() > 1) {
                    return defaultValue;
                } else {
                    return values.getFirst();
                }
            }
        });
    }

    private static final DbMetadataComparisonUtil dbMetadataComparisonUtil = new DbMetadataComparisonUtil();

    private String getIndexSql(DaIndex index) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ");
        if (index.isUnique()) {
            sb.append("UNIQUE ");
        }
        if (index.getIndexType() == DaIndexType.CLUSTERED) {
            sb.append("CLUSTERED ");
        }
        sb.append("INDEX ").append(index.getName()).append(" ON ").append(index.getParent().getName());
        sb.append(index.getColumns().collect(DaNamedObject.TO_NAME).makeString("(", ", ", ")"));
        return sb.toString();
    }

    private void handleBreaks(DaTable table, DaTable idealTable, MutableCollection<CompareBreak> compareBreaks,
            File outputFile) {
        if (compareBreaks.isEmpty()) {
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PrintStream ps = null;
        try {
            outputFile.getParentFile().mkdirs();
            ps = new PrintStream(baos);

            for (CompareBreak compareBreak : compareBreaks) {
                if (compareBreak instanceof ObjectCompareBreak) {
                    ObjectCompareBreak objectBreak = (ObjectCompareBreak) compareBreak;
                    if (objectBreak.getObject() instanceof DaColumn) {
                        DaColumn col = (DaColumn) objectBreak.getObject();
                        this.printSql(ps, "addCol" + col.getName(), this.getAddColumnString(col));
                    } else if (objectBreak.getObject() instanceof DaTable) {
                        LOG.debug("Ignoring this break [{}], relying on AquaReveng to do this", objectBreak.getObject());
                    } else if (objectBreak.getObject() instanceof DaIndex) {
                        DaIndex index = (DaIndex) objectBreak.getObject();
                        if (table.getPrimaryKey() == null || !DaIndex.TO_COLUMNS.valueOf(table.getPrimaryKey()).equals(DaIndex.TO_COLUMNS.valueOf(index))) {
                            this.printSql(ps, "addIndex" + index.getName(), this.getIndexSql(index));
                        } else {
                            System.out.println("Warning - a case where one side has a unique index that is represented as a pk");
                        }
                    } else {
                        this.printSql(ps, "unhandled", "Unhandled Object Break: " + objectBreak.getObject().getClass() + ":" + objectBreak);
                    }
                } else if (compareBreak instanceof FieldCompareBreak) {
                    FieldCompareBreak fieldBreak = (FieldCompareBreak) compareBreak;

                    if (fieldBreak.getLeft() instanceof DaColumn) {
                        DaColumn col = (DaColumn) fieldBreak.getLeft();
                        if (fieldBreak.getFieldName().equals("nullable")) {
                            this.printSql(ps
                                    , "modifyNullable_" + col.getName()
                                    , "-- if the difference is in the nullable value, then set it to nullable for compatibility across instances\n"
                                    + "ALTER TABLE " + col.getParent().getName() + " MODIFY " + col.getName() + " NULL");
                        } else if (fieldBreak.getFieldName().equals("width")) {
                            this.printSql(ps
                                    , "modifyWidth_" + col.getName()
                                    , "-- if the difference is in the width value, then set it to the max value\n"
                                    + "ALTER TABLE " + col.getParent().getName() + " MODIFY " + col.getName() + " " + col.getColumnDataType());
                        } else if (fieldBreak.getFieldName().equals("default")) {
                            this.printSql(ps
                                    , "modifyDefault_" + col.getName()
                                    , "-- if the difference is in the default value, then change it\n"
                                    + "ALTER TABLE " + col.getParent().getName() + " MODIFY " + col.getName() + " DEFAULT " + col.getDefaultValue());
                        } else {
                            this.printSql(ps, "unhandled", fieldBreak.getCompareSubject() + " for field " + fieldBreak.getFieldName() + " had value " + fieldBreak.getLeftVal() + " in left but " + fieldBreak.getRightVal() + " in right");
                        }
                    } else if (fieldBreak.getLeft() instanceof DaPrimaryKey) {
                        if (fieldBreak.getFieldName().equals("indexName")) {
                            LOG.debug("ignore this case - likely a generated PK name: {}", fieldBreak.getFieldName());
                        } else if (fieldBreak.getFieldName().equals("indexType")) {
                            LOG.debug("ignore this case for now [{}] - would get clustered vs. other diffs for pks, which can't seem to be set", fieldBreak.getFieldName());
                        } else {
                            this.printSql(ps, "unhandled", "Less sure on how to represent these...Break was on " + fieldBreak.getLeft().getClass() + ", here are the details: " + compareBreak);
                        }
                    } else if (fieldBreak.getLeft() instanceof DaTable) {
                        if (fieldBreak.getFieldName().equals("primaryKey")) {
                            if (fieldBreak.getLeftVal() == null) {
                                DaIndex uniqueIndex = table.getIndices().detect(Predicates.attributeEqual
                                        (DaIndex.TO_COLUMN_STRING, DaIndex.TO_COLUMN_STRING.valueOf((DaIndex) fieldBreak.getRightVal())));
                                if (uniqueIndex != null) {
                                    this.printSql(ps, "dropAsUnique" + uniqueIndex.getName(),
                                            "DROP INDEX " + table.getName() + "." + uniqueIndex.getName
                                                    ());
                                }
                                DaPrimaryKey primaryKey = idealTable.getPrimaryKey();
                                this.printSql(ps, "createAsPk" + primaryKey.getName(),
                                        "ALTER TABLE " + table.getName() + " ADD PRIMARY KEY " + "(" + DaIndex.TO_COLUMN_STRING.valueOf(primaryKey)
                                                + ")");
                            } else {
                                this.printSql(ps, "unhandled", "Less sure on how to represent these (DIFFERENCE IN PK).." +
                                        ".Break was on " + fieldBreak.getLeft().getClass() + ", here are the details: " + compareBreak);
                            }
                        } else {
                            this.printSql(ps, "unhandled", "Less sure on how to represent these...Break was on " + fieldBreak.getLeft().getClass() + ", here are the details: " + compareBreak);
                        }
                    } else {
                        this.printSql(ps, "unhandled", "Less sure on how to represent these...Break was on " + fieldBreak.getLeft().getClass() + ", here are the details: " + compareBreak);
                    }
                } else {
                    throw new IllegalArgumentException("No such break type");
                }
            }
        } finally {
            IOUtils.closeQuietly(ps);
        }
        String output = baos.toString();
        if (!StringUtils.isBlank(output)) {
            try {
                FileUtils.writeStringToFile(outputFile, output);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void printSql(PrintStream ps, String changeName, String text) {
        ps.println("//// CHANGE name=\"" + changeName + "\"");
        ps.println(text);
        ps.println("GO");
        ps.println("");
    }

    private String getAddColumnString(DaColumn col) {
        return this.getColumnString(col, true);
    }

    private String getModifyColumnString(DaColumn col) {
        return this.getColumnString(col, false);
    }

    private String getColumnString(DaColumn col, boolean addOrModify) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ").append(col.getParent().getName());
        sb.append(" ").append(addOrModify ? "ADD" : "MODIFY").append(" ").append(col.getName());
        // for sybase, we used getLocalTypeName
//        String widthSuffix = col.getColumnDataType().getLocalTypeName().equalsIgnoreCase("float") ? "" : col.getWidth();
        String widthSuffix = col.getColumnDataType().getName().equalsIgnoreCase("float") ? "" : col.getWidth();
        sb.append(" ").append(col.getColumnDataType()).append(widthSuffix);
        if (col.getDefaultValue() != null) {
            sb.append(" DEFAULT ").append(col.getDefaultValue());
        }
        sb.append(" ").append(col.isNullable() ? "NULL" : "NOT NULL");

        return sb.toString();
    }
}
