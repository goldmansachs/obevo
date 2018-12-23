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
package com.gs.obevo.db.impl.core.changetypes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper;
import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaNamedObject;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import com.gs.obevo.impl.reader.TextMarkupDocumentReader;
import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.compare.CatoProperties;
import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.compare.breaks.DataObjectBreak;
import com.gs.obevocomparer.compare.breaks.FieldBreak;
import com.gs.obevocomparer.compare.simple.SimpleCatoProperties;
import com.gs.obevocomparer.input.CatoDataSource;
import com.gs.obevocomparer.input.db.QueryDataSource;
import com.gs.obevocomparer.input.db.QueryDataSource.QueryExecutor;
import com.gs.obevocomparer.util.CatoBaseUtil;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deployer class for loading CSV data into the target table.
 *
 * The deployArtifact method in this class will read in the data from the CSV file and then delegate to the abstract
 * executeInserts method (implemented in the different subclasses) to do the actual loading. This is separated as
 * there may be different ways to load data to the DB (e.g. incremental sqls vs. bulk loads,
 * and bulk load logic may differ across different DBMSs)
 *
 * We separate the calculation of diffs (getStaticDataChangesForTable) from the execution of the changes
 * (executeInserts) as this class will get called by the static data getDeployer to work across multiple tables.
 * e.g. based on the foreign key associations for a number of tables, we calculate the diffs for all,
 * and then in order, we would execute the inserts on all tables in the proper FK order, then updates on all tables,
 * then deletes on all tables in the proper FK order
 */
public class CsvStaticDataDeployer {
    private static final Logger LOG = LoggerFactory.getLogger(CsvStaticDataDeployer.class);

    private static final int INSERT_BATCH_SIZE = 25;

    private final DbEnvironment env;
    private final DbMetadataManager metadataManager;
    private final DbPlatform dbPlatform;
    private final SqlExecutor sqlExecutor;
    protected final JdbcHelper jdbcTemplate;
    private final DataSource dataSource;

    public CsvStaticDataDeployer(DbEnvironment env, SqlExecutor sqlExecutor, DataSource dataSource, DbMetadataManager metadataManager,
            DbPlatform dbPlatform) {
        this.env = env;
        this.sqlExecutor = sqlExecutor;
        this.jdbcTemplate = sqlExecutor.getJdbcTemplate();
        this.dataSource = dataSource;
        this.metadataManager = metadataManager;
        this.dbPlatform = dbPlatform;
    }

    final void deployArtifact(Change staticData) {
        this.deployArtifact(Lists.mutable.with(staticData));
    }

    /**
     * The table list should be in proper insertion order via FK (i.e. if TABLE_B has an FK pointing to TABLE_A,
     * then TABLE_A should come first in the sorted list here)
     */
    final void deployArtifact(ListIterable<Change> staticDatas) {
        ListIterable<StaticDataChangeRows> staticDataChanges = staticDatas.collect(new Function<Change,
                StaticDataChangeRows>() {
            @Override
            public StaticDataChangeRows valueOf(Change artifact) {
                return CsvStaticDataDeployer.this.getStaticDataChangesForTable(env, artifact);
            }
        });

        for (final StaticDataChangeRows staticDataChange : staticDataChanges) {
            sqlExecutor.executeWithinContext(staticDataChange.getSchema(), new Procedure<Connection>() {
                @Override
                public void value(Connection conn) {
                    executeInserts(conn, staticDataChange);
                }
            });
        }
        for (final StaticDataChangeRows staticDataChange : staticDataChanges) {
            sqlExecutor.executeWithinContext(staticDataChange.getSchema(), new Procedure<Connection>() {
                @Override
                public void value(Connection conn) {
                    executeUpdates(conn, staticDataChange);
                }
            });
        }
        // note here that deletes must be done in reverse order of the inserts
        for (final StaticDataChangeRows staticDataChange : staticDataChanges.toList().reverseThis()) {
            sqlExecutor.executeWithinContext(staticDataChange.getSchema(), new Procedure<Connection>() {
                @Override
                public void value(Connection conn) {
                    executeDeletes(conn, staticDataChange);
                }
            });
        }
    }

