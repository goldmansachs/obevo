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
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.impl.graph.SortableDependency;
import com.gs.obevo.impl.reader.TableChangeParser.GetChangeType;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.vfs2.FileName;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableChangeParserTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final String objectName = "MyObj";

    private final ChangeType tableChangeType = mock(ChangeType.class);
    private final GetChangeType getChangeType = GetChangeType.DEFAULT_IMPL;

    private static class EmptyContentHashStrategy implements DbChangeHashStrategy {
        @Override
        public String hashContent(String content) {
            return content.length() > 6 ? content.substring(0, 6).toLowerCase() : content.toLowerCase();
        }
    }

    @Test
    public void testTemplate() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "//// METADATA templateParams=\"suffix=1;suffix=2\"\n" +
                "//// CHANGE name=chng1\ncreate1\n" +
                "//// CHANGE name=chng2\ncreate2\n" +
                "";

        ImmutableList<Change> changes = parser.value(tableChangeType, null, fileContent, "MyTemplate${suffix}", "schema", null);
        assertEquals(4, changes.size());
        assertEquals(2, changes.count(Predicates.attributeEqual(SortableDependency.TO_OBJECT_NAME, "MyTemplate1")));
        assertEquals(2, changes.count(Predicates.attributeEqual(SortableDependency.TO_OBJECT_NAME, "MyTemplate2")));
    }

    @Test
    public void invalidNoContentAllowedInMetadata() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("First content of the file must be the")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "contentNotAllowedHere\n" +
                "//// METADATA\n" +
                "invalid content\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void invalidNoContentAllowedInPrologue1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("First content of the file must be the")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "contentNotAllowedHere\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void invalidNoContentAllowedInPrologue2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("First content of the file must be the")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "contentNotAllowedHere\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void invalidNoContentAllowedInPrologue3() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("No //// CHANGE sections found")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "contentNotAllowedHere\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("Instead, found this section in between")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine1_FineInBackwardsCompatibleMode() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), true, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false), getChangeType);
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("Instead, found this section in between")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine2_FineInBackwardsCompatibleMode() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), true, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false), getChangeType);
        String fileContent = "\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine3() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("Instead, found this section in between")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine3_FineInBackwardsCompatibleMode() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), true, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false), getChangeType);
        String fileContent = "\n" +
                "//// METADATA\n" +
                "//// METADATA\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noContentAtAll1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("No //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections found; at least one is required")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noContentAtAll2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("No //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections found; at least one is required")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), getChangeType);
        String fileContent = "\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void testDbChange() {
        ChangeIncremental change = (ChangeIncremental) new TableChangeParser(new EmptyContentHashStrategy(), getChangeType)
                .value(tableChangeType,
                        null, "//// CHANGE name=chng5Rollback applyGrants=true INACTIVE baselinedChanges=\"a,b,c\" \nmychange\n\n// ROLLBACK-IF-ALREADY-DEPLOYED\nmyrollbackcommand\n", objectName
                        , "schem", null).get(0);
        assertEquals("schem", change.getSchema());
        assertEquals("chng5Rollback", change.getChangeName());
        assertEquals("mychange\n", change.getContent());
        assertEquals("mychan", change.getContentHash());
        assertEquals("myrollbackcommand", change.getRollbackIfAlreadyDeployedContent());
        assertEquals(UnifiedSet.newSetWith("a", "b", "c"), change.getBaselinedChanges().toSet());
        assertFalse(change.isActive());
        assertTrue(change.getApplyGrants());
    }

    @Test
    public void testDbChange2DiffValues() {
        ChangeIncremental change = (ChangeIncremental) new TableChangeParser(new EmptyContentHashStrategy(), getChangeType)
                .value(tableChangeType,
                        null, "//// CHANGE name=chng5Rollback INACTIVE baselinedChanges=\"a,b,c\" \nmychange\n\n// ROLLBACK-IF-ALREADY-DEPLOYED\nmyrollbackcommand\n", objectName
                        , "schem", null).get(0);
        assertEquals("schem", change.getSchema());
        assertEquals("chng5Rollback", change.getChangeName());
        assertEquals("mychange\n", change.getContent());
        assertEquals("mychan", change.getContentHash());
        assertEquals("myrollbackcommand", change.getRollbackIfAlreadyDeployedContent());
        assertEquals(UnifiedSet.newSetWith("a", "b", "c"), change.getBaselinedChanges().toSet());
        assertFalse(change.isActive());
        assertNull(change.getApplyGrants());
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
