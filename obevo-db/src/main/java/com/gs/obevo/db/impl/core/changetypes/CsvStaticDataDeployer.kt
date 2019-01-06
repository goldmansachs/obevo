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
package com.gs.obevo.db.impl.core.changetypes

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.api.platform.DeployerRuntimeException
import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.platform.DbPlatform
import com.gs.obevo.db.api.platform.SqlExecutor
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper
import com.gs.obevo.dbmetadata.api.*
import com.gs.obevo.impl.reader.TextMarkupDocumentReader
import com.gs.obevocomparer.compare.CatoDataSide
import com.gs.obevocomparer.compare.CatoProperties
import com.gs.obevocomparer.compare.breaks.DataObjectBreak
import com.gs.obevocomparer.compare.breaks.FieldBreak
import com.gs.obevocomparer.compare.simple.SimpleCatoProperties
import com.gs.obevocomparer.input.CatoDataSource
import com.gs.obevocomparer.input.db.QueryDataSource
import com.gs.obevocomparer.input.db.QueryDataSource.QueryExecutor
import com.gs.obevocomparer.util.CatoBaseUtil
import org.apache.commons.lang3.Validate
import org.eclipse.collections.api.block.procedure.Procedure
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.map.mutable.UnifiedMap
import org.slf4j.LoggerFactory
import java.sql.*
import java.util.*
import java.util.Date
import javax.sql.DataSource

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
open class CsvStaticDataDeployer(
        private val env: DbEnvironment,
        private val sqlExecutor: SqlExecutor,
        private val dataSource: DataSource,
        private val metadataManager: DbMetadataManager,
        private val dbPlatform: DbPlatform
) {
    protected val jdbcTemplate: JdbcHelper = sqlExecutor.jdbcTemplate

    fun deployArtifact(staticData: Change) {
        this.deployArtifact(Lists.mutable.with(staticData))
    }

    /**
     * The table list should be in proper insertion order via FK (i.e. if TABLE_B has an FK pointing to TABLE_A,
     * then TABLE_A should come first in the sorted list here)
     */
    fun deployArtifact(staticDatas: List<Change>) {
        val staticDataChanges = staticDatas.map { artifact -> getStaticDataChangesForTable(env, artifact) }

        for (staticDataChange in staticDataChanges) {
            sqlExecutor.executeWithinContext(staticDataChange.schema) { conn -> executeInserts(conn, staticDataChange) }
        }
        for (staticDataChange in staticDataChanges) {
            sqlExecutor.executeWithinContext(staticDataChange.schema) { conn -> executeUpdates(conn, staticDataChange) }
        }
        // note here that deletes must be done in reverse order of the inserts
        for (staticDataChange in staticDataChanges.asReversed()) {
            sqlExecutor.executeWithinContext(staticDataChange.schema) { conn -> executeDeletes(conn, staticDataChange) }
        }
    }

    private fun getStaticDataChangesForTable(env: DbEnvironment, artifact: Change): StaticDataChangeRows {
        val table = Validate.notNull(
                this.metadataManager.getTableInfo(artifact.getPhysicalSchema(env), artifact.objectName, DaSchemaInfoLevel()
                        .setRetrieveTables(true)
                        .setRetrieveTableColumns(true)
                        .setRetrieveTableCheckConstraints(true)
                        .setRetrieveTableIndexes(true)
                        // not retrieving foreign keys
                ),
                "Could not find table %1\$s.%2\$s", artifact.getPhysicalSchema(env), artifact.objectName)

        val fileSource = CsvStaticDataReader().getFileDataSource(env.csvVersion, table, artifact.convertedContent,
                env.dataDelimiter, env.nullToken, dbPlatform.convertDbObjectName())

        // we check this here to ensure that in case there are more fields in the DB than in the csv file
        // (i.e. for default columns), that we exclude them later on

        val fileColumnNames = fileSource.fields.map(this.dbPlatform.convertDbObjectName()::valueOf).toSet()
        val dbColumnNames = table.columns.map(DaNamedObject::getName).map(this.dbPlatform.convertDbObjectName()::valueOf).toSet()

        var updateTimeColumn = artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN)
        if (updateTimeColumn != null) {
            updateTimeColumn = this.dbPlatform.convertDbObjectName().valueOf(updateTimeColumn)
            if (fileColumnNames.contains(updateTimeColumn)) {
                throw IllegalArgumentException(String.format(
                        "The updateTimeColumn value %1\$s should not also be specified in the CSV column content: %2\$s",
                        updateTimeColumn, fileColumnNames))
            } else if (!dbColumnNames.contains(updateTimeColumn)) {
                throw IllegalArgumentException(String.format(
                        "The updateTimeColumn value %1\$s is expected in the database, but was not found: %2\$s",
                        updateTimeColumn, dbColumnNames))
            }
        }

        val keyFields = getUniqueIndexColumnNames(artifact, table, fileColumnNames)

        // exclude fields that are in the db table but not in the file; we'd assume the default/null value would be
        // taken care of by the table definition
        val excludeFields = dbColumnNames.filter { !fileColumnNames.contains(it) }

        val reconFields = SimpleCatoProperties(keyFields, excludeFields)
        return this.parseReconChanges(artifact, table, fileSource, reconFields, fileColumnNames, updateTimeColumn)
    }

    private fun getUniqueIndexColumnNames(artifact: Change, table: DaTable, fileColumnNames: Set<String>): List<String> {
        val doesIndexColumnExistInCsv = { column: DaColumn -> fileColumnNames.contains(column.name)}

        val artifactCandidate = getArtifactColumns(artifact)
        val eligibleUniqueIndexes = getEligibleUniqueIndexes(table)
        val indexCandidate = eligibleUniqueIndexes.firstOrNull { it.columns.all(doesIndexColumnExistInCsv) }

        if (artifactCandidate != null) {
            if (indexCandidate == null) {
                return artifactCandidate
            } else {
                throw IllegalStateException("Cannot specify primary key and override tag on table ${table.name} to support CSV-based static data support")
            }
        } else if (indexCandidate != null) {
            return indexCandidate.columns.map(DaNamedObject::getName).map(this.dbPlatform.convertDbObjectName()::valueOf)
        } else {
            val indexMessages = eligibleUniqueIndexes.map { index ->
                val columnDisplay = { column: DaColumn ->
                    if (doesIndexColumnExistInCsv(column)) column.name else column.name + " (missing)"
                }
                index.name + "-[" + index.columns.joinToString(transform = columnDisplay) + "]"
            }.sorted()

            val messageSuffix = if (indexMessages.isEmpty()) "but none found" else "but existing ${indexMessages.size} indices did not have all columns defined in CSV: ${indexMessages.joinToString("; ")}}"

            throw IllegalStateException("CSV-based static data loads require primary key or unique index on table " + table.name + ", " + messageSuffix)
        }
    }


    private fun getArtifactColumns(artifact: Change): List<String>? {
        return artifact.getMetadataAttribute(TextMarkupDocumentReader.ATTR_PRIMARY_KEYS)?.split(",")
    }

    private fun getEligibleUniqueIndexes(table: DaTable): List<DaIndex> {
        return listOfNotNull(table.primaryKey)
                .plus(table.indices.filter { it.isUnique })
    }

    private fun parseReconChanges(artifact: Change, table: DaTable,
                                  fileSource: CatoDataSource,
                                  reconFields: CatoProperties, fileColumnNames: Set<String>, updateTimeColumn: String?): StaticDataChangeRows {
        val dbSource = this.getQueryDataSource(artifact.getPhysicalSchema(env), table)

        val recon = CatoBaseUtil.compare("name", fileSource, dbSource, reconFields)

        val inserts = Lists.mutable.empty<StaticDataInsertRow>()
        val updates = Lists.mutable.empty<StaticDataUpdateRow>()
        val deletes = Lists.mutable.empty<StaticDataDeleteRow>()

        // must be java.sql.Timestamp, not Date, as that is correct JDBC (and Sybase ASE isn't forgiving of taking in
        // Date for jdbc batch updates)
        val updateTime = Timestamp(Date().time)

        for (reconBreak in recon.breaks) {
            if (reconBreak is FieldBreak) {
                LOG.debug("Found as diff {}", reconBreak)

                val params = UnifiedMap.newMap<String, Any>()
                val whereParams = UnifiedMap.newMap<String, Any>()

                UnifiedMap.newMap(reconBreak.fieldBreaks).forEachKey(Procedure { field ->
                    // same as for updates
                    val fieldToCompare = this@CsvStaticDataDeployer.dbPlatform.convertDbObjectName().valueOf(field)
                    if (!fileColumnNames.contains(fieldToCompare)) {
                        return@Procedure
                    }
                    val value = reconBreak.dataObject.getValue(field)
                    params[field] = value
                })

                if (params.isEmpty) {
                    // nothing to do - only diff was in a default column
                    // see the "DEFAULT_FIELD TIMESTAMP NOT NULL DEFAULT CURRENT TIMESTAMP," use case
                    continue
                }

                if (updateTimeColumn != null) {
                    params[updateTimeColumn] = updateTime
                }

                for (keyField in recon.keyFields) {
                    whereParams[keyField] = reconBreak.getDataObject().getValue(keyField)
                }

                updates.add(StaticDataUpdateRow(params.toImmutable(), whereParams.toImmutable()))
            } else if (reconBreak is DataObjectBreak) {

                when (reconBreak.dataSide) {
                    CatoDataSide.LEFT -> {
                        // file source should be an insert
                        LOG.debug("Found as insert {}", reconBreak)

                        val params = UnifiedMap.newMap<String, Any>()
                        for (field in reconBreak.dataObject.fields) {
                            val fieldToCompare = this.dbPlatform.convertDbObjectName().valueOf(field)
                            if (!fileColumnNames.contains(fieldToCompare)) {
                                continue
                            }
                            params[field] = reconBreak.dataObject.getValue(field)
                        }

                        if (updateTimeColumn != null) {
                            params[updateTimeColumn] = updateTime
                        }

                        val rowNumber = reconBreak.dataObject.getValue(CsvReaderDataSource.ROW_NUMBER_FIELD) as Int

                        inserts.add(StaticDataInsertRow(rowNumber, params.toImmutable()))
                    }
                    CatoDataSide.RIGHT -> {
                        // db source should be a delete
                        LOG.debug("Found as delete {}", reconBreak)

                        val whereParams = UnifiedMap.newMap<String, Any>()
                        for (keyField in recon.keyFields) {
                            whereParams[keyField] = reconBreak.getDataObject().getValue(keyField)
                        }

                        deletes.add(StaticDataDeleteRow(whereParams.toImmutable()))
                    }
                    else -> throw IllegalArgumentException("Invalid enum specified here: " + reconBreak.dataSide + " on " + reconBreak)
                }
            } else {
                throw IllegalStateException(
                        "Cannot have group breaks or any breaks other than Field or DataObject - is your primary key defined correctly? $reconBreak")
            }
        }

        // sort this by the row number to assure that the insertion row from the CSV file remains preserved
        inserts.sortThisBy { it.rowNumber }

        return StaticDataChangeRows(artifact.getPhysicalSchema(env), table, inserts.toImmutable(), updates.toImmutable(), deletes.toImmutable())
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
    protected open fun executeInserts(conn: Connection, changeRows: StaticDataChangeRows) {
        if (changeRows.insertRows.isEmpty) {
            return
        }

        val changeFormat = changeRows.insertRows.first

        val paramValMarkers = arrayOfNulls<String>(changeFormat.insertColumns.size())
        Arrays.fill(paramValMarkers, "?")
        val insertValues = Lists.mutable.with<String>(*paramValMarkers)

        val sql = "INSERT INTO " + dbPlatform.getSchemaPrefix(changeRows.schema) + changeRows.table.name +
                changeFormat.insertColumns.makeString("(", ", ", ")") +
                " VALUES " + insertValues.makeString("(", ", ", ")")
        LOG.info("Executing the insert $sql")

        for (chunkInsertRows in changeRows.insertRows.chunk(INSERT_BATCH_SIZE)) {
            val paramArrays = arrayOfNulls<Array<Any>>(chunkInsertRows.size())
            chunkInsertRows.forEachWithIndex { insert, i -> paramArrays[i] = insert.params.valuesView().toList().toTypedArray() }

            if (LOG.isDebugEnabled) {
                LOG.debug("for " + paramArrays.size + " rows with params: " + Arrays.deepToString(paramArrays))
            }
            this.jdbcTemplate.batchUpdate(conn, sql, paramArrays)
        }
    }

    /**
     * See executeInserts javadoc for why we don't leverage batching here
     */
    private fun executeUpdates(conn: Connection, changeRows: StaticDataChangeRows) {
        for (update in changeRows.updateRows) {
            val updatePieces = Lists.mutable.empty<String>()
            val whereClauseParts = Lists.mutable.empty<String>()
            val paramVals = Lists.mutable.empty<Any>()

            for (stringObjectPair in update.params.keyValuesView()) {
                updatePieces.add(stringObjectPair.one + " = ?")
                paramVals.add(stringObjectPair.two)
            }

            for (stringObjectPair in update.whereParams.keyValuesView()) {
                whereClauseParts.add(stringObjectPair.one + " = ?")
                paramVals.add(stringObjectPair.two)
            }

            val sql = "UPDATE " + dbPlatform.getSchemaPrefix(changeRows.schema) + changeRows.table.name +
                    " SET " + updatePieces.makeString(", ") +
                    " WHERE " + whereClauseParts.makeString(" AND ")

            LOG.info("Executing this break: [$sql] with params [$paramVals]")

            this.jdbcTemplate.update(conn, sql, *paramVals.toArray())
        }
    }

    /**
     * See executeInserts javadoc for why we don't leverage batching here
     */
    private fun executeDeletes(conn: Connection, changeRows: StaticDataChangeRows) {
        for (delete in changeRows.deleteRows) {
            val paramVals = Lists.mutable.empty<Any>()
            val whereClauseParts = Lists.mutable.empty<String>()

            for (stringObjectPair in delete.whereParams.keyValuesView()) {
                val column = stringObjectPair.one
                val value = stringObjectPair.two
                whereClauseParts.add("$column = ?")
                paramVals.add(value)
            }

            val sql = "DELETE FROM " + dbPlatform.getSchemaPrefix(changeRows.schema) + changeRows.table.name +
                    " WHERE " + whereClauseParts.makeString(" AND ")
            LOG.info("DELETING: " + sql + ":" + paramVals.makeString(", "))
            this.jdbcTemplate.update(conn, sql, *paramVals.toArray())
        }
    }

    private fun getQueryDataSource(physicalSchema: PhysicalSchema, table: DaTable): CatoDataSource {
        val cols = table.columns
        val colNameStr = cols.collect(DaNamedObject.TO_NAME).collect(this.dbPlatform.convertDbObjectName()).makeString(", ")
        val query = "select " + colNameStr + " from " + this.dbPlatform.getSchemaPrefix(physicalSchema) + table.name

        try {
            val conn = this.dataSource.connection

            // Here, we need to execute the query using jdbcTemplate so that we can retry exceptions where applicable (e.g. DB2 reorgs)
            return QueryDataSource("dbSource", conn, object : QueryExecutor {
                private var stmt: Statement? = null
                @Throws(Exception::class)
                override fun getResultSet(connection: Connection): ResultSet {
                    val stmtRsPair = jdbcTemplate.queryAndLeaveStatementOpen(conn, query)
                    this.stmt = stmtRsPair.one
                    return stmtRsPair.two
                }

                @Throws(Exception::class)
                override fun close() {
                    this.stmt!!.close()
                }
            })
        } catch (e: SQLException) {
            throw DeployerRuntimeException(e)
        }

    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CsvStaticDataDeployer::class.java)

        private val INSERT_BATCH_SIZE = 25
    }
}
