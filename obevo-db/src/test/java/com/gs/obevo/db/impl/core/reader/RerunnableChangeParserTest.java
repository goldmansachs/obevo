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
package com.gs.obevo.db.impl.core.reader;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.vfs2.FileName;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RerunnableChangeParserTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void readSimpleFile() throws Exception {
        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "mycontent" +
                ""
        );

        ImmutableList<Change> changes = parser.value(mock(ChangeType.class), file, "schema", null);
        Verify.assertSize(1, changes);
        Change change = changes.get(0);

        assertEquals("MyObj", change.getObjectName());
        assertEquals("\nmycontent", change.getContent());
        assertEquals(null, change.getDropContent());
    }

    @Test
    public void readFileWithMetaAndDrop() throws Exception {
        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "//// METADATA dependencies=\"abc,123\"\n" +
                "mycontent\n" +
                "line2\n" +
                "//// DROP_COMMAND\n" +
                "mydrop" +
                ""
        );

        ImmutableList<Change> changes = parser.value(mock(ChangeType.class), file, "schema", null);
        Verify.assertSize(1, changes);
        Change change = changes.get(0);

        assertEquals("MyObj", change.getObjectName());
        assertEquals("mycontent\nline2", change.getContent());
        assertEquals(Sets.immutable.with("abc", "123"), change.getDependencies());
        assertEquals("mydrop", change.getDropContent());
    }

    @Test
    public void noContentInPrologue() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectCause(new ThrowableMessageMatcher<Throwable>(containsString("Improper section ordering: METADATA section must come before the content section")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "prologueContentBeforeSections" +
                "//// METADATA\n" +
                ""
        );

        parser.value(mock(ChangeType.class), file, "schema", null);
    }

    @Test
    public void noChangeSectionAllowed1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectCause(new ThrowableMessageMatcher<Throwable>(containsString("found these disallowed sections")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "//// METADATA\n" +
                "//// CHANGE name=abc"
        );

        parser.value(mock(ChangeType.class), file, "schema", null);
    }

    @Test
    public void noChangeSectionAllowed2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectCause(new ThrowableMessageMatcher<Throwable>(containsString("found these disallowed sections")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "//// CHANGE name=abc"
        );

        parser.value(mock(ChangeType.class), file, "schema", null);
    }

    @Test
    public void noMultipleMetadataSections1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectCause(new ThrowableMessageMatcher<Throwable>(containsString("found these extra sections instances: [METADATA")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "//// METADATA\n" +
                "//// DROP\n" +
                "//// METADATA\n" +
                ""
        );

        parser.value(mock(ChangeType.class), file, "schema", null);
    }

    @Test
    public void noMultipleMetadataSections2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectCause(new ThrowableMessageMatcher<Throwable>(containsString("found these extra sections instances: [METADATA")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "//// METADATA\n" +
                "//// METADATA\n" +
                ""
        );

        parser.value(mock(ChangeType.class), file, "schema", null);
    }

    @Test
    public void noMultipleDropSections() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectCause(new ThrowableMessageMatcher<Throwable>(containsString("found these extra sections instances: [DROP_COMMAND")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        FileObject file = file("MyObj.sql", "\n" +
                "//// METADATA\n" +
                "//// DROP_COMMAND\n" +
                "//// DROP_COMMAND\n" +
                ""
        );

        parser.value(mock(ChangeType.class), file, "schema", null);
    }

    private FileObject file(String fileName, String fileContent) {
        FileName fileNameObj = mock(FileName.class);
        when(fileNameObj.getBaseName()).thenReturn(fileName);

        FileObject file = mock(FileObject.class);
        when(file.getName()).thenReturn(fileNameObj);
        when(file.getStringContent()).thenReturn(fileContent);

        return file;
    }
}
