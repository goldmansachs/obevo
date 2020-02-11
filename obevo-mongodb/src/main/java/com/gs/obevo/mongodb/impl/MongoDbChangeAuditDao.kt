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
package com.gs.obevo.mongodb.impl

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.ChangeIncremental
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.ChangeRerunnable
import com.gs.obevo.api.appdata.DeployExecution
import com.gs.obevo.api.platform.AuditLock
import com.gs.obevo.api.platform.ChangeAuditDao
import com.gs.obevo.api.platform.DeployExecutionDao
import com.gs.obevo.api.platform.Platform
import com.gs.obevo.impl.changeauditdao.InMemLock
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment
import com.gs.obevo.util.knex.InternMap
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoIterable
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import org.apache.commons.lang3.ObjectUtils
import org.bson.Document
import org.bson.conversions.Bson
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.api.list.MutableList
import org.eclipse.collections.impl.factory.Lists
import org.joda.time.DateTime
import java.sql.Timestamp
import java.util.Date


class MongoDbChangeAuditDao(private val mongoClient: MongoClient, private val env: MongoDbEnvironment, private val platform: Platform, private val deployUserId: String, private val deployExecutionDao: DeployExecutionDao) : ChangeAuditDao {
    private val deployBehavior: MongoDeployBehavior = MongoDeployBehavior(env)
    private val changeNameColumn = "changeName"
    private val changeTypeColumn = "changeType"
    private val dbSchemaColumn = "DBSCHEMA"
    private val deployUserIdColumn = "deployUserId"
    private val timeInsertedColumn = "timeInserted"
    private val timeUpdatedColumn = "timeUpdated"
    private val rollbackContentColumn = "rollbackContent"
    private val insertDeployExecutionIdColumn = "insertDeployExecutionId"
    private val updateDeployExecutionIdColumn = "updateDeployExecutionId"

    private val currentTimestamp: Date
        get() = DateTime().toDate()

    override fun init() {
        deployBehavior.validateMongoEnvironmentSetup()
        for ((physicalName) in env.physicalSchemas) {
            val database = mongoClient.getDatabase(physicalName)
            try {
                database.createCollection(auditContainerName)
            } catch (e: Exception) {
                // create if it doesn't exist already; TODO clean this up
            }

            val collection = database.getCollection(auditContainerName)
            collection.createIndex(Indexes.ascending(changeNameColumn, "OBJECTNAME"))
        }
    }

    override fun getAuditContainerName(): String {
        return ChangeAuditDao.CHANGE_AUDIT_TABLE_NAME
    }

    override fun getDeployedChanges(): ImmutableList<Change> {
        val artfs = env.schemaNames.flatMap { schema ->
            val deployExecutionsById = deployExecutionDao.getDeployExecutions(schema).groupByUniqueKey(DeployExecution::getId)
            val database = mongoClient.getDatabase(env.getPhysicalSchema(schema).physicalName)
            val auditCollection = database.getCollection(auditContainerName)

            val changes = auditCollection.find(Filters.eq(dbSchemaColumn, schema)).map { doc ->
                val artfType = doc.getString("ARTFTYPE")
                val artf: Change
                if (artfType == "I") {
                    artf = ChangeIncremental()
                } else if (artfType == "R") {
                    artf = ChangeRerunnable()
                } else {
                    throw IllegalArgumentException("This type does not exist $artfType")
                }

                val schema = InternMap.instance().intern(doc.getString(dbSchemaColumn))
                artf.changeKey = ChangeKey(
                        schema,
                        platform.getChangeType(doc.getString("CHANGETYPE")),
                        InternMap.instance().intern(doc.getString("OBJECTNAME")),
                        doc.getString(changeNameColumn)
                )

                artf.isActive = doc.getInteger("ACTIVE") == 1
                artf.contentHash = doc.getString("CONTENTHASH")
                artf.timeInserted = Timestamp(doc.getDate(timeInsertedColumn).time)
                artf.timeUpdated = Timestamp(doc.getDate(timeUpdatedColumn).time)
                artf.deployExecution = deployExecutionsById.get(doc.getLong(updateDeployExecutionIdColumn)!!)

                artf.rollbackContent = doc.getString(rollbackContentColumn)

                artf
            }
            changes

        }.toList()
        return Lists.immutable.ofAll(artfs)
    }

    override fun insertNewChange(change: Change, deployExecution: DeployExecution) {
        val auditCollection = getAuditCollection(change)

        auditCollection.insertOne(createDocFromChange(change, deployExecution, null))
    }

    private fun createDocFromChange(change: Change, deployExecution: DeployExecution, insertTimestamp: Date?): Document {
        val currentTimestamp = currentTimestamp
        return Document()
                .append("ARTFTYPE", if (change is ChangeIncremental) "I" else "R")
                .append(dbSchemaColumn, change.schema)
                .append("ACTIVE", if (change.isActive) 1 else 0)
                .append("CHANGETYPE", change.changeType.name)
                .append("CONTENTHASH", change.contentHash)
                .append(changeNameColumn, change.changeName)
                .append("OBJECTNAME", change.objectName)
                .append(rollbackContentColumn, change.rollbackContent)
                .append(deployUserIdColumn, deployUserId)
                .append(timeInsertedColumn, ObjectUtils.firstNonNull<Date>(insertTimestamp, currentTimestamp))
                .append(timeUpdatedColumn, currentTimestamp)
                .append(insertDeployExecutionIdColumn, deployExecution.id)
                .append(updateDeployExecutionIdColumn, deployExecution.id)
    }

    private fun getAuditCollection(change: Change): MongoCollection<Document> {
        val database = mongoClient.getDatabase(change.getPhysicalSchema(env).physicalName)
        return database.getCollection(auditContainerName)
    }

    override fun updateOrInsertChange(change: Change, deployExecution: DeployExecution) {
        val auditCollection = getAuditCollection(change)
        val docs = iterableToCollection(auditCollection.find(getChangeFilter(change)))

        if (docs.size > 1) {
            throw IllegalStateException("Not expecting multiple changes for this key [" + change.objectName + "." + change.changeName + "], but found " + docs)
        } else if (docs.isEmpty) {
            insertNewChange(change, deployExecution)
        } else {
            val previousDoc = docs[0]
            val timeInserted = previousDoc.getDate(timeInsertedColumn)
            auditCollection.replaceOne(getChangeFilter(change), createDocFromChange(change, deployExecution, timeInserted))
        }
    }

    private fun getChangeFilter(change: Change): Bson {
        return Filters.and(
                Filters.eq(changeNameColumn, change.changeName),
                Filters.eq("OBJECTNAME", change.objectName)
        )
    }

    override fun deleteChange(change: Change) {
        val auditCollection = getAuditCollection(change)
        auditCollection.deleteOne(Filters.and(
                Filters.eq(changeNameColumn, change.changeName),
                Filters.eq("OBJECTNAME", change.objectName)
        ))
    }

    override fun deleteObjectChanges(change: Change) {
        val auditCollection = getAuditCollection(change)
        auditCollection.deleteOne(
                Filters.eq("OBJECTNAME", change.objectName)
        )
    }

    override fun acquireLock(): AuditLock {
        return InMemLock()
    }

    companion object {
        @JvmStatic
        internal fun <T> iterableToCollection(iterable: MongoIterable<T>): MutableList<T> {
            val list = Lists.mutable.empty<T>()
            iterable.iterator().use { iterator ->
                while (iterator.hasNext()) {
                    list.add(iterator.next())
                }
            }

            return list
        }
    }
}
