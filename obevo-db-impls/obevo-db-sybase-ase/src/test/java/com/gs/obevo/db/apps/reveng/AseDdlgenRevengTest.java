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
package com.gs.obevo.db.apps.reveng;

import java.io.File;

import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class AseDdlgenRevengTest {
    @Test
    public void testInstructions() throws Exception {
        File outputDir = new File("./target/ddlgen/instructions");
        FileUtils.deleteDirectory(outputDir);

        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("dbdeploy01");
        args.setGenerateBaseline(false);
        args.setOutputPath(outputDir);

        new AseDdlgenReveng().reveng(args);
    }

    @Test
    public void testFullReveng() throws Exception {
        File outputDir = new File("./target/ddlreveng/execute");
        FileUtils.deleteDirectory(outputDir);

        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("dbdeploy01");
        args.setInputPath(new File("./src/test/resources/reveng/ddlgen/input/ase-ddlgen-input.txt"));
        args.setGenerateBaseline(false);
        args.setOutputPath(outputDir);

        new AseDdlgenReveng().reveng(args);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/ddlgen/expected"), new File(outputDir, "final"));
    }
}
