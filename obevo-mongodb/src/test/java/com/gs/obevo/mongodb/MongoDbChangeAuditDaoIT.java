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
package com.gs.obevo.mongodb;

import java.sql.Timestamp;
import java.util.Date;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.gs.obevo.mongodb.impl.MongoClientFactory;
import com.gs.obevo.mongodb.impl.MongoDbChangeAuditDao;
import com.gs.obevo.mongodb.impl.MongoDbDeployExecutionDao;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MongoDbChangeAuditDaoIT {
    private MongoClient mongoClient;

    @Before
    public void setup() {
        this.mongoClient = MongoClientFactory.getInstance().getMongoClient(MongoDbTestHelper.HOST, MongoDbTestHelper.PORT);
        MongoDatabase mydb = mongoClient.getDatabase("mydb");
        mydb.getCollection("ARTIFACTDEPLOYMENT").drop();
        mydb.getCollection("ARTIFACTEXECUTION").drop();
    }

    @Test
    public void test() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("type");

        MongoDbPlatform platform = mock(MongoDbPlatform.class);
        when(platform.getChangeType(Mockito.anyString())).thenReturn(changeType);
        when(platform.convertDbObjectName()).thenReturn(s -> s);

        MongoDbEnvironment env = mock(MongoDbEnvironment.class);
        env = new MongoDbEnvironment();
        Schema schema = new Schema("mydb");
        env.setSchemas(Sets.immutable.of(schema));
        env.setPlatform(platform);

        MongoDbDeployExecutionDao deployExecutionDao = new MongoDbDeployExecutionDao(mongoClient, env);

        // test initial persistence
        deployExecutionDao.persistNew(new DeployExecutionImpl("requester", "executor", schema.getName(), "1.0.0", new Timestamp(new Date().getTime()), false, false, "1.0.0", "reason", Sets.immutable.empty()), env.getPhysicalSchema(schema));
        // do a second deployment to verify incrementing logic
        DeployExecutionImpl exec2 = new DeployExecutionImpl("requester", "executor", schema.getName(), "1.0.0", new Timestamp(new Date().getTime()), false, false, "2.0.0", "reason", Sets.immutable.empty());
        deployExecutionDao.persistNew(exec2, env.getPhysicalSchema(schema));

        MongoDbChangeAuditDao changeAuditDao = new MongoDbChangeAuditDao(mongoClient, env, platform, "test", deployExecutionDao);


        Change change = new ChangeIncremental(changeType, "mydb", "obj1", "c1", 0, "hash", "content");

        changeAuditDao.insertNewChange(change, exec2);
        ImmutableList<Change> deployedChanges = changeAuditDao.getDeployedChanges();
        assertEquals(1, deployedChanges.size());
        assertEquals(2, deployedChanges.get(0).getDeployExecution().getId());
        assertEquals("2.0.0", deployedChanges.get(0).getDeployExecution().getProductVersion());

    }
}