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
package com.gs.obevo.db.impl.platforms.oracle;

import java.io.File;

import com.gs.obevo.db.apps.reveng.AbstractDdlRevengTest;
import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * Test for Oracle reverse-engineering based, assuming that the content from DB is already provided.
 * We have this unit test so that we can verify this test for all builds, as we don't have the Oracle DB setup for
 * integration testing yet.
 */
public class OracleRevengTest extends AbstractDdlRevengTest {
    @Test
    @Override
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("DBDEPLOY01");
        args.setGenerateBaseline(false);
        args.setJdbcUrl("jdbc:oracle:thin:@localhost:1521/ORCLPDB1");
        args.setUsername("myuser");
        args.setPassword("mypass");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        args.setInputPath(new File("./src/test/resources/reveng/oracle/input.sql"));

        new OracleDbPlatform().getDdlReveng().reveng(args);

        compareOutput(new File(outputDir, "final"));
    }

    /**
     * Compares the reverse-engineering output. Set as package-private so that the reverse-engineering integration test
     * can also access this.
     */
    static void compareOutput(File outputDir) {
        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/oracle/expected"), outputDir, true);
    }
}
