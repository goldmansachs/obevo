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
package com.gs.obevo.db.impl.core.changeauditdao

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.ChangeIncremental
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.ChangeRerunnable
import com.gs.obevo.api.appdata.DeployExecution
import com.gs.obevo.api.appdata.DeployExecutionImpl
import com.gs.obevo.api.appdata.DeployExecutionStatus
import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.api.platform.AuditLock
import com.gs.obevo.api.platform.ChangeAuditDao
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.appdata.Grant
import com.gs.obevo.db.api.appdata.GrantTargetType
import com.gs.obevo.db.api.appdata.Permission
import com.gs.obevo.db.api.platform.DbChangeTypeBehavior
import com.gs.obevo.db.api.platform.SqlExecutor
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel
import com.gs.obevo.dbmetadata.api.DaTable
import com.gs.obevo.dbmetadata.api.DbMetadataManager
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry
import com.gs.obevo.util.VisibleForTesting
import com.gs.obevo.util.knex.InternMap
import org.apache.commons.dbutils.handlers.MapListHandler
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.function.checked.ThrowingFunction
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.factory.Sets
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Timestamp

/**
 * AuditDao that will write the audit to the same db schema as the Change audit
 */
class SameSchemaChangeAuditDao(private val env: DbEnvironment, private val sqlExecutor: SqlExecutor, private val dbMetadataManager: DbMetadataManager,
                               private val deployUserId: String, private val deployExecutionDao: SameSchemaDeployExecutionDao, private val changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry) : ChangeAuditDao {
    private val dbChangeTable: String
    private val changeNameColumn: String
    private val changeTypeColumn: String
    private val deployUserIdColumn: String
    private val timeInsertedColumn: String
    private val timeUpdatedColumn: String
    private val rollbackContentColumn: String
    private val insertDeployExecutionIdColumn: String
    private val updateDeployExecutionIdColumn: String

    private val currentTimestamp: Timestamp
        get() = Timestamp(DateTime().millis)

    init {

        val convertDbObjectName = env.platform.convertDbObjectName()
        this.dbChangeTable = convertDbObjectName.valueOf(ChangeAuditDao.CHANGE_AUDIT_TABLE_NAME)  // for backwards-compatibility, the dbChange table is named "ARTIFACTDEPLOYMENT". We hope to migrate existing tables eventually
        this.changeNameColumn = convertDbObjectName.valueOf("ARTIFACTPATH")  // for backwards-compatibility, the changeName column is named "ArtifactPath". We hope to migrate existing tables eventually
        this.changeTypeColumn = convertDbObjectName.valueOf("CHANGETYPE")
        this.deployUserIdColumn = convertDbObjectName.valueOf("DEPLOY_USER_ID")
        this.timeInsertedColumn = convertDbObjectName.valueOf("TIME_INSERTED")
        this.timeUpdatedColumn = convertDbObjectName.valueOf("TIME_UPDATED")
        this.rollbackContentColumn = convertDbObjectName.valueOf("ROLLBACKCONTENT")
        this.insertDeployExecutionIdColumn = convertDbObjectName.valueOf("INSERTDEPLOYID")
        this.updateDeployExecutionIdColumn = convertDbObjectName.valueOf("UPDATEDEPLOYID")
    }

    override fun getAuditContainerName(): String {
        return dbChangeTable
    }

    override fun init() {
        for (schema in env.schemaNames) {
            val physicalSchema = env.getPhysicalSchema(schema)
            sqlExecutor.executeWithinContext(physicalSchema) { conn -> init(conn, schema) }
        }
    }

    private fun init(conn: Connection, schema: String) {
        val physicalSchema = env.getPhysicalSchema(schema)
        val artifactTable = queryAuditTable(physicalSchema)
        val jdbc = sqlExecutor.jdbcTemplate
        if (artifactTable == null) {
            val auditTableSql = get5_1Sql(physicalSchema)
            jdbc.execute(conn, auditTableSql)

            // We only grant SELECT access to PUBLIC so that folks can read the audit table without DBO permission
            // (we don't grant R/W access, as we assume whatever login that executes deployments already has DBO access,
            // and so can modify this table)
            val tableChangeType = changeTypeBehaviorRegistry.getChangeTypeBehavior(ChangeType.TABLE_STR) as DbChangeTypeBehavior

            if (env.platform.isPublicSchemaSupported) {
                tableChangeType.applyGrants(conn, physicalSchema, dbChangeTable, Lists.immutable.with(Permission("artifacTable",
                        Lists.immutable.with(Grant(Lists.immutable.with("SELECT"), Multimaps.immutable.list.with(GrantTargetType.PUBLIC, "PUBLIC"))))))
            }
        } else {
            // We will still grant this here to make up for the existing DBs that did not have the grants given
            val tableChangeType = changeTypeBehaviorRegistry.getChangeTypeBehavior(ChangeType.TABLE_STR) as DbChangeTypeBehavior

            val schemaPlusTable = env.platform.getSubschemaPrefix(physicalSchema) + dbChangeTable

            // Here, we detect if we are on an older version of the table due to missing columns (added for version
            // 3.9.0). If we find the
            // columns are missing, we will add them and backfill
            if (artifactTable.getColumn(deployUserIdColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        deployUserIdColumn, "VARCHAR(32)", env.platform.nullMarkerForCreateTable))
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable,
                        deployUserIdColumn, "'backfill'"))
            }
            if (artifactTable.getColumn(timeUpdatedColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        timeUpdatedColumn, env.platform.timestampType, env.platform
                        .nullMarkerForCreateTable))
                jdbc.execute(
                        conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, timeUpdatedColumn, "'"
                        + TIMESTAMP_FORMAT.print(DateTime()) + "'"))
            }
            if (artifactTable.getColumn(timeInsertedColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        timeInsertedColumn, env.platform.timestampType, env.platform
                        .nullMarkerForCreateTable))
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, timeInsertedColumn, timeUpdatedColumn))
            }
            if (artifactTable.getColumn(rollbackContentColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        rollbackContentColumn, env.platform.textType, env.platform.nullMarkerForCreateTable))
                // for the 3.12.0 release, we will also update the METADATA changeType value to STATICDATA
                jdbc.execute(conn, String.format("UPDATE %1\$s SET %2\$s='%3\$s' WHERE %2\$s='%4\$s'",
                        schemaPlusTable, changeTypeColumn, ChangeType.STATICDATA_STR,
                        OLD_STATICDATA_CHANGETYPE))
            }
            if (artifactTable.getColumn(insertDeployExecutionIdColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        insertDeployExecutionIdColumn, env.platform.bigIntType, env.platform.nullMarkerForCreateTable))

                // If this column doesn't exist, it means we've just moved to the version w/ the DeployExecution table.
                // Let's add a row here to backfill the data.
                val deployExecution = DeployExecutionImpl("backfill", "backfill", schema, "0.0.0", currentTimestamp, false, false, null, "backfill", Sets.immutable.empty())
                deployExecution.status = DeployExecutionStatus.SUCCEEDED
                deployExecutionDao.persistNewSameContext(conn, deployExecution, physicalSchema)
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, insertDeployExecutionIdColumn, deployExecution.id))
            }
            if (artifactTable.getColumn(updateDeployExecutionIdColumn) == null) {
                jdbc.execute(conn, String.format("alter table %s ADD %s %s %s", schemaPlusTable,
                        updateDeployExecutionIdColumn, env.platform.bigIntType, env.platform.nullMarkerForCreateTable))
                jdbc.execute(conn, String.format("UPDATE %s SET %s = %s", schemaPlusTable, updateDeployExecutionIdColumn, insertDeployExecutionIdColumn))
            }
        }
    }

    private fun queryAuditTable(physicalSchema: PhysicalSchema): DaTable? {
        return this.dbMetadataManager.getTableInfo(physicalSchema, dbChangeTable, DaSchemaInfoLevel().setRetrieveTableColumns(true))
    }

    /**
     * SQL for the 5.0.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    fun get5_0Sql(physicalSchema: PhysicalSchema): String {
        return if (env.auditTableSql != null)
            env.auditTableSql
        else
            String.format("CREATE TABLE " + env.platform.getSchemaPrefix(physicalSchema) + dbChangeTable + " ( \n" +
                    "    ARTFTYPE    \tVARCHAR(31) NOT NULL,\n" +
                    "    " + changeNameColumn + "\tVARCHAR(255) NOT NULL,\n" +
                    "    OBJECTNAME  \tVARCHAR(255) NOT NULL,\n" +
                    "    ACTIVE      \tINTEGER %1\$s,\n" +
                    "    " + changeTypeColumn + "  \tVARCHAR(255) %1\$s,\n" +
                    "    CONTENTHASH \tVARCHAR(255) %1\$s,\n" +
                    "    DBSCHEMA    \tVARCHAR(255) %1\$s,\n" +
                    "    " + deployUserIdColumn + "    \tVARCHAR(32) %1\$s,\n" +
                    "    " + timeInsertedColumn + "   \t" + env.platform.timestampType + " %1\$s,\n" +
                    "    " + timeUpdatedColumn + "    \t" + env.platform.timestampType + " %1\$s,\n" +
                    "    " + rollbackContentColumn + "\t" + env.platform.textType + " %1\$s,\n" +
                    "    CONSTRAINT ARTDEFPK PRIMARY KEY(" + changeNameColumn + ",OBJECTNAME)\n" +
                    ") %2\$s\n", env.platform.nullMarkerForCreateTable, env.platform.getTableSuffixSql(env))
    }

    /**
     * SQL for the 5.1.x upgrade.
     * We keep in separate methods to allow for easy testing of the upgrades in SameSchemaChangeAuditDaoTest for different DBMSs
     */
    @VisibleForTesting
    fun get5_1Sql(physicalSchema: PhysicalSchema): String {
        return if (env.auditTableSql != null)
            env.auditTableSql
        else
            String.format("CREATE TABLE " + env.platform.getSchemaPrefix(physicalSchema) + dbChangeTable + " ( \n" +
                    "    ARTFTYPE    \tVARCHAR(31) NOT NULL,\n" +
                    "    " + changeNameColumn + "\tVARCHAR(255) NOT NULL,\n" +
                    "    OBJECTNAME  \tVARCHAR(255) NOT NULL,\n" +
                    "    ACTIVE      \tINTEGER %1\$s,\n" +
                    "    " + changeTypeColumn + "  \tVARCHAR(255) %1\$s,\n" +
                    "    CONTENTHASH \tVARCHAR(255) %1\$s,\n" +
                    "    DBSCHEMA    \tVARCHAR(255) %1\$s,\n" +
                    "    " + deployUserIdColumn + "    \tVARCHAR(32) %1\$s,\n" +
                    "    " + timeInsertedColumn + "   \t" + env.platform.timestampType + " %1\$s,\n" +
                    "    " + timeUpdatedColumn + "    \t" + env.platform.timestampType + " %1\$s,\n" +
                    "    " + rollbackContentColumn + "\t" + env.platform.textType + " %1\$s,\n" +
                    "    " + insertDeployExecutionIdColumn + "\t" + env.platform.bigIntType + " %1\$s,\n" +
                    "    " + updateDeployExecutionIdColumn + "\t" + env.platform.bigIntType + " %1\$s,\n" +
                    "    CONSTRAINT ARTDEFPK PRIMARY KEY(" + changeNameColumn + ",OBJECTNAME)\n" +
                    ") %2\$s\n", env.platform.nullMarkerForCreateTable, env.platform.getTableSuffixSql(env))
    }

    override fun insertNewChange(change: Change, deployExecution: DeployExecution) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env)) { conn -> insertNewChangeInternal(conn, change, deployExecution) }
    }

    private fun insertNewChangeInternal(conn: Connection, change: Change, deployExecution: DeployExecution) {
        val jdbcTemplate = sqlExecutor.jdbcTemplate

        val currentTimestamp = currentTimestamp
        jdbcTemplate.update(
                conn, "INSERT INTO " + env.platform.getSchemaPrefix(change.getPhysicalSchema(env))
                + dbChangeTable +
                " (ARTFTYPE, DBSCHEMA, ACTIVE, CHANGETYPE, CONTENTHASH, " + changeNameColumn + ", OBJECTNAME, "
                + rollbackContentColumn + ", " + deployUserIdColumn + ", " + timeInsertedColumn + ", " + timeUpdatedColumn + ", " + insertDeployExecutionIdColumn + ", " + updateDeployExecutionIdColumn + ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", if (change is ChangeIncremental) "I" else "R", change.schema, if (change.isActive) 1 else 0, change.changeType.name, change.contentHash, change.changeName, change.objectName, change.rollbackContent, deployUserId, currentTimestamp, currentTimestamp, deployExecution.id, deployExecution.id
        )
    }

    override fun updateOrInsertChange(change: Change, deployExecution: DeployExecution) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env)) { conn ->
            val numRowsUpdated = updateInternal(conn, change, deployExecution)
            if (numRowsUpdated == 0) {
                insertNewChangeInternal(conn, change, deployExecution)
            }
        }
    }

    override fun getDeployedChanges(): ImmutableList<Change> {
        val convertDbObjectName = env.platform.convertDbObjectName()

        val artfs = env.schemaNames.flatMap { schema ->
            val deployExecutionsById = deployExecutionDao.getDeployExecutions(schema).associateBy(DeployExecution::getId)

            val physicalSchema = env.getPhysicalSchema(schema)
            sqlExecutor.executeWithinContext(physicalSchema, ThrowingFunction<Connection, List<Change>> { conn ->
                val jdbcTemplate = sqlExecutor.jdbcTemplate
                val artifactTable = queryAuditTable(physicalSchema)
                        ?: return@ThrowingFunction emptyList()  // If the artifact tables does not exist, then return empty list for that schema

                return@ThrowingFunction jdbcTemplate.query(
                        conn, "SELECT * FROM " + env.platform.getSchemaPrefix(physicalSchema) + dbChangeTable
                        + " WHERE DBSCHEMA = '" + schema + "'", MapListHandler()).map { resultSet ->
                    val artfType = resultSet[convertDbObjectName.valueOf("ARTFTYPE")] as String
                    val artf: Change
                    if (artfType == "I") {
                        artf = ChangeIncremental()
                    } else if (artfType == "R") {
                        artf = ChangeRerunnable()
                    } else {
                        throw IllegalArgumentException("This type does not exist $artfType")
                    }

                    var changeType = resultSet[changeTypeColumn] as String
                    changeType = if (changeType == OLD_STATICDATA_CHANGETYPE) ChangeType.STATICDATA_STR else changeType
                    artf.changeKey = ChangeKey(
                            InternMap.instance().intern(resultSet[convertDbObjectName.valueOf("DBSCHEMA")] as String),
                            env.platform.getChangeType(changeType),
                            InternMap.instance().intern(resultSet[convertDbObjectName.valueOf("OBJECTNAME")] as String),
                            resultSet[convertDbObjectName.valueOf(changeNameColumn)] as String
                    )

                    artf.isActive = env.platform.getIntegerValue(resultSet[convertDbObjectName.valueOf("ACTIVE")]) == 1
                    // change METADATA to STATICDATA for backward compatability

                    artf.contentHash = resultSet[convertDbObjectName.valueOf("CONTENTHASH")] as String?
                    // these are repeated often

                    artf.timeInserted = env.platform.getTimestampValue(resultSet[convertDbObjectName.valueOf(timeInsertedColumn)])
                    artf.timeUpdated = env.platform.getTimestampValue(resultSet[convertDbObjectName.valueOf(timeUpdatedColumn)])
                    artf.deployExecution = deployExecutionsById[resultSet[updateDeployExecutionIdColumn] as Long]

                    // for backward compatibility, make sure the ROLLBACKCONTENT column exists
                    if (artifactTable.getColumn(rollbackContentColumn) != null) {
                        artf.rollbackContent = resultSet[rollbackContentColumn] as String?
                    }
                    artf
                }
            })
        }

        // This block is to aim to backfill the "orderWithinObject" field, since we don't persist it in the database
        val incrementalChanges = artfs.filterNot { it.changeType.isRerunnable }
        val incrementalChangeMap = incrementalChanges.groupBy(Change::getObjectKey)
        incrementalChangeMap.values.forEach { objectChanges ->
            val sortedObjectChanges = objectChanges.sortedBy(Change::getTimeInserted)
            sortedObjectChanges.forEachIndexed { index, each -> each.orderWithinObject = 5000 + index }
        }

        return Lists.immutable.ofAll(artfs.toSet().toList().filter { env.schemaNames.contains(it.schema) })
    }

    override fun deleteChange(change: Change) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env)) { conn ->
            sqlExecutor.jdbcTemplate.update(
                    conn, "DELETE FROM " + env.platform.getSchemaPrefix(change.getPhysicalSchema(env))
                    + dbChangeTable + " WHERE " + changeNameColumn + " = ? AND OBJECTNAME = ?", change.changeName, change.objectName)
        }
    }

    override fun deleteObjectChanges(change: Change) {
        sqlExecutor.executeWithinContext(change.getPhysicalSchema(env)) { conn ->
            sqlExecutor.jdbcTemplate.update(
                    conn, "DELETE FROM " + env.platform.getSchemaPrefix(change.getPhysicalSchema(env))
                    + dbChangeTable + " WHERE OBJECTNAME = ? AND CHANGETYPE = ?",
                    change.objectName,
                    change.changeType.name
            )

            // TODO delete this eventually
            sqlExecutor.jdbcTemplate.update(
                    conn, "DELETE FROM " + env.platform.getSchemaPrefix(change.getPhysicalSchema(env))
                    + dbChangeTable + " WHERE OBJECTNAME = ? AND CHANGETYPE = ?",
                    change.objectName,
                    "GRANT"
            )
        }
    }

    private fun updateInternal(conn: Connection, artifact: Change, deployExecution: DeployExecution): Int {
        return sqlExecutor.jdbcTemplate.update(
                conn, "UPDATE " + env.platform.getSchemaPrefix(artifact.getPhysicalSchema(env)) + dbChangeTable
                + " SET " +
                "ARTFTYPE = ?, " +
                "DBSCHEMA = ?, " +
                "ACTIVE = ?, " +
                "CHANGETYPE = ?, " +
                "CONTENTHASH = ?, " +
                rollbackContentColumn + " = ?, " +
                deployUserIdColumn + " = ?, " +
                timeUpdatedColumn + " = ?, " +
                updateDeployExecutionIdColumn + " = ? " +
                "WHERE " + changeNameColumn + " = ? AND OBJECTNAME = ?", if (artifact is ChangeIncremental) "I" else "R", artifact.schema, if (artifact.isActive) 1 else 0, artifact.changeType.name, artifact.contentHash, artifact.rollbackContent, deployUserId, currentTimestamp, deployExecution.id, artifact.changeName, artifact.objectName
        )
    }

    override fun acquireLock(): AuditLock {
        return sqlExecutor.executeWithinContext(env.physicalSchemas.first, ThrowingFunction { sqlExecutor.lock(it) })
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SameSchemaChangeAuditDao::class.java)
        private val TIMESTAMP_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")

        // older version of static data
        private val OLD_STATICDATA_CHANGETYPE = "METADATA"
    }
}
