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
package com.gs.obevo.mongodb;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import com.gs.obevo.api.appdata.DeployExecutionAttributeImpl;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployExecutionDao;
import com.gs.obevo.api.platform.Platform;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

import static com.gs.obevo.mongodb.MongoDbChangeAuditDao.iterableToCollection;

public class MongoDbDeployExecutionDao implements DeployExecutionDao {
    private final MongoClient mongoClient;
    private final MongoDbEnvironment env;

    private final String deployExecutionTableName;
    private final String deployExecutionAttributeTableName;
    private final String idColName;
    private final String statusColName;
    private final String deployTimeColName;
    private final String executorIdColName;
    private final String toolVersionColName;
    private final String initCommandColName;
    private final String rollbackCommandColName;
    private final String requesterIdColName;
    private final String reasonColName;
    private final String deployExecutionIdColName;
    private final String attrNameColName;
    private final String attrValueColName;
    private final String productVersionColName;
    private final String dbSchemaColName;
    private final ImmutableMap<PhysicalSchema, MutableInt> nextIdBySchema;

    public MongoDbDeployExecutionDao(MongoClient mongoClient, MongoDbEnvironment env) {
        this.mongoClient = mongoClient;
        this.env = env;
        Platform platform = env.getPlatform();
        Function<String, String> convertDbObjectName = platform.convertDbObjectName();
        this.deployExecutionTableName = convertDbObjectName.valueOf(DEPLOY_EXECUTION_TABLE_NAME);
        this.deployExecutionAttributeTableName = convertDbObjectName.valueOf(DEPLOY_EXECUTION_ATTRIBUTE_TABLE_NAME);
        this.idColName = convertDbObjectName.valueOf("ID");
        this.statusColName = convertDbObjectName.valueOf("STATUS");
        this.deployTimeColName = convertDbObjectName.valueOf("DEPLOYTIME");
        this.executorIdColName = convertDbObjectName.valueOf("EXECUTORID");
        this.toolVersionColName = convertDbObjectName.valueOf("TOOLVERSION");
        this.initCommandColName = convertDbObjectName.valueOf("INIT_COMMAND");
        this.rollbackCommandColName = convertDbObjectName.valueOf("ROLLBACK_COMMAND");
        this.requesterIdColName = convertDbObjectName.valueOf("REQUESTERID");
        this.reasonColName = convertDbObjectName.valueOf("REASON");
        this.productVersionColName = convertDbObjectName.valueOf("PRODUCTVERSION");
        this.dbSchemaColName = convertDbObjectName.valueOf("DBSCHEMA");
//        this.allMainColumns = Lists.immutable.with(idColName, statusColName, deployTimeColName, executorIdColName, toolVersionColName, initCommandColName, rollbackCommandColName, requesterIdColName, reasonColName, dbSchemaColName, productVersionColName);

        this.deployExecutionIdColName = convertDbObjectName.valueOf("DEPLOYEXECUTIONID");
        this.attrNameColName = convertDbObjectName.valueOf("ATTRNAME");
        this.attrValueColName = convertDbObjectName.valueOf("ATTRVALUE");
//        this.allAttrColumns = Lists.immutable.with(deployExecutionIdColName, attrNameColName, attrValueColName);

        this.nextIdBySchema = env.getPhysicalSchemas()
                .toMap(Functions.<PhysicalSchema>getPassThru(), new Function<PhysicalSchema, MutableInt>() {
                    @Override
                    public MutableInt valueOf(PhysicalSchema object) {
                        return new MutableInt(1);
                    }
                })
                .toImmutable();
    }

    @Override
    public void init() {
        for (PhysicalSchema physicalSchema : env.getPhysicalSchemas()) {
            MongoDatabase database = mongoClient.getDatabase(physicalSchema.getPhysicalName());
            try {
                database.createCollection(deployExecutionTableName);
            } catch (Exception e) {
                // create if it doesn't exist already; TODO clean this up
            }
//            database.createCollection(deployExecutionAttributeTableName);
//            MongoCollection<Document> collection = database.getCollection(deployExecutionTableName);
//            collection.createIndex(Indexes.ascending(changeNameColumn, "OBJECTNAME"));

//            nextIdBySchema.get(physicalSchema).setValue(maxId != null ? maxId.longValue() + 1 : 1);
            // TODO set this value from DB
        }

    }

