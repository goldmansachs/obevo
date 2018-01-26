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
package com.gs.obevo.dist;

import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.factory.Obevo;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.impl.platforms.h2.H2DbPlatform;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;

public class ApiExampleForDocumentation {
    public void fileEnvCreation() {
        // Read the environment from your file system
        DbEnvironment env = Obevo.readEnvironment("./src/test/resources/platforms/hsql");

        // Build the app context - you can pass in credentials via the API if needed
        DeployerAppContext context = Obevo.buildContext(env, "sa", "password");

        // Then invoke the deploy commands.
        context.cleanEnvironment();
        context.deploy();
    }

    public void programmaticEnvCreation() {
        DbEnvironment dbEnv = new DbEnvironment();
        dbEnv.setSourceDirs(Lists.immutable.with(FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/platforms/h2/step1")));
        dbEnv.setName("test");
        dbEnv.setPlatform(new H2DbPlatform());
        dbEnv.setSchemas(Sets.immutable.with(new Schema("SCHEMA1"), new Schema("SCHEMA2")));
        dbEnv.setDbServer("BLAH");

        dbEnv.setSchemaNameOverrides(Maps.immutable.of("SCHEMA1", "bogusSchema"));
        dbEnv.setNullToken("(null)");
        dbEnv.setDataDelimiter('^');


        DeployerAppContext context = Obevo.buildContext(dbEnv, new Credential("sa", ""));

        context.setupEnvInfra();
        context.cleanEnvironment();
        context.deploy();

    }
}
