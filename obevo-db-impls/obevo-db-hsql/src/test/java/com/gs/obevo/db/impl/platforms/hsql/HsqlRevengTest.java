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
package com.gs.obevo.db.impl.platforms.hsql;

import java.io.File;

import com.gs.obevo.apps.reveng.AbstractRevengTest;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class HsqlRevengTest extends AbstractRevengTest {
    @Test
    @Override
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("MYSCHEMA01");
        args.setGenerateBaseline(false);
        args.setJdbcUrl("jdbc:hsqldb:mem:hsqldbreveng");
        args.setUsername("myuser");
        args.setPassword("mypass");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        args.setInputPath(new File("./src/test/resources/reveng/hsql/input.sql"));

        new HsqlDbPlatform().getDdlReveng().reveng(args);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/hsql/expected"), new File(outputDir, "final"));

        // Ensure that we can still build the schema that was reverse engineered
        DbEnvironment prod = DbEnvironmentFactory.getInstance().readOneFromSourcePath(new File(outputDir, "final").getAbsolutePath(), "prod");
        prod.setCleanBuildAllowed(true);
        prod.buildAppContext("sa", "")
                .setupEnvInfra()
                .cleanEnvironment()
                .deploy();
    }
}