    @Override
    public void persistNew(DeployExecution deployExecution, PhysicalSchema physicalSchema) {
        MongoDatabase database = mongoClient.getDatabase(physicalSchema.getPhysicalName());
        MongoCollection<Document> auditCollection = database.getCollection(deployExecutionTableName);

        MutableInt mutableInt = nextIdBySchema.get(physicalSchema);
        mutableInt.increment();
        ((DeployExecutionImpl)deployExecution).setId(mutableInt.longValue());
        Document doc = getDocumentFromDeployExecution(deployExecution, false);
        auditCollection.insertOne(doc);
    }

    @Override
    public void update(DeployExecution deployExecution) {
        MongoDatabase database = mongoClient.getDatabase(env.getPhysicalSchema(deployExecution.getSchema()).getPhysicalName());
        MongoCollection<Document> auditCollection = database.getCollection(deployExecutionTableName);

        auditCollection.replaceOne(getChangeFilter(deployExecution), getDocumentFromDeployExecution(deployExecution, true));
    }

    private Document getDocumentFromDeployExecution(DeployExecution deployExecution, boolean forUpdate) {
        ImmutableSet<Document> attrs = deployExecution.getAttributes().collect(new Function<DeployExecutionAttribute, Document>() {
            @Override
            public Document valueOf(DeployExecutionAttribute object) {
                return new Document()
                        .append(attrNameColName, object.getName())
                        .append(attrValueColName, object.getValue());
            }
        });

        return new Document()
                .append(idColName, deployExecution.getId())
                .append(requesterIdColName, deployExecution.getRequesterId())
                .append(deployExecutionIdColName, deployExecution.getExecutorId())
                .append(dbSchemaColName, deployExecution.getSchema())
                .append(toolVersionColName, deployExecution.getToolVersion())
                .append(deployTimeColName, new Date(deployExecution.getDeployTime().getTime()))
                .append(initCommandColName, deployExecution.isInit())
                .append(rollbackCommandColName, deployExecution.isRollback())
                .append(productVersionColName, deployExecution.getProductVersion())
                .append(reasonColName, deployExecution.getReason())
                .append("attrs", new ArrayList<>(attrs.toList()));
    }

    private Bson getChangeFilter(DeployExecution deployExecution) {
        return Filters.eq(idColName, deployExecution.getId());
    }

    @Override
    public ImmutableCollection<DeployExecution> getDeployExecutions(String schema) {
        PhysicalSchema physicalSchema = env.getPhysicalSchema(schema);

        MongoDatabase database = mongoClient.getDatabase(physicalSchema.getPhysicalName());
        MongoCollection<Document> auditCollection = database.getCollection(deployExecutionTableName);

        return iterableToCollection(auditCollection.find()).collect(new Function<Document, DeployExecution>() {
            @Override
            public DeployExecution valueOf(Document doc) {
                MutableList<Document> attrsList = ListAdapter.adapt(doc.get("attrs", List.class));
                MutableList<DeployExecutionAttribute> attrs = attrsList.collect(new Function<Document, DeployExecutionAttribute>() {
                    @Override
                    public DeployExecutionAttribute valueOf(Document object) {
                        return new DeployExecutionAttributeImpl(
                                object.getString(attrNameColName),
                                object.getString(attrValueColName)
                        );
                    }
                });

                DeployExecutionImpl exec = new DeployExecutionImpl(
                        doc.getString(requesterIdColName),
                        doc.getString(deployExecutionIdColName),
                        doc.getString(dbSchemaColName),
                        doc.getString(toolVersionColName),
                        new Timestamp(doc.getDate(deployTimeColName).getTime()),
                        doc.getBoolean(initCommandColName),
                        doc.getBoolean(rollbackCommandColName),
                        doc.getString(productVersionColName),
                        doc.getString(reasonColName),
                        attrs.toSet().toImmutable()
                );
                exec.setId(doc.getLong(idColName));

                return exec;
            }
        }).toImmutable();
    }

    @Override
    public DeployExecution getLatestDeployExecution(String schema) {
        ImmutableCollection<DeployExecution> deployExecutions = getDeployExecutions(schema);
        if (deployExecutions.isEmpty()) {
            return null;
        }
        return deployExecutions.maxBy(DeployExecution.TO_ID);
    }

    @Override
    public String getExecutionContainerName() {
        return deployExecutionTableName;
    }

    @Override
    public String getExecutionAttributeContainerName() {
        return deployExecutionAttributeTableName;
    }
}
