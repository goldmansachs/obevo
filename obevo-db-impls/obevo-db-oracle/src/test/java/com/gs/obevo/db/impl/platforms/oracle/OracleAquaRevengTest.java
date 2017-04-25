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

import com.gs.obevo.db.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("need to implement this test")
public class OracleAquaRevengTest {

    @Test
    public void testReveng2() throws Exception {
        File outputDir = new File("./target/reveng/oracle1");
        FileUtils.deleteDirectory(outputDir);

        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("MYSCHEMA01");
        args.setInputPath(new File("./src/test/resources/reveng/oracle/aquaexample1"));
        args.setGenerateBaseline(false);
        args.setOutputPath(outputDir);
        new OracleAquaReveng().reveng(args);

        // TODO need to add Oracle reverse engineering examples
        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/oracle/aqua/expected"), outputDir);
    }
}
