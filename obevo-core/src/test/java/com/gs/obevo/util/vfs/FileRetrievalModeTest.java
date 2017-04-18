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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSetIterable;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Sets;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.gs.obevo.util.vfs.FileFilterUtils.vcsAware;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileRetrievalModeTest {

    @Test
    public void testShouldRetrieveAllFoldersAvailableInClassPath() {
        ListIterable<FileObject> sourceDirs = FileRetrievalMode.CLASSPATH.resolveFileObjects("database/");
        assertEquals(3, sourceDirs.size());

        MutableSetIterable<String> resolved = Sets.mutable.empty();
        for (FileObject sourceDir : sourceDirs) {
            FileObject[] files = sourceDir.findFiles(new BasicFileSelector(vcsAware(), true));
            for (FileObject file : files) {
                resolved.add(file.getURL().getPath());
            }
        }
        assertTrue(resolved.anySatisfy(StringPredicates.endsWith("TEST_TABLE.sql")));
        assertTrue(resolved.anySatisfy(StringPredicates.endsWith("TEST_TABLE_1.sql")));
        assertTrue(resolved.anySatisfy(StringPredicates.endsWith("TEST_TABLE_2.sql")));
        assertTrue(resolved.anySatisfy(StringPredicates.endsWith("TEST_TABLE_3.sql")));
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
    public void testReadFromJar() {
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
