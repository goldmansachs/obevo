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
package com.gs.obevo.db.apps.reveng;

import java.io.File;

import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import com.gs.obevo.util.ArgsParser;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static java.lang.String.format;

public class AseAquaRevengMainTest {

    @Test
    public void testAse() {
        AquaRevengMain reveng = new AquaRevengMain();

        File input = new File("./src/test/resources/reveng/ase/input");
        File outputDir = new File("./target/reveng-test/ase");
        FileUtils.deleteQuietly(outputDir);
        File expected = new File("./src/test/resources/reveng/ase/expected");

        String argsStr = format("-mode schema -inputDir %s -outputDir %s -generateBaseline -dbType %s -dbSchema %s",
                input, outputDir, "SYBASE_ASE", "dbdeploy01");
        AquaRevengArgs args = new ArgsParser().parse(argsStr.split(" "), new AquaRevengArgs());

        reveng.execute(args);

        DirectoryAssert.assertDirectoriesEqual(expected, new File(outputDir, "final"));
    }

    @Test
    public void testAseWithIndex() {
        AquaRevengMain reveng = new AquaRevengMain();

        File input = new File("./src/test/resources/reveng/ase/input-with-index");
        File outputDir = new File("./target/reveng-test/ase/output-with-index");
        FileUtils.deleteQuietly(outputDir);
        File expected = new File("./src/test/resources/reveng/ase/expected-withindexes");

        String argsStr = format("-mode schema -inputDir %s -outputDir %s -generateBaseline -dbType %s -dbSchema %s",
                input, outputDir, "SYBASE_ASE", "dbdeploy01");
        AquaRevengArgs args = new ArgsParser().parse(argsStr.split(" "), new AquaRevengArgs());

        reveng.execute(args);

        DirectoryAssert.assertDirectoriesEqual(expected, new File(outputDir, "final"));
    }
}
