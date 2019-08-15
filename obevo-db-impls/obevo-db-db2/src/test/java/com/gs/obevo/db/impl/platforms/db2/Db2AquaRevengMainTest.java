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
package com.gs.obevo.db.impl.platforms.db2;

import java.io.File;

import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.apps.reveng.AquaRevengMain;
import com.gs.obevo.testutil.DirectoryAssert;
import com.gs.obevo.util.ArgsParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class Db2AquaRevengMainTest {
    @Test
    public void testDb2() {
        AquaRevengMain reveng = new AquaRevengMain();

        File input = new File("./src/test/resources/reveng/db2/input");
        File outputDir = new File("./target/reveng-test/db2");
        FileUtils.deleteQuietly(outputDir);
        File expected = new File("./src/test/resources/reveng/db2/expected");

        String argsStr = String
                .format("-mode schema -inputDir %s -outputDir %s -dbSchema RPTSNAP -tablespaceToken -tokenizeDefaultSchema -generateBaseline -dbType %s",
                        input, outputDir, "DB2");
        AquaRevengArgs args = new ArgsParser().parse(argsStr.split(" "), new AquaRevengArgs());

        reveng.execute(args);

        DirectoryAssert.assertDirectoriesEqual(expected, outputDir);
    }
}
