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

import com.gs.obevo.db.testutil.DirectoryAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

public class DbFileMergerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void basicMergeTest() {
        FileUtils.deleteQuietly(new File("./target/merge/unittest"));

        DbFileMerger.main(("-dbMergeConfigFile src/test/resources/scenariotests/reveng-merge/merge-config.txt " +
                "-outputDir ./target/merge/unittest").split(" "));

        DirectoryAssert.assertDirectoriesEqual(
                new File("./src/test/resources/scenariotests/reveng-merge/expected")
                , new File("./target/merge/unittest")
        );
    }

    @Test
    public void inputValidationTest() {
        thrown.expectCause(hasMessage(containsString("db1.inputDir file (use forward-slash")));
        thrown.expectCause(hasMessage(containsString("db2.inputDir file")));
        FileUtils.deleteQuietly(new File("./target/merge/unittest"));

        DbFileMerger.main(("-dbMergeConfigFile src/test/resources/scenariotests/reveng-merge/merge-config-inputerror.txt " +
                "-outputDir ./target/merge/unittest").split(" "));
    }
}
