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

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.DeployExecutionAttribute;
import com.gs.obevo.api.appdata.DeployExecutionImpl;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.gs.obevo.mongodb.impl.MongoClientFactory;
import com.gs.obevo.mongodb.impl.MongoDbDeployExecutionDao;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Not yet setup for remote environments")
public class MongoDbDeployExecutionDaoIT {
    private MongoClient mongoClient;

    @Before
    public void setup() {
        this.mongoClient = MongoClientFactory.getInstance().getMongoClient(MongoDbTestHelper.CONNECTION_URI);
        MongoDatabase mydb = mongoClient.getDatabase("mydb");
        mydb.getCollection("ARTIFACTEXECUTION").drop();
    }

    @Test
    public void test() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("type");

        MongoDbPlatform platform = mock(MongoDbPlatform.class);
        when(platform.getChangeType(Mockito.anyString())).thenReturn(changeType);
        when(platform.convertDbObjectName()).thenReturn(StringFunctions.toUpperCase());

        MongoDbEnvironment env = mock(MongoDbEnvironment.class);
        when(env.getPlatform()).thenReturn(platform);
        when(env.getPhysicalSchemas()).thenReturn(Sets.immutable.of(new PhysicalSchema("mydb")));
        when(env.getPhysicalSchema("schema")).thenReturn(new PhysicalSchema("mydb"));

        MongoDbDeployExecutionDao deployExecDao = new MongoDbDeployExecutionDao(mongoClient, env);

        DeployExecutionImpl exec = new DeployExecutionImpl("requester", "executor", "schema", "1.0.0", new Timestamp(new Date().getTime()), false, false, "1.0.0", "reason", Sets.immutable.<DeployExecutionAttribute>empty());

        when(env.getPhysicalSchema("mydb")).thenReturn(new PhysicalSchema("mydb"));

        deployExecDao.persistNew(exec, new PhysicalSchema("mydb"));

        ImmutableCollection<DeployExecution> execs = deployExecDao.getDeployExecutions("mydb");
        assertEquals(1, execs.size());

        deployExecDao.update(execs.getFirst());
    }
}