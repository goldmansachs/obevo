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

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.gs.obevo.apps.reveng.AbstractRevengTest;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.mongodb.impl.MongoDbPlatform;
import com.gs.obevo.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Not yet setup for remote environments")
public class MongoDbRevengTest extends AbstractRevengTest {
    @Test
    @Override
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("mydb");
        args.setGenerateBaseline(false);
        args.setJdbcUrl(MongoDbTestHelper.CONNECTION_URI);
        args.setUsername("myuser");
        args.setPassword("mypass");
        args.setCharsetEncoding(StandardCharsets.UTF_8.displayName());

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

//        args.setInputPath(new File("./src/test/resources/reveng/oracle/input.sql"));

        new MongoDbPlatform().getDdlReveng().reveng(args);

        compareOutput(outputDir);
    }

    /**
     * Compares the reverse-engineering output. Set as package-private so that the reverse-engineering integration test
     * can also access this.
     */
    static void compareOutput(File outputDir) {
        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/mongodb/expected"), outputDir, true);
    }
}
