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
package com.gs.obevo.mongodb.impl;

import java.sql.Timestamp;
import java.util.Date;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.ChangeRerunnable;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.gs.obevo.util.knex.InternMap;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.joda.time.DateTime;

public class MongoDbChangeAuditDao implements ChangeAuditDao {
    private final MongoClient mongoClient;
    private final MongoDbEnvironment env;
    private final Platform platform;
    private final String deployUserId;
    private final String changeNameColumn = "changeName";
    private final String changeTypeColumn = "changeType";
    private final String deployUserIdColumn = "deployUserId";
    private final String timeInsertedColumn = "timeInserted";
    private final String timeUpdatedColumn = "timeUpdated";
    private final String rollbackContentColumn = "rollbackContent";
    private final String insertDeployExecutionIdColumn = "insertDeployExecutionId";
    private final String updateDeployExecutionIdColumn = "updateDeployExecutionId";

    public MongoDbChangeAuditDao(MongoClient mongoClient, MongoDbEnvironment env, Platform platform, String deployUserId) {
        this.mongoClient = mongoClient;
        this.env = env;
        this.platform = platform;
        this.deployUserId = deployUserId;
    }

    @Override
    public void init() {
        for (PhysicalSchema physicalSchema : env.getPhysicalSchemas()) {
            MongoDatabase database = mongoClient.getDatabase(physicalSchema.getPhysicalName());
            try {
                database.createCollection(getAuditContainerName());
            } catch (Exception e) {
                // create if it doesn't exist already; TODO clean this up
            }
            MongoCollection<Document> collection = database.getCollection(getAuditContainerName());
            collection.createIndex(Indexes.ascending(changeNameColumn, "OBJECTNAME"));
        }
    }

    @Override
    public String getAuditContainerName() {
        return CHANGE_AUDIT_TABLE_NAME;
    }

    @Override
    public ImmutableList<Change> getDeployedChanges() {
        return env.getPhysicalSchemas().flatCollect(new Function<PhysicalSchema, Iterable<Change>>() {
            @Override
            public Iterable<Change> valueOf(PhysicalSchema physicalSchema) {
                MongoDatabase database = mongoClient.getDatabase(physicalSchema.getPhysicalName());
                MongoCollection<Document> auditCollection = database.getCollection(getAuditContainerName());

                MutableList<Change> changes = iterableToCollection(auditCollection.find()).collect(new Function<Document, Change>() {
                    @Override
                    public Change valueOf(Document doc) {
                        String artfType = doc.getString("ARTFTYPE");
                        Change artf;
                        if (artfType.equals("I")) {
                            artf = new ChangeIncremental();
                        } else if (artfType.equals("R")) {
                            artf = new ChangeRerunnable();
                        } else {
                            throw new IllegalArgumentException("This type does not exist " + artfType);
                        }

                        artf.setChangeName(doc.getString(changeNameColumn));
                        // these are repeated semi-often; hence the intern
                        artf.setObjectName(InternMap.instance().intern(doc.getString("OBJECTNAME")));

                        artf.setActive(doc.getInteger("ACTIVE") == 1);
                        // change METADATA to STATICDATA for backward compatability
                        String changeType = doc.getString("CHANGETYPE");
                        artf.setChangeType(platform.getChangeType(changeType));

                        artf.setContentHash(doc.getString("CONTENTHASH"));
                        // these are repeated often
                        artf.setSchema(InternMap.instance().intern(doc.getString("DBSCHEMA")));

                        artf.setTimeInserted(new Timestamp(doc.getDate(timeInsertedColumn).getTime()));
                        artf.setTimeUpdated(new Timestamp(doc.getDate(timeUpdatedColumn).getTime()));

                        artf.setRollbackContent(doc.getString(rollbackContentColumn));

                        return artf;
                    }
                });

                return changes.toImmutable();
            }
        }).toList().toImmutable();
    }

    @Override
    public void insertNewChange(Change change, DeployExecution deployExecution) {
        MongoCollection<Document> auditCollection = getAuditCollection(change);

        auditCollection.insertOne(createDocFromChange(change, deployExecution, null));
    }

    private Document createDocFromChange(Change change, DeployExecution deployExecution, Date insertTimestamp) {
        Date currentTimestamp = getCurrentTimestamp();
        return new Document()
                .append("ARTFTYPE", change instanceof ChangeIncremental ? "I" : "R")
                .append("DBSCHEMA", change.getSchema())
                .append("ACTIVE", change.isActive() ? 1 : 0)
                .append("CHANGETYPE", change.getChangeType().getName())
                .append("CONTENTHASH", change.getContentHash())
                .append(changeNameColumn, change.getChangeName())
                .append("OBJECTNAME", change.getObjectName())
                .append(rollbackContentColumn, change.getRollbackContent())
                .append(deployUserIdColumn, deployUserId)
                .append(timeInsertedColumn, ObjectUtils.firstNonNull(insertTimestamp, currentTimestamp))
                .append(timeUpdatedColumn, currentTimestamp)
                .append(insertDeployExecutionIdColumn, deployExecution.getId())
                .append(updateDeployExecutionIdColumn, deployExecution.getId());
    }

    private MongoCollection<Document> getAuditCollection(Change change) {
        MongoDatabase database = mongoClient.getDatabase(change.getPhysicalSchema(env).getPhysicalName());
        return database.getCollection(getAuditContainerName());
    }

    private Date getCurrentTimestamp() {
        return new DateTime().toDate();
    }

    @Override
    public void updateOrInsertChange(Change change, DeployExecution deployExecution) {
        MongoCollection<Document> auditCollection = getAuditCollection(change);
        MutableList<Document> docs = iterableToCollection(auditCollection.find(getChangeFilter(change)));

        if (docs.size() > 1) {
            throw new IllegalStateException("Not expecting multiple changes for this key [" + change.getObjectName() + "." + change.getChangeName() + "], but found " + docs);
        } else if (docs.isEmpty()) {
            insertNewChange(change, deployExecution);
        } else {
            Document previousDoc = docs.get(0);
            Date timeInserted = previousDoc.getDate(timeInsertedColumn);
            auditCollection.replaceOne(getChangeFilter(change), createDocFromChange(change, deployExecution, timeInserted));
        }
    }

    private Bson getChangeFilter(Change change) {
        return Filters.and(
                Filters.eq(changeNameColumn, change.getChangeName()),
                Filters.eq("OBJECTNAME", change.getObjectName())
        );
    }

    static <T> MutableList<T> iterableToCollection(FindIterable<T> iterable) {
        MutableList<T> list = Lists.mutable.empty();
        try (MongoCursor<T> iterator = iterable.iterator()) {
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
        }

        return list;
    }

    @Override
    public void deleteChange(Change change) {
        MongoCollection<Document> auditCollection = getAuditCollection(change);
        auditCollection.deleteOne(Filters.and(
                Filters.eq(changeNameColumn, change.getChangeName()),
                Filters.eq("OBJECTNAME", change.getObjectName())
        ));
    }

    @Override
    public void deleteObjectChanges(Change change) {
        MongoCollection<Document> auditCollection = getAuditCollection(change);
        auditCollection.deleteOne(
                Filters.eq("OBJECTNAME", change.getObjectName())
        );
    }
}
