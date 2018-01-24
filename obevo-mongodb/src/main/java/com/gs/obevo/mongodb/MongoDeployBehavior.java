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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.api.platform.CommandExecutionContext;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDeployBehavior implements ChangeTypeBehavior {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDeployBehavior.class);

    private final MongoClient mongoClient;
    private final Environment env;

    public MongoDeployBehavior(MongoClient mongoClient, Environment env) {
        this.mongoClient = mongoClient;
        this.env = env;
    }

    @Override
    public void deploy(Change change, CommandExecutionContext cec) {
        MongoDatabase database = mongoClient.getDatabase(change.getPhysicalSchema(env).getPhysicalName());
        final BasicDBObject command = new BasicDBObject();
        command.put("eval", change.getContent());
        Document result = database.runCommand(command);
        LOG.info("Result: {}", result);
    }

    @Override
    public void undeploy(Change change) {

    }

    @Override
    public void dropObject(Change change, boolean dropForRecreate) {

    }

    @Override
    public String getDefinitionFromEnvironment(Change exampleChange) {
        return null;
    }
}
