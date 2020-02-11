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

import com.gs.obevo.api.appdata.DeployExecution
import com.gs.obevo.api.appdata.DeployExecutionAttributeImpl
import com.gs.obevo.api.appdata.DeployExecutionImpl
import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.api.platform.DeployExecutionDao
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment
import com.mongodb.MongoClient
import com.mongodb.client.model.Filters
import org.apache.commons.lang3.mutable.MutableInt
import org.bson.Document
import org.bson.conversions.Bson
import org.eclipse.collections.api.collection.ImmutableCollection
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Sets
import java.sql.Timestamp
import java.util.Date

class MongoDbDeployExecutionDao(private val mongoClient: MongoClient, private val env: MongoDbEnvironment) : DeployExecutionDao {
    private val deployExecutionTableName: String
    private val deployExecutionAttributeTableName: String
    private val idColName: String
    private val statusColName: String
    private val deployTimeColName: String
    private val executorIdColName: String
    private val toolVersionColName: String
    private val initCommandColName: String
    private val rollbackCommandColName: String
    private val requesterIdColName: String
    private val reasonColName: String
    private val deployExecutionIdColName: String
    private val attrNameColName: String
    private val attrValueColName: String
    private val productVersionColName: String
    private val dbSchemaColName: String
    private val nextIdBySchema: Map<PhysicalSchema, MutableInt>

    init {
        val platform = env.platform
        val convertDbObjectName = platform.convertDbObjectName()
        this.deployExecutionTableName = convertDbObjectName.valueOf(DeployExecutionDao.DEPLOY_EXECUTION_TABLE_NAME)
        this.deployExecutionAttributeTableName = convertDbObjectName.valueOf(DeployExecutionDao.DEPLOY_EXECUTION_ATTRIBUTE_TABLE_NAME)
        this.idColName = convertDbObjectName.valueOf("ID")
        this.statusColName = convertDbObjectName.valueOf("STATUS")
        this.deployTimeColName = convertDbObjectName.valueOf("DEPLOYTIME")
        this.executorIdColName = convertDbObjectName.valueOf("EXECUTORID")
        this.toolVersionColName = convertDbObjectName.valueOf("TOOLVERSION")
        this.initCommandColName = convertDbObjectName.valueOf("INIT_COMMAND")
        this.rollbackCommandColName = convertDbObjectName.valueOf("ROLLBACK_COMMAND")
        this.requesterIdColName = convertDbObjectName.valueOf("REQUESTERID")
        this.reasonColName = convertDbObjectName.valueOf("REASON")
        this.productVersionColName = convertDbObjectName.valueOf("PRODUCTVERSION")
        this.dbSchemaColName = convertDbObjectName.valueOf("DBSCHEMA")
        //        this.allMainColumns = Lists.immutable.with(idColName, statusColName, deployTimeColName, executorIdColName, toolVersionColName, initCommandColName, rollbackCommandColName, requesterIdColName, reasonColName, dbSchemaColName, productVersionColName);

        this.deployExecutionIdColName = convertDbObjectName.valueOf("DEPLOYEXECUTIONID")
        this.attrNameColName = convertDbObjectName.valueOf("ATTRNAME")
        this.attrValueColName = convertDbObjectName.valueOf("ATTRVALUE")
        //        this.allAttrColumns = Lists.immutable.with(deployExecutionIdColName, attrNameColName, attrValueColName);

        this.nextIdBySchema = env.physicalSchemas.associateWith { MutableInt(0) }
    }

    override fun init() {
        for ((physicalName) in env.physicalSchemas) {
            val database = mongoClient.getDatabase(physicalName)
            try {
                database.createCollection(deployExecutionTableName)
            } catch (e: Exception) {
                // create if it doesn't exist already; TODO clean this up
            }

            //            database.createCollection(deployExecutionAttributeTableName);
            //            MongoCollection<Document> collection = database.getCollection(deployExecutionTableName);
            //            collection.createIndex(Indexes.ascending(changeNameColumn, "OBJECTNAME"));

            //            nextIdBySchema.get(physicalSchema).setValue(maxId != null ? maxId.longValue() + 1 : 1);
            // TODO set this value from DB
        }
    }