    private StaticDataChangeRows getStaticDataChangesForTable(DbEnvironment env, Change artifact) {
        DaTable table = Validate.notNull(
                this.metadataManager.getTableInfo(artifact.getPhysicalSchema(env), artifact.getObjectName(), new DaSchemaInfoLevel()
                                .setRetrieveTables(true)
                                .setRetrieveTableColumns(true)
                                .setRetrieveTableCheckConstraints(true)
                                .setRetrieveTableIndexes(true)
                        // not retrieving foreign keys
                ),
                "Could not find table %1$s.%2$s", artifact.getPhysicalSchema(env), artifact.getObjectName());

        CsvReaderDataSource fileSource = new CsvStaticDataReader().getFileDataSource(env.getCsvVersion(), table, artifact.getConvertedContent(),
                env.getDataDelimiter(), env.getNullToken(), dbPlatform.convertDbObjectName());

        // we check this here to ensure that in case there are more fields in the DB than in the csv file
        // (i.e. for default columns), that we exclude them later on
        MutableSet<String> fileColumnNames = UnifiedSet.newSet(fileSource.getFields()).collect(this.dbPlatform.convertDbObjectName());
        MutableSet<String> dbColumnNames = table.getColumns().collect(
                DaNamedObject.TO_NAME).collect(this.dbPlatform.convertDbObjectName()).toSet();

        String updateTimeColumn = artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN);
        if (updateTimeColumn != null) {
            updateTimeColumn = this.dbPlatform.convertDbObjectName().valueOf(updateTimeColumn);
            if (fileColumnNames.contains(updateTimeColumn)) {
                throw new IllegalArgumentException(String.format(
                        "The updateTimeColumn value %1$s should not also be specified in the CSV column content: %2$s",
                        updateTimeColumn, fileColumnNames));
            } else if (!dbColumnNames.contains(updateTimeColumn)) {
                throw new IllegalArgumentException(String.format(
                        "The updateTimeColumn value %1$s is expected in the database, but was not found: %2$s",
                        updateTimeColumn, dbColumnNames));
            }
        }

        ImmutableList<String> keyFields = getUniqueIndexColumnNames(artifact, table);

        // exclude fields that are in the db table but not in the file; we'd assume the default/null value would be
        // taken care of by the table definition
        MutableSet<String> excludeFields = dbColumnNames.select(Predicates.notIn(fileColumnNames));

