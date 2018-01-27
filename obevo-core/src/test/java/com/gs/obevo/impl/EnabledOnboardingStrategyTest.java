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
package com.gs.obevo.impl;

import java.io.File;
import java.io.IOException;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnabledOnboardingStrategyTest {
    private final OnboardingStrategy strategy = new EnabledOnboardingStrategy();
    private final FileObject exceptionDir = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/OnboardingStrategyTests/testExceptionDirs");
    private final FileObject noExceptionDir = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/OnboardingStrategyTests/testNoExceptionDirs");
    private final FileObject otherDir = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/OnboardingStrategyTests/otherDir");

    private File testDir;

    @Before
    public void setup() {
        testDir = new File("./target/EnabledOnboardingTest");
        FileUtils.deleteQuietly(testDir);
    }

    @Test
    public void validateShouldAlwaysPassInputRegardlessOfExceptions() throws Exception {
        strategy.validateSourceDirs(Lists.mutable.with(exceptionDir, otherDir, noExceptionDir), Sets.immutable.of("testExceptionDirs"));
    }

    @Test
    public void successShouldMoveFileFromExceptionFolderToRegularFolder() throws Exception {
        strategy.handleSuccess(newChange("mydir2/exceptions/myfile.sql"));

        // Verify that the file has been moved to the correct folder and exception file created
        assertTrue(new File(testDir, "mydir2/myfile.sql").exists());
    }

    @Test
    public void exceptionShouldMoveFileFromRegularFolderToExceptionFolder() throws Exception {
        strategy.handleException(newChange("mydir/myfile.sql"), new Exception(), Sets.mutable.<String>of());

        // Verify that the file has been moved to the correct folder and exception file created
        assertTrue(new File(testDir, "mydir/exceptions/myfile.sql").exists());
        assertTrue(new File(testDir, "mydir/exceptions/myfile.sql.exception").exists());
    }

    @Test
    public void exceptionShouldMoveFileFromValidateToExceptionFolder() throws Exception {
        strategy.handleException(newChange("mydir3/" + OnboardingStrategy.DEPENDENT_EXCEPTION_DIR + "/myfile.sql"), new Exception(), Sets.mutable.<String>of());

        assertTrue(new File(testDir, "mydir3/exceptions/myfile.sql").exists());
        assertTrue(new File(testDir, "mydir3/exceptions/myfile.sql.exception").exists());
    }

    private Change newChange(String fileLocation) throws IOException {
        File file = new File(testDir, fileLocation);
        file.getParentFile().mkdirs();
        file.createNewFile();
        final FileObject fileObject = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject(file.getAbsolutePath());

        Change change = mock(Change.class);
        when(change.getFileLocation()).thenReturn(fileObject);
        return change;
    }
}
