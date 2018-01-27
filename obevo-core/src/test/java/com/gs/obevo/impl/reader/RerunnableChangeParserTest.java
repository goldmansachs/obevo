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
package com.gs.obevo.impl.reader;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.CodeDependency;
import com.gs.obevo.api.appdata.CodeDependencyType;
import com.gs.obevo.api.platform.ChangeType;
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

    private final String objectName = "MyObj";

    @Test
    public void readSimpleFile() throws Exception {
        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "mycontent" +
                "";

        ImmutableList<Change> changes = parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
        Verify.assertSize(1, changes);
        Change change = changes.get(0);

        assertEquals(objectName, change.getObjectName());
        assertEquals("\nmycontent", change.getContent());
        assertEquals(null, change.getDropContent());
    }

    @Test
    public void readFileWithMetaAndDrop() throws Exception {
        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "//// METADATA dependencies=\"abc,123\"\n" +
                "mycontent\n" +
                "line2\n" +
                "//// DROP_COMMAND\n" +
                "mydrop" +
                "";

        ImmutableList<Change> changes = parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
        Verify.assertSize(1, changes);
        Change change = changes.get(0);

        assertEquals(objectName, change.getObjectName());
        assertEquals("mycontent\nline2", change.getContent());
        assertEquals(Sets.immutable.with(new CodeDependency("abc", CodeDependencyType.EXPLICIT), new CodeDependency("123", CodeDependencyType.EXPLICIT)), change.getCodeDependencies());
        assertEquals("mydrop", change.getDropContent());
    }

    @Test
    public void readFileWithBody() throws Exception {
        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent =
                "main\n" +
                        "//// BODY\n" +
                        "body content\n" +
                        "";

        ChangeType mainChangeType = mock(ChangeType.class);
        ChangeType bodyChangeType = mock(ChangeType.class);
        when(mainChangeType.getBodyChangeType()).thenReturn(bodyChangeType);
        ImmutableList<Change> changes = parser.value(mainChangeType, null, fileContent, objectName, "schema", null);
        Verify.assertSize(2, changes);

        Change c1 = changes.get(0);
        assertEquals(objectName, c1.getObjectName());
        assertEquals("main", c1.getContent());

        Change c2 = changes.get(1);
        assertEquals(objectName, c2.getObjectName());
        assertEquals("body", c2.getChangeName());
        assertEquals("body content", c2.getContent());
    }

    @Test
    public void noContentInPrologue() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("Improper section ordering: METADATA section must come before the content section")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "prologueContentBeforeSections" +
                "//// METADATA\n" +
                "";

        parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noChangeSectionAllowed1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("found these disallowed sections")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// CHANGE name=abc";

        parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noChangeSectionAllowed2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("found these disallowed sections")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "//// CHANGE name=abc";

        parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMultipleMetadataSections1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("found these extra sections instances: [METADATA")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// DROP\n" +
                "//// METADATA\n" +
                "";

        parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMultipleMetadataSections2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("found these extra sections instances: [METADATA")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// METADATA\n" +
                "";

        parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMultipleDropSections() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("found these extra sections instances: [DROP_COMMAND")));

        RerunnableChangeParser parser = new RerunnableChangeParser();
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// DROP_COMMAND\n" +
                "//// DROP_COMMAND\n" +
                "";

        parser.value(mock(ChangeType.class), null, fileContent, objectName, "schema", null);
    }
}
