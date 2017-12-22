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
package com.gs.obevo.util.vfs;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.gs.obevo.util.vfs.FileFilterUtils.vcsAware;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileRetrievalModeTest {
    private static final Function<FileObject, String> getFilePath = new Function<FileObject, String>() {
        @Override
        public String valueOf(FileObject object) {
            return object.getURLDa().getPath();
        }
    };

    @Test
    public void testShouldRetrieveAllFoldersAvailableInClassPath() {
        ListIterable<FileObject> sourceDirs = FileRetrievalMode.CLASSPATH.resolveFileObjects("vfs/classpathtest");

        // verify that we can find the same directories if they exist under different classpaths
        assertThat(sourceDirs.collect(getFilePath), containsInAnyOrder(
                endsWith("obevo-core/target/test-classes/vfs/classpathtest"),
                allOf(containsString("obevo-internal-test-client-01"), containsString("vfs/classpathtest")),
                allOf(containsString("obevo-internal-test-client-02"), containsString("vfs/classpathtest"))
        ));


        // verify that we can find the same files if they exist under different classpaths
        assertThat(FileRetrievalMode.CLASSPATH.resolveFileObjects("vfs/classpathtest/dir1/file2-multiinstance.txt").collect(getFilePath), containsInAnyOrder(
                allOf(containsString("obevo-internal-test-client-01"), containsString("vfs/classpathtest/dir1/file2-multiinstance.txt")),
                allOf(containsString("obevo-internal-test-client-02"), containsString("vfs/classpathtest/dir1/file2-multiinstance.txt"))
        ));


        // now verify that traversal from a folder to its descendants can still emit a full classpath
        MutableList<String> resolvedFiles = Lists.mutable.empty();
        for (FileObject sourceDir : sourceDirs) {
            FileObject[] files = sourceDir.findFiles(new BasicFileSelector(vcsAware(), true));
            for (FileObject file : files) {
                // only check for files, not folders
                if (!file.getType().hasChildren()) {
                    resolvedFiles.add(getFilePath.valueOf(file));
                }
            }
        }
        assertThat(resolvedFiles, containsInAnyOrder(
                endsWith("obevo-core/target/test-classes/vfs/classpathtest/dir1/file0.txt"),
                allOf(containsString("obevo-internal-test-client-01"), containsString("vfs/classpathtest/dir1/file1.txt")),
                allOf(containsString("obevo-internal-test-client-01"), containsString("vfs/classpathtest/dir1/file2-multiinstance.txt")),
                allOf(containsString("obevo-internal-test-client-02"), containsString("vfs/classpathtest/dir1/file2-multiinstance.txt")),
                allOf(containsString("obevo-internal-test-client-02"), containsString("vfs/classpathtest/dir1/file3.txt")),
                allOf(containsString("obevo-internal-test-client-02"), containsString("vfs/classpathtest/dir2/file4.txt"))
//                endsWith("obevo-internal-test-client-01/target/classes/vfs/classpathtest/dir1/file1.txt"),
//                endsWith("obevo-internal-test-client-01/target/classes/vfs/classpathtest/dir1/file2-multiinstance.txt"),
//                endsWith("obevo-internal-test-client-02/target/classes/vfs/classpathtest/dir1/file2-multiinstance.txt"),
//                endsWith("obevo-internal-test-client-02/target/classes/vfs/classpathtest/dir1/file3.txt"),
//                endsWith("obevo-internal-test-client-02/target/classes/vfs/classpathtest/dir2/file4.txt")
        ));
    }


    @Test
    public void testRegularFile() {
        FileObject file;
        file = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/vfs/regularFile.txt");
        assertThat(file.getStringContent(), containsString("regularFileContent ${myFileContent}"));

        file = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/regularFile.txt");
        assertThat(file.getStringContent(), containsString("regularFileContent ${myFileContent}"));
    }

    @Test
    public void testFileWithSharp() {
        FileObject file = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/vfs/#urltestfile.txt");
        assertThat(file.getStringContent(), containsString("poundUrlTestFile ${myFileContent}"));

        file = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/#urltestfile.txt");
        assertThat(file.getStringContent(), containsString("poundUrlTestFile ${myFileContent}"));
    }

    @Test
    public void testFileWithDollarBrace() {
        FileObject file = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/vfs/token${file}.txt");
        assertThat(file.getStringContent(), containsString("dollar brace"));

        file = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/token${file}.txt");
        assertThat(file.getStringContent(), containsString("dollar brace"));

        file = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/jarLookupTest${token}.txt");
        assertThat(file.getStringContent(), containsString("dollar brace jar lookup"));
    }

    @Test
    public void testReadFromThirdPartyJar() {
        FileObject file = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("org/apache/commons/vfs2/impl/providers.xml");
        assertThat(file.getStringContent(), containsString("org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider"));
    }

    @Test
    public void testFileWithSpaces() {
        FileObject file = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/vfs/space in name.txt");
        assertThat(file.getStringContent(), containsString("space in name content"));

        file = FileRetrievalMode.CLASSPATH.resolveSingleFileObject("vfs/space in name.txt");
        assertThat(file.getStringContent(), containsString("space in name content"));
    }

    @Test
    public void testFileCreate() {
        FileObject file = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./target").resolveFile("test123");
        file.delete();
        assertFalse(file.exists());

        file = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./target").resolveFile("test123");
        file.createFolder();
        assertTrue(file.exists());
    }
}