    override fun persistNew(deployExecution: DeployExecution, physicalSchema: PhysicalSchema) {
        val database = mongoClient.getDatabase(physicalSchema.physicalName)
        val auditCollection = database.getCollection(deployExecutionTableName)

        val mutableInt = nextIdBySchema.getValue(physicalSchema)
        mutableInt.increment()
        (deployExecution as DeployExecutionImpl).id = mutableInt.toLong()
        val doc = getDocumentFromDeployExecution(deployExecution, false)
        auditCollection.insertOne(doc)
    }

    override fun update(deployExecution: DeployExecution) {
        val database = mongoClient.getDatabase(env.getPhysicalSchema(deployExecution.schema).physicalName)
        val auditCollection = database.getCollection(deployExecutionTableName)

        auditCollection.replaceOne(getChangeFilter(deployExecution), getDocumentFromDeployExecution(deployExecution, true))
    }

    private fun getDocumentFromDeployExecution(deployExecution: DeployExecution, forUpdate: Boolean): Document {
        val attrs = deployExecution.attributes.map {
            Document().append(attrNameColName, it.name).append(attrValueColName, it.value)
        }

        return Document()
                .append(idColName, deployExecution.id)
                .append(requesterIdColName, deployExecution.requesterId)
                .append(deployExecutionIdColName, deployExecution.executorId)
                .append(dbSchemaColName, deployExecution.schema)
                .append(toolVersionColName, deployExecution.toolVersion)
                .append(deployTimeColName, Date(deployExecution.deployTime.time))
                .append(initCommandColName, deployExecution.isInit)
                .append(rollbackCommandColName, deployExecution.isRollback)
                .append(productVersionColName, deployExecution.productVersion)
                .append(reasonColName, deployExecution.reason)
                .append("attrs", attrs)
    }

    private fun getChangeFilter(deployExecution: DeployExecution): Bson {
        return Filters.eq(idColName, deployExecution.id)
    }

    override fun getDeployExecutions(schema: String): ImmutableCollection<DeployExecution> {
        val (physicalName) = env.getPhysicalSchema(schema)

        val database = mongoClient.getDatabase(physicalName)
        val auditCollection = database.getCollection(deployExecutionTableName)

        val execs = auditCollection.find(Filters.eq(dbSchemaColName, schema)).map { doc ->
            val attrsList = doc.get("attrs") as List<Document>
            val attrs = attrsList.map {
                DeployExecutionAttributeImpl(it.getString(attrNameColName), it.getString(attrValueColName))
            }

            val exec = DeployExecutionImpl(
                    doc.getString(requesterIdColName),
                    doc.getString(deployExecutionIdColName),
                    doc.getString(dbSchemaColName),
                    doc.getString(toolVersionColName),
                    Timestamp(doc.getDate(deployTimeColName).time),
                    doc.getBoolean(initCommandColName)!!,
                    doc.getBoolean(rollbackCommandColName)!!,
                    doc.getString(productVersionColName),
                    doc.getString(reasonColName),
                    Sets.immutable.ofAll(attrs.toSet())
            )
            exec.id = doc.getLong(idColName)!!

            exec
        }
        return Lists.immutable.ofAll(execs)
    }

    override fun getLatestDeployExecution(schema: String): DeployExecution? {
        val deployExecutions = getDeployExecutions(schema)
        return if (deployExecutions.isEmpty) {
            null
        } else deployExecutions.maxBy { it.id }
    }

    override fun getExecutionContainerName(): String {
        return deployExecutionTableName
    }

    override fun getExecutionAttributeContainerName(): String {
        return deployExecutionAttributeTableName
    }
}
