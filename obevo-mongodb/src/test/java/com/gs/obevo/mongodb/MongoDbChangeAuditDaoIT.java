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
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.gs.obevo.mongodb.impl.MongoClientFactory;
import com.gs.obevo.mongodb.impl.MongoDbChangeAuditDao;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
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
        mydb.getCollection("ARTIFACTDEPLOYMENT").drop();
    }

    @Test
    public void test() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("type");

        Platform platform = mock(Platform.class);
        when(platform.getChangeType(Mockito.anyString())).thenReturn(changeType);

        MongoDbEnvironment env = mock(MongoDbEnvironment.class);
        when(env.getPhysicalSchema("mydb")).thenReturn(new PhysicalSchema("mydb"));
        when(env.getPhysicalSchemas()).thenReturn(Sets.immutable.of(new PhysicalSchema("mydb")));

        MongoDbChangeAuditDao changeAuditDao = new MongoDbChangeAuditDao(mongoClient, env, platform, "test");

        DeployExecutionImpl exec = new DeployExecutionImpl("requester", "executor", "schema", "1.0.0", new Timestamp(new Date().getTime()), false, false, "1.0.0", "reason", Sets.immutable.<DeployExecutionAttribute>empty());
        exec.setId(1L);

        Change change = new ChangeIncremental(changeType, "mydb", "obj1", "c1", 0, "hash", "content");

        changeAuditDao.insertNewChange(change, exec);
        assertEquals(1, changeAuditDao.getDeployedChanges().size());
    }
}