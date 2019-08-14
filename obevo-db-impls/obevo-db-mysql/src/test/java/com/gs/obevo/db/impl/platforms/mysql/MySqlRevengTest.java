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
package com.gs.obevo.db.impl.platforms.mysql;

import java.io.File;

import com.gs.obevo.apps.reveng.AbstractRevengTest;
import com.gs.obevo.apps.reveng.AquaRevengArgs;
import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Test not working yet")
public class MySqlRevengTest extends AbstractRevengTest {
    @Test
    @Override
    public void testReverseEngineeringFromFile() throws Exception {
        AquaRevengArgs args = new AquaRevengArgs();
        args.setDbSchema("myschema01");
        args.setGenerateBaseline(false);
        args.setDbHost("myhost.me.com");
        args.setDbPort(1234);
        args.setDbServer("myserver");
        args.setUsername("myuser");
        args.setPassword("mypass");

        File outputDir = new File("./target/outputReveng");
        FileUtils.deleteDirectory(outputDir);
        args.setOutputPath(outputDir);

        args.setInputPath(new File("./src/test/resources/reveng/pgdump/input/input.sql"));

        new MySqlDbPlatform().getDdlReveng().reveng(args);

        DirectoryAssert.assertDirectoriesEqual(new File("./src/test/resources/reveng/pgdump/expected"), new File(outputDir, "final"));
    }
}
