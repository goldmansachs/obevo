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
package com.gs.obevo.impl;

import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

public class DisabledOnboardingStrategyTest {
    private final OnboardingStrategy strategy = new DisabledOnboardingStrategy();
    private final FileObject rootDir = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/OnboardingStrategyTests");
    private final String EXCEPTION = "testExceptionDirs";
    private final String EXCEPTION2 = "testExceptionDirs2";
    private final String NOEXCEPTION = "testNoExceptionDirs";
    private final String ANALYZEFOLDER = "testAnalyzeFolder";
    private final String OTHER = "otherDir";

    @Test(expected = IllegalArgumentException.class)
    public void validateShouldFailInputDirsWithExceptions() throws Exception {
        strategy.validateSourceDirs(Lists.mutable.with(rootDir), Sets.immutable.of(EXCEPTION, OTHER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateShouldFailInputDirsWithRevengAnalysisFolders() throws Exception {
        strategy.validateSourceDirs(Lists.mutable.with(rootDir), Sets.immutable.of(ANALYZEFOLDER, OTHER));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateShouldFailInputDirsWithExceptionsInSubdir() throws Exception {
        strategy.validateSourceDirs(Lists.mutable.with(rootDir), Sets.immutable.of(EXCEPTION2, OTHER));
    }

    @Test
    public void validateShouldPassInputDirsWithoutExceptions() throws Exception {
        // note - there are exceptions in other folders under the root, but we only check the schema folders
        strategy.validateSourceDirs(Lists.mutable.with(rootDir), Sets.immutable.of(NOEXCEPTION, OTHER));
    }
}