        CatoProperties reconFields = new SimpleCatoProperties(keyFields.castToList(), excludeFields);
        return this.parseReconChanges(artifact, table, fileSource, reconFields, fileColumnNames, updateTimeColumn);
    }

    private ImmutableList<String> getUniqueIndexColumnNames(Change artifact, DaTable table) {
        String keySpecified = artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_PRIMARY_KEYS);
        boolean overrideKeys = keySpecified != null;
        ImmutableList<String> keyFields;
        DaIndex uniqueKey = this.getUniqueKey(table);
        if (overrideKeys && uniqueKey == null) {
            keyFields = Lists.immutable.with(artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_PRIMARY_KEYS).split(","));
        } else if (overrideKeys && uniqueKey != null) {
            throw new IllegalStateException("Cannot specify primary key and override tag on table " + table.getName()
                    + " to support CSV-based static data support");
        } else {
            if (uniqueKey == null) {
                throw new IllegalStateException("Require a primary key or unique index on table " + table.getName()
                        + " to support CSV-based static data support");
            }
            keyFields = uniqueKey.getColumns().collect(DaNamedObject.TO_NAME)
                    .collect(this.dbPlatform.convertDbObjectName());
        }
        return keyFields;
    }

    private StaticDataChangeRows parseReconChanges(Change artifact, DaTable table,
            CatoDataSource fileSource,
            CatoProperties reconFields, final MutableSet<String> fileColumnNames, String updateTimeColumn) {
        CatoDataSource dbSource = this.getQueryDataSource(artifact.getPhysicalSchema(env), table);

        CatoComparison recon = CatoBaseUtil.compare("name", fileSource, dbSource, reconFields);

        MutableList<StaticDataInsertRow> inserts = Lists.mutable.empty();
        MutableList<StaticDataUpdateRow> updates = Lists.mutable.empty();
        MutableList<StaticDataDeleteRow> deletes = Lists.mutable.empty();

        // must be java.sql.Timestamp, not Date, as that is correct JDBC (and Sybase ASE isn't forgiving of taking in
        // Date for jdbc batch updates)
        Timestamp updateTime = new Timestamp(new Date().getTime());

        for (Break reconBreak : recon.getBreaks()) {
            if (reconBreak instanceof FieldBreak) {
                LOG.debug("Found as diff {}", reconBreak);
                final FieldBreak fieldBreak = (FieldBreak) reconBreak;

                final MutableMap<String, Object> params = UnifiedMap.newMap();
                MutableMap<String, Object> whereParams = UnifiedMap.newMap();

                UnifiedMap.newMap(fieldBreak.getFieldBreaks()).forEachKey(new Procedure<String>() {
                    @Override
                    public void value(String field) {
                        // same as for updates
                        String fieldToCompare = CsvStaticDataDeployer.this.dbPlatform.convertDbObjectName().valueOf(field);
                        if (!fileColumnNames.contains(fieldToCompare)) {
                            return;
                        }
                        Object value = fieldBreak.getDataObject().getValue(field);
                        params.put(field, value);
                    }
                });

                if (params.isEmpty()) {
                    // nothing to do - only diff was in a default column
                    // see the "DEFAULT_FIELD TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP," use case
                    continue;
                }

                if (updateTimeColumn != null) {
                    params.put(updateTimeColumn, updateTime);
                }

                for (String keyField : recon.getKeyFields()) {
                    whereParams.put(keyField, reconBreak.getDataObject().getValue(keyField));
                }

                updates.add(new StaticDataUpdateRow(params.toImmutable(), whereParams.toImmutable()));
            } else if (reconBreak instanceof DataObjectBreak) {
                DataObjectBreak dataBreak = (DataObjectBreak) reconBreak;

                switch (dataBreak.getDataSide()) {
                case LEFT: // file source should be an insert
                    LOG.debug("Found as insert {}", dataBreak);

                    MutableMap<String, Object> params = UnifiedMap.newMap();
                    for (String field : dataBreak.getDataObject().getFields()) {
                        String fieldToCompare = this.dbPlatform.convertDbObjectName().valueOf(field);
                        if (!fileColumnNames.contains(fieldToCompare)) {
                            continue;
                        }
                        params.put(field, dataBreak.getDataObject().getValue(field));
                    }

                    if (updateTimeColumn != null) {
                        params.put(updateTimeColumn, updateTime);
                    }

                    int rowNumber = (int) dataBreak.getDataObject().getValue(CsvReaderDataSource.ROW_NUMBER_FIELD);

                    inserts.add(new StaticDataInsertRow(rowNumber, params.toImmutable()));
                    break;
                case RIGHT: // db source should be a delete
                    LOG.debug("Found as delete {}", dataBreak);

                    MutableMap<String, Object> whereParams = UnifiedMap.newMap();
                    for (String keyField : recon.getKeyFields()) {
                        whereParams.put(keyField, reconBreak.getDataObject().getValue(keyField));
                    }

                    deletes.add(new StaticDataDeleteRow(whereParams));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid enum specified here: " + dataBreak.getDataSide()
                            + " on " + dataBreak);
                }
            } else {
                throw new IllegalStateException(
                        "Cannot have group breaks or any breaks other than Field or DataObject - is your primary key defined correctly? "
                                + reconBreak);
            }
        }

        // sort this by the row number to assure that the insertion row from the CSV file remains preserved
        inserts.sortThisBy(new Function<StaticDataInsertRow, Comparable>() {
            @Override
            public Comparable valueOf(StaticDataInsertRow object) {
                return object.getRowNumber();
            }
        });

        return new StaticDataChangeRows(artifact.getPhysicalSchema(env), table, inserts.toImmutable(), updates.toImmutable(), deletes.toImmutable());
    }

    /**
     * Note - we still need the PhysicalSchema object, as the schema coming from sybase may still have "dbo" there.
     * Until we abstract this in the metadata API, we go w/ the signature as is
     *
     * Also - this can be overridable in case we want to support bulk-inserts for specific database types,
     * e.g. Sybase IQ
     *
     * We only use batching for "executeInserts" as that is the 90% case of the performance
     * (updates may vary the kinds of sqls that are needed, and deletes I'd assume are rare)
     */
    protected void executeInserts(Connection conn, StaticDataChangeRows changeRows) {
        if (changeRows.getInsertRows().isEmpty()) {
            return;
        }

        StaticDataInsertRow changeFormat = changeRows.getInsertRows().getFirst();

        String[] paramValMarkers = new String[changeFormat.getInsertColumns().size()];
        Arrays.fill(paramValMarkers, "?");
        MutableList<String> insertValues = Lists.mutable.with(paramValMarkers);

        String sql = "INSERT INTO " + dbPlatform.getSchemaPrefix(changeRows.getSchema()) + changeRows.getTable().getName() +
                changeFormat.getInsertColumns().makeString("(", ", ", ")") +
                " VALUES " + insertValues.makeString("(", ", ", ")");
        LOG.info("Executing the insert " + sql);

        for (RichIterable<StaticDataInsertRow> chunkInsertRows : changeRows.getInsertRows().chunk(INSERT_BATCH_SIZE)) {
            final Object[][] paramArrays = new Object[chunkInsertRows.size()][];
            chunkInsertRows.forEachWithIndex(new ObjectIntProcedure<StaticDataInsertRow>() {
                @Override
                public void value(StaticDataInsertRow insert, int i) {
                    paramArrays[i] = insert.getParams().valuesView().toList().toArray(new Object[0]);
                }
            });

            if (LOG.isDebugEnabled()) {
                LOG.debug("for " + paramArrays.length + " rows with params: " + Arrays.deepToString(paramArrays));
            }
            this.jdbcTemplate.batchUpdate(conn, sql, paramArrays);
        }
    }

    /**
     * See executeInserts javadoc for why we don't leverage batching here
     */
    private void executeUpdates(Connection conn, StaticDataChangeRows changeRows) {
        for (StaticDataUpdateRow update : changeRows.getUpdateRows()) {
            MutableList<String> updatePieces = Lists.mutable.empty();
            MutableList<String> whereClauseParts = Lists.mutable.empty();
            MutableList<Object> paramVals = Lists.mutable.empty();

            for (Pair<String, Object> stringObjectPair : update.getParams().keyValuesView()) {
                updatePieces.add(stringObjectPair.getOne() + " = ?");
                paramVals.add(stringObjectPair.getTwo());
            }

            for (Pair<String, Object> stringObjectPair : update.getWhereParams().keyValuesView()) {
                whereClauseParts.add(stringObjectPair.getOne() + " = ?");
                paramVals.add(stringObjectPair.getTwo());
            }

            String sql = "UPDATE " + dbPlatform.getSchemaPrefix(changeRows.getSchema()) + changeRows.getTable().getName() +
                    " SET " + updatePieces.makeString(", ") +
                    " WHERE " + whereClauseParts.makeString(" AND ");

            LOG.info("Executing this break: [" + sql + "] with params [" + paramVals + "]");

            this.jdbcTemplate.update(conn, sql, paramVals.toArray());
        }
    }

    /**
     * See executeInserts javadoc for why we don't leverage batching here
     */
    private void executeDeletes(Connection conn, StaticDataChangeRows changeRows) {
        for (StaticDataDeleteRow delete : changeRows.getDeleteRows()) {
            MutableList<Object> paramVals = Lists.mutable.empty();
            MutableList<String> whereClauseParts = Lists.mutable.empty();

            for (Pair<String, Object> stringObjectPair : delete.getWhereParams().keyValuesView()) {
                String column = stringObjectPair.getOne();
                Object value = stringObjectPair.getTwo();
                whereClauseParts.add(column + " = ?");
                paramVals.add(value);
            }

            String sql = "DELETE FROM " + dbPlatform.getSchemaPrefix(changeRows.getSchema()) + changeRows.getTable().getName() +
                    " WHERE " + whereClauseParts.makeString(" AND ");
            LOG.info("DELETING: " + sql + ":" + paramVals.makeString(", "));
            this.jdbcTemplate.update(conn, sql, paramVals.toArray());
        }
    }

    private CatoDataSource getQueryDataSource(PhysicalSchema physicalSchema, DaTable table) {
        ImmutableList<DaColumn> cols = table.getColumns();
        String colNameStr = cols.collect(DaNamedObject.TO_NAME).collect(this.dbPlatform.convertDbObjectName()).makeString(", ");
        final String query = "select " + colNameStr + " from " + this.dbPlatform.getSchemaPrefix(physicalSchema) + table.getName();

        try {
            final Connection conn = this.dataSource.getConnection();

            // Here, we need to execute the query using jdbcTemplate so that we can retry exceptions where applicable (e.g. DB2 reorgs)
            return new QueryDataSource("dbSource", conn, new QueryExecutor() {
                private Statement stmt;
                @Override
                public ResultSet getResultSet(Connection connection) throws Exception {
                    ResultSet rs = jdbcTemplate.queryAndLeaveStatementOpen(conn, query);
                    this.stmt = rs.getStatement();
                    return rs;
                }

                @Override
                public void close() throws Exception {
                    this.stmt.close();
                }
            });
        } catch (SQLException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    private DaIndex getUniqueKey(DaTable table) {
        if (table.getPrimaryKey() == null) {
            for (DaIndex index : table.getIndices()) {
                if (index.isUnique()) {
                    return index;
                }
            }
        } else {
            return table.getPrimaryKey();
        }
        return null;
    }
}
