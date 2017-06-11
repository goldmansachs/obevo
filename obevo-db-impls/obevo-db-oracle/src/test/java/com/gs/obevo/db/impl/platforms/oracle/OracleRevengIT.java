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
package com.gs.obevo.db.impl.platforms.oracle;

import java.io.File;

import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Not supporting reverse-engineering tests at this point, and Oracle tests can't run in build due to proprietary driver")
public class OracleRevengIT {
    @Test
    public void testReveng() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("DBDEPLOY01");
        //args.setInputPath(new File("./src/test/resources/reveng/db2look/input/db2input.txt"));
        args.setGenerateBaseline(false);
        args.setJdbcUrl("jdbc:oracle:thin:@dbdeploy-oracle-12-1.c87tzbeo5ssa.us-west-2.rds.amazonaws.com:1521:DBDEPLOY");
        args.setUsername("deploybuilddbo");
        args.setPassword("deploybuilddb0");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        new OracleReveng().reveng(args);
    }

    @Test
    public void testRedeploy() {
        File outputDir = new File("./target/outputReveng");
        DbEnvironment prod = DbEnvironmentFactory.getInstance().readOneFromSourcePath(outputDir.getPath(), "prod");
        prod.setCleanBuildAllowed(true);
        prod.buildAppContext("deploybuilddbo", "deploybuilddb0")
                .cleanEnvironment()
                .deploy();

    }
}
