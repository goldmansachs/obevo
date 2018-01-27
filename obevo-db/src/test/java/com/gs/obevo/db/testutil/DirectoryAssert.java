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
package com.gs.obevo.db.testutil;

import java.io.File;

import junitx.framework.FileAssert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

import static org.junit.Assert.assertEquals;

public class DirectoryAssert {
    private static final IOFileFilter DIR_FILE_FILTER = FileFilterUtils.makeCVSAware(FileFilterUtils
            .makeSVNAware(DirectoryFileFilter.INSTANCE));

    /**
     * Primitive DB comparison method
     * We just compare file names, not the subdirecory structure also
     * (so this would fail if multiple subdirectories had the same file name)
     */
    public static void assertDirectoriesEqual(File expected, File actual) {
        MutableList<File> expectedFiles = FastList.newList(FileUtils.listFiles(expected, new WildcardFileFilter("*"),
                DIR_FILE_FILTER));
        expectedFiles = expectedFiles.sortThisBy(toRelativePath(expected));
        MutableList<File> actualFiles = FastList.newList(FileUtils.listFiles(actual, new WildcardFileFilter("*"),
                DIR_FILE_FILTER));
        actualFiles = actualFiles.sortThisBy(toRelativePath(actual));

        assertEquals(
                String.format("Directories did not have same # of files:\nExpected: %1$s\nbut was: %2$s",
                        expectedFiles.makeString("\n"), actualFiles.makeString("\n")),
                expectedFiles.size(), actualFiles.size());
        for (int i = 0; i < expectedFiles.size(); i++) {
            File expectedFile = expectedFiles.get(i);
            File actualFile = actualFiles.get(i);

            String expectedFilePath = getRelativePath(expectedFile, expected);
            String actualFilePath = getRelativePath(actualFile, actual);
            System.out.println("Comparing" + expectedFilePath + " vs " + actualFilePath);

            assertEquals("File " + i + " [" + expectedFile + " vs " + actualFile
                    + " does not match paths relative from their roots", expectedFilePath, actualFilePath);
            FileAssert.assertEquals("Mismatch on file " + expectedFile.getAbsolutePath(), expectedFile, actualFile);
        }
    }

    private static String getRelativePath(File childFile, File baseFile) {
        if (childFile == null) {
            throw new IllegalArgumentException("childFile was not a child of the base file");
        } else if (childFile.equals(baseFile)) {
            return "";
        } else {
            return getRelativePath(childFile.getParentFile(), baseFile) + "/" + childFile.getName();
        }
    }

    private static Function<File, String> toRelativePath(final File baseFile) {
        return new Function<File, String>() {
            @Override
            public String valueOf(File object) {
                return getRelativePath(object, baseFile);
            }
        };
    }
}
