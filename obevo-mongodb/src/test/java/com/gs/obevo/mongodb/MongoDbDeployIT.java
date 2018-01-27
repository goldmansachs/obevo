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

import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.factory.Obevo;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment;
import com.gs.obevo.mongodb.impl.MongoClientFactory;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import com.mongodb.client.MongoDatabase;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Not yet setup for remote environments")
public class MongoDbDeployIT {
    @Before
    public void setup() {
        MongoDatabase mydb = MongoClientFactory.getInstance().getMongoClient(MongoDbTestHelper.CONNECTION_URI).getDatabase("mydb");
        mydb.getCollection("ARTIFACTDEPLOYMENT").drop();
        mydb.getCollection("ARTIFACTDEPLOYMENT").drop();
        mydb.getCollection("ARTIFACTEXECUTION").drop();
    }

    @Test
    public void deployTest() {
        deployProgrammatically("./src/test/resources/platforms/mongodb/step1");
        deployProgrammatically("./src/test/resources/platforms/mongodb/step2");
    }

    @Test
    public void deployFromFile() {
        deployFromFile("./src/test/resources/platforms/mongodb/step1");
        deployFromFile("./src/test/resources/platforms/mongodb/step2");
    }

    private void deployProgrammatically(String sourcePath) {
        MongoDbEnvironment env = new MongoDbEnvironment();
        env.setPlatform(new MongoDbPlatform());
        env.setSourceDirs(FileRetrievalMode.FILE_SYSTEM.resolveFileObjects(sourcePath));
        env.setSchemas(Sets.immutable.<Schema>of(new Schema("schema1")));
        env.setSchemaNameOverrides(Maps.immutable.of("schema1", "mydb"));
        env.setConnectionURI(MongoDbTestHelper.CONNECTION_URI);
    }

    private void deployFromFile(String sourcePath) {
        ImmutableCollection<MongoDbEnvironment> environments = Obevo.readEnvironments(sourcePath);
        for (MongoDbEnvironment environment : environments) {
            deploy(environment);
        }
    }

    private void deploy(MongoDbEnvironment env) {
        MainDeployerArgs args = new MainDeployerArgs();

        Credential credential = new Credential("a", "b");
        //DeployerAppContext deployerAppContext = new Obevo().buildContext(sourcePath, credential);
        DeployerAppContext deployerAppContext = Obevo.buildContext(env, credential);
        deployerAppContext.deploy(args);
//        deployerAppContext.deploy(changes, args);  // part 2
//        deployerAppContext.deploy(changes, args);  // part 3
        // each context can specify a default file reader context for convenience
        // maybe leave that to platform? I don't want the deploy impl coupled t  the input source type

        /*
        an approach
        obevo.deploy(sourceCode, targetEnv)

1)
        platform = obevo.getPlatform(...)
        context = platform.getcontext();
        context.deploy(sourceCode, targetEnv)

2) context = obevo.getContext(...)

        dbEnvironment
        -permissions
        -connection info
        -csv reading info
        -auto reorg
        -post reorg check stuff


        parts:
        - runtime target information (e.g. connection info)
        - runtime target information (e.g. schema to physical mapping; requires some knowledge of source code)
        - runtime behavior info (e.g. whether to run post-deploy checks)
        - source code definitions (either read in via file or provided programatically)
        - source code core constructs (e.g. schemas)

        Platform "should" be stateless.
        Context uses platform. Platform "should not" use context though there may be exceptions (e.g. try/catch block)

         */
    }
}
