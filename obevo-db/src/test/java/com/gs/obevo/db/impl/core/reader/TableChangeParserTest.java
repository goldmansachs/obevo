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

import com.gs.obevo.api.appdata.ArtifactEnvironmentRestrictions;
import com.gs.obevo.api.appdata.ArtifactPlatformRestrictions;
import com.gs.obevo.api.appdata.ArtifactRestrictions;
import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.impl.graph.SortableDependency;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.vfs2.FileName;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
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

    private final ChangeType viewChangeType = mock(ChangeType.class);
    private final ChangeType spChangeType = mock(ChangeType.class);
    private final ChangeType fkChangeType = mock(ChangeType.class);
    private final ChangeType tableChangeType = mock(ChangeType.class);
    private final ChangeType staticDataChangeType = mock(ChangeType.class);
    private final ChangeType triggerChangeType = mock(ChangeType.class);

    private static class EmptyContentHashStrategy implements DbChangeHashStrategy {
        @Override
        public String hashContent(String content) {
            return content.length() > 6 ? content.substring(0, 6).toLowerCase() : content.toLowerCase();
        }
    }

    @Test
    public void testTableChangeParse() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
        String fileContent = "\n" +
                "\n" +
                "\n" +
                "//// METADATA includeEnvs=q1 includePlatforms=DB2,SYBASE_ASE,HSQL\n" +
                "\n\n\n" +  // empty space is allowed
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// CHANGE FK name=chng2\n" +
                "ADD FK1\n" +
                "  only pick up this part if at start of line // // CHANGE whatever (disabling this check)\n" +
                "\n" +
                "//// CHANGE FK name=chng3\r\n" +
                "\r\n" +
                "\r\n" +
                "ADD FK2\n" +
                "//// CHANGE name=blank1NoLine\n" +
                "//// CHANGE name=\"blank2WithLine\"\r\n" +
                "\r\n" +
                "\r\n" +
                "//// CHANGE TRIGGER name=trigger1\n" +
                "CREATE TRIGGER ABC123\n" +
                "//// CHANGE name=chng4\n" +
                "  ALTER TABLE position ADD quantity DOUBLE\n" +
                "  \n" +
                "\n" +
                "//// CHANGE name=chng5Rollback includeEnvs=abc* excludePlatforms=HSQL\n" +
                "mychange\n" +
                "\n" +
                "// ROLLBACK-IF-ALREADY-DEPLOYED\n" +
                "myrollbackcommand\n" +
                "\n" +
                "//// CHANGE name=chng5Rollback excludeEnvs=abc*\n" +
                "mychange\n" +
                "\n" +
                "// ROLLBACK-IF-ALREADY-DEPLOYED\n" +
                "myrollbackcommand\n" +
                "\n" +
                "//// CHANGE name=chng6Inactive INACTIVE\n" +
                " myinactive change\n" +
                "\n" +
                "\n" +
                "//// CHANGE name=chng7InactiveWithRollback INACTIVE\n" +
                "inroll change\n" +
                "\n" +
                "// ROLLBACK-IF-ALREADY-DEPLOYED\n" +
                "myotherrollbackcommand\n" +
                "\n";

        ImmutableList<Change> changes = parser.value(
                tableChangeType, null, fileContent, objectName, "schema", null);

        ImmutableList<ChangeIncremental> expected = Lists.immutable.
                with(
                        new ChangeIncremental(tableChangeType, "schema", "MyObj", "chng1", 0, "create",
                                "\nCREATE TABLE;")
                        ,
                        new ChangeIncremental(fkChangeType, "schema", "MyObj", "chng2", 1, "add fk",
                                "ADD FK1\r\n  only pick up this part if at start of line // // CHANGE whatever (disabling this check)")
                        , new ChangeIncremental(fkChangeType, "schema", "MyObj", "chng3", 2,
                                "\r\n\r\nad", "\r\n\r\nADD FK2")
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj", "blank1NoLine", 3, "", "")
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj", "blank2WithLine", 4,
                                "\r\n", "\r\n")
                        , new ChangeIncremental(triggerChangeType, "schema", "MyObj", "trigger1", 5, "create",
                                "CREATE TRIGGER ABC123")
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj", "chng4", 6, "  alte",
                                "  ALTER TABLE position ADD quantity DOUBLE")
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj", "chng5Rollback", 7,
                                "mychan", "mychange", "myrollbackcommand", true)
                                .withRestrictions(
                                        Lists.immutable.of(
                                                new ArtifactEnvironmentRestrictions(UnifiedSet.newSetWith("abc*"), UnifiedSet.<String>newSet()),
                                                new ArtifactPlatformRestrictions(UnifiedSet.<String>newSet(), UnifiedSet.newSetWith("HSQL"))
                                        )
                                )
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj", "chng5Rollback", 8,
                                "mychan", "mychange", "myrollbackcommand", true)
                                .withRestrictions(
                                        Lists.immutable.of(
                                                new ArtifactEnvironmentRestrictions(UnifiedSet.<String>newSet(), UnifiedSet.newSetWith("abc*")),
                                                new ArtifactPlatformRestrictions(UnifiedSet.newSetWith("DB2", "SYBASE_ASE", "HSQL"), UnifiedSet.<String>newSet())
                                        )
                                )
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj", "chng6Inactive", 9,
                                " myina", "myinactive change", null, false)
                        , new ChangeIncremental(tableChangeType, "schema", "MyObj",
                                "chng7InactiveWithRollback", 10, "inroll", "inroll change", "myotherrollbackcommand",
                                false)
                );

        assertEquals(expected.size(), changes.size());
        for (int i = 0; i < expected.size(); i++) {
            assertTrue("Mismatch on row " + i + "; expected [" + expected.get(i) + "] but was [" + changes.get(i)
                    + "]", expected.get(i).equalsOnContent(changes.get(i)));
            assertEquals(expected.get(i).getChangeName(), changes.get(i).getChangeName());
            ImmutableList<ArtifactRestrictions> restrictions = expected.get(i).getRestrictions() == null ?
                    Lists.immutable.of(
                            new ArtifactEnvironmentRestrictions(UnifiedSet.newSetWith("q1"), UnifiedSet.<String>newSet()),
                            new ArtifactPlatformRestrictions(UnifiedSet.newSetWith("DB2", "SYBASE_ASE", "HSQL"), UnifiedSet.<String>newSet())
                    ) :
                    expected.get(i).getRestrictions();

            assertEquals(2, changes.get(i).getRestrictions().size());
            assertRestrictions(ArtifactEnvironmentRestrictions.class, restrictions, changes.get(i));
            assertRestrictions(ArtifactPlatformRestrictions.class, restrictions, changes.get(i));
        }
    }

    @Test
    public void testTemplate() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
        String fileContent = "contentNotAllowedHere\n" +
                "CREATE TABLE;\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine1() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("Instead, found this section in between")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
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
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType, true, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false));
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
        String fileContent = "\n" +
                "//// CHANGE name=chng1\n" +
                "CREATE TABLE;\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noMetadataContentAllowedAfterFirstLine2_FineInBackwardsCompatibleMode() throws Exception {
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType, true, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false));
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
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
        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType, true, new DeployMetricsCollectorImpl(), new TextMarkupDocumentReader(false));
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

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
        String fileContent = "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }

    @Test
    public void noContentAtAll2() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expect(new ThrowableMessageMatcher<Throwable>(containsString("No //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections found; at least one is required")));

        TableChangeParser parser = new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType);
        String fileContent = "\n" +
                "//// METADATA\n" +
                "";

        parser.value(tableChangeType, null, fileContent, objectName, "schema", null);
    }


    private void assertRestrictions(Class<? extends ArtifactRestrictions> type, ImmutableList<ArtifactRestrictions> expected, Change actual) {
        assertEquals(getRestrictionsByType(type, expected).getIncludes(), getRestrictionsByType(type, actual.getRestrictions()).getIncludes());
        assertEquals(getRestrictionsByType(type, expected).getExcludes(), getRestrictionsByType(type, actual.getRestrictions()).getExcludes());
    }

    private ArtifactRestrictions getRestrictionsByType(Class<? extends ArtifactRestrictions> type, ImmutableList<ArtifactRestrictions> restrictions) {
        MutableList<ArtifactRestrictions> found = Lists.mutable.ofAll(restrictions).select(Predicates.instanceOf(type));
        assertEquals(String.format("Expecting [%s] restriction of type [%s]", 1, type.getSimpleName()), 1, found.size());
        return found.getFirst();
    }

    @Test
    public void testDbChange() {
        ChangeIncremental change = (ChangeIncremental) new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType)
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
        ChangeIncremental change = (ChangeIncremental) new TableChangeParser(new EmptyContentHashStrategy(), fkChangeType, triggerChangeType)
                .value(tableChangeType,
                        null,"//// CHANGE name=chng5Rollback INACTIVE baselinedChanges=\"a,b,c\" \nmychange\n\n// ROLLBACK-IF-ALREADY-DEPLOYED\nmyrollbackcommand\n", objectName
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
