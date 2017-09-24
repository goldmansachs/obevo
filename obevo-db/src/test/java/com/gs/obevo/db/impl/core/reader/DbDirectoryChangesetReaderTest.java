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
import com.gs.obevo.api.appdata.ArtifactRestrictions;
import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.ChangeRerunnable;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSelectInfo;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbDirectoryChangesetReaderTest {
    private static final String CONTENT = "";
    
    private final ChangeType viewChangeType = mock(ChangeType.class);
    private final ChangeType spChangeType = mock(ChangeType.class);
    private final ChangeType fkChangeType = mock(ChangeType.class);
    private final ChangeType tableChangeType = mock(ChangeType.class);
    private final ChangeType staticDataChangeType = mock(ChangeType.class);

    final Change sp1Change = new ChangeRerunnable(viewChangeType, "schema1", "sp1", "hash", CONTENT);
    final Change sp2Change = new ChangeRerunnable(viewChangeType, "schema1", "sp2", "hash", CONTENT);
    final Change view1Change = new ChangeRerunnable(spChangeType, "schema1", "view1", "hash", CONTENT);
    final Change view2Change = new ChangeRerunnable(spChangeType, "schema1", "view2", "hash", CONTENT);
    final Change table1Change = new ChangeIncremental(tableChangeType, "schema1", "table", "ch1", 0, "hash",
            CONTENT);
    final Change table2Change = new ChangeIncremental(fkChangeType, "schema1", "table", "ch2", 1,
            "hash", CONTENT);
    final Change table3Change = new ChangeIncremental(tableChangeType, "schema1", "table", "ch3", 2, "hash",
            CONTENT);
    final Change deltab1Change = new ChangeIncremental(tableChangeType, "schema1", "deletedtable", "ch1", 0,
            "hash", CONTENT);
    final Change deltab2Change = new ChangeIncremental(fkChangeType, "schema1", "deletedtable",
            "ch2", 1, "hash", CONTENT);
    //    Change deltab3Change = new ChangeIncremental(ChangeType.DROP_TABLE, "schema1", "deletedtable",
//            "ch3", 2, "hash", CONTENT);
    final Change deltab3Change = new ChangeIncremental(null, "schema1", "deletedtable",
            "ch3", 2, "hash", CONTENT);
    final Change data1Change = new ChangeRerunnable(staticDataChangeType, "schema1", "table", "hash",
            CONTENT);
    final Change data2Change = new ChangeRerunnable(staticDataChangeType, "schema1", "table2", "hash",
            CONTENT);
    final Change sch2sp3Change = new ChangeRerunnable(viewChangeType, "schema2", "sp3", "hash2", CONTENT);
    final Change sch2sp2Change = new ChangeRerunnable(viewChangeType, "schema2", "sp2", "hash2", CONTENT);
    final Change sch2view1Change = new ChangeRerunnable(spChangeType, "schema2", "view1", "hash2", CONTENT);
    final Change sch2view3Change = new ChangeRerunnable(spChangeType, "schema2", "view3", "hash2", CONTENT);
    final Change sch2table1Change = new ChangeIncremental(tableChangeType, "schema2", "table", "ch1", 0,
            "hash2", CONTENT);
    final Change sch2table3Change = new ChangeIncremental(tableChangeType, "schema2", "table3", "ch1", 0,
            "hash2", CONTENT);
    final Change sch2data1Change = new ChangeRerunnable(staticDataChangeType, "schema2", "table", "hash2",
            CONTENT);
    final Change sch2data3Change = new ChangeRerunnable(staticDataChangeType, "schema2", "table3", "hash2",
            CONTENT);

    final Change sptestexclude = new ChangeRerunnable(viewChangeType, "schema1", "sptestexclude", "hash",
            CONTENT);
    final Change sptestinclude = new ChangeRerunnable(viewChangeType, "schema1", "sptestinclude", "hash",
            CONTENT);

    /**
     * Note - this test needs some work - too much mocking on changeParser
     * to show that this works - we should instead be verifying that changeParser is getting called
     * , not mocking its return values
     */
    @Test
    @Ignore("Reignoring for now")
    public void testRead() throws Exception {
        String excludeEnvName = "exclEnv";
        String includeEnvName = "inclEnv";
        this.sptestexclude.setRestrictions(
                Lists.immutable.<ArtifactRestrictions>of(
                    new ArtifactEnvironmentRestrictions(null, UnifiedSet.newSetWith(excludeEnvName))
                ));
        this.sptestinclude.setRestrictions(
                Lists.immutable.<ArtifactRestrictions>of(
                        new ArtifactEnvironmentRestrictions(UnifiedSet.newSetWith(includeEnvName), null)
                ));

        TableChangeParser changeParser = mock(TableChangeParser.class);

        FileObject sourcePath1 = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/reader/DbDirectoryChangesetReader");
        FileObject sourcePath2 = FileRetrievalMode.FILE_SYSTEM.resolveSingleFileObject("./src/test/resources/reader/DbDirectoryChangesetReader2");

        MutableSet<Change> expectedSchema1Changes = UnifiedSet.newSetWith(this.sp1Change, this.sp2Change,
                this.view1Change, this.view2Change, this.table1Change, this.table2Change, this.table3Change, this.deltab1Change, this.deltab2Change,
                this.deltab3Change, this.data1Change, this.data2Change);
        MutableSet<Change> expectedSchema2Changes = UnifiedSet.newSetWith(this.sch2sp3Change, this.sch2sp2Change,
                this.sch2view1Change, this.sch2view3Change, this.sch2table1Change, this.sch2table3Change, this.sch2data1Change, this.sch2data3Change);

        // test 1 - specify neither the include or exclude env; this will cause the sp w/ the include clause to get
        // ignored
        {
            DbEnvironment env = mock(DbEnvironment.class);
            this.setupParser1(changeParser, sourcePath1);
            this.setupParser2(changeParser, sourcePath2, env);
            when(env.getSourceDirs()).thenReturn(Lists.mutable.with(sourcePath1, sourcePath2));
            when(env.getSchemaNames()).thenReturn(Sets.immutable.with("schema1", "schema2"));
            when(env.getName()).thenReturn("abc");
            DbDirectoryChangesetReader reader = new DbDirectoryChangesetReader(Functions.getStringPassThru(), changeParser,
                    changeParser, changeParser, env);
            ImmutableList<Change> changes = reader.readChanges(false);
            assertEquals(expectedSchema1Changes.with(this.sptestexclude),
                    changes.select(Predicates.attributeEqual(Change.schema(), "schema1")));
            assertEquals(expectedSchema2Changes,
                    changes.select(Predicates.attributeEqual(Change.schema(), "schema2")));
        }

        // test 2 - specify the include env; this will allow both artifacts w/ include and exclude in as both conditions
        // pass
        {
            DbEnvironment env = mock(DbEnvironment.class);
            this.setupParser1(changeParser, sourcePath1);
            this.setupParser2(changeParser, sourcePath2, env);
            when(env.getSourceDirs()).thenReturn(Lists.mutable.with(sourcePath1, sourcePath2));
            when(env.getName()).thenReturn(includeEnvName);
            when(env.getSchemaNames()).thenReturn(Sets.immutable.with("schema1", "schema2"));
            DbDirectoryChangesetReader reader = new DbDirectoryChangesetReader(Functions.getStringPassThru(), changeParser,
                    changeParser, changeParser, env);
            ImmutableList<Change> changes = reader.readChanges(false);
            assertEquals(expectedSchema1Changes.with(this.sptestexclude).with(this.sptestinclude),
                    changes.select(Predicates.attributeEqual(Change.schema(), "schema1")));
            assertEquals(expectedSchema2Changes,
                    changes.select(Predicates.attributeEqual(Change.schema(), "schema2")));
        }

        // test 3 - specify the exclude env; neither condition passes in this case
        DbEnvironment env = mock(DbEnvironment.class);
        this.setupParser1(changeParser, sourcePath1);
        this.setupParser2(changeParser, sourcePath2, env);
        when(env.getSourceDirs()).thenReturn(Lists.mutable.with(sourcePath1, sourcePath2));
        when(env.getName()).thenReturn(includeEnvName);
        when(env.getSchemaNames()).thenReturn(Sets.immutable.with("schema1", "schema2"));
        DbDirectoryChangesetReader reader = new DbDirectoryChangesetReader(Functions.getStringPassThru(), changeParser,
                changeParser, changeParser, env);
        ImmutableList<Change> changes = reader.readChanges(false);
        assertEquals(expectedSchema1Changes,
                changes.select(Predicates.attributeEqual(Change.schema(), "schema1")));
        assertEquals(expectedSchema2Changes,
                changes.select(Predicates.attributeEqual(Change.schema(), "schema2")));

        // assertThat(changes.get("schema1"), hasItems(expectedSchema1Changes));
        // assertThat(changes.get("schema2"), hasItems(expectedSchema2Changes));
    }

    private void setupParser1(TableChangeParser changeParser, FileObject sourcePath) {
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/sp/sp1.spc"), "schema1",
                Lists.immutable.with(this.sp1Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/sp/sp2.ddl"), "schema1",
                Lists.immutable.with(this.sp2Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/sp/sptestexclude.spc"), "schema1",
                Lists.immutable.with(this.sptestexclude));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/sp/sptestinclude.spc"), "schema1",
                Lists.immutable.with(this.sptestinclude));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/view/view1.ddl"), "schema1",
                Lists.immutable.with(this.view1Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/view/view2.sql"), "schema1",
                Lists.immutable.with(this.view2Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/sp/sp3.spc"), "schema2",
                Lists.immutable.with(this.sch2sp3Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/sp/sp2.ddl"), "schema2",
                Lists.immutable.with(this.sch2sp2Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/view/view1.ddl"), "schema2",
                Lists.immutable.with(this.sch2view1Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/view/view3.sql"), "schema2",
                Lists.immutable.with(this.sch2view3Change));
    }

    private void setupParser2(TableChangeParser changeParser, FileObject sourcePath, DbEnvironment env) {
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/table/table.ddl"), "schema1",
                Lists.immutable.with(this.table1Change, this.table2Change, this.table3Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/table/deletedtable.ddl"), "schema1",
                Lists.immutable.with(this.deltab1Change, this.deltab2Change, this.deltab3Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/data/table.txt"), "schema1",
                Lists.immutable.with(this.data1Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema1/data/table2.csv"), "schema1",
                Lists.immutable.with(this.data2Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/table/table.ddl"), "schema2",
                Lists.immutable.with(this.sch2table1Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/table/table3.ddl"), "schema2",
                Lists.immutable.with(this.sch2table3Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/data/table.txt"), "schema2",
                Lists.immutable.with(this.sch2data1Change));
        addFileToChangeParserMock(changeParser, sourcePath.resolveFile("schema2/data/table3.csv"), "schema2",
                Lists.immutable.with(this.sch2data3Change));
    }

    private void addFileToChangeParserMock(TableChangeParser changeParser, FileObject fileObject, String schema, ImmutableList<Change> changes) {
        when(changeParser.value(tableChangeType, fileObject, fileObject.getStringContent(), fileObject.getName().getBaseName(), schema, null)).thenReturn(changes);
    }

    @Test
    public void testWildcardOperator() {
        assertFalse(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("myfile")));
        assertFalse(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("changes")));
        assertTrue(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("mytable.changes.txt")));
        assertTrue(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("MYTABLE.cHAngES.txt")));
        assertTrue(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("abc.mytable.changes.txt")));
        assertTrue(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("mytable.changes.txt.txt")));
        assertFalse(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("changes.txt")));
        assertFalse(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("mytable.changes")));
        assertFalse(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("my_changes.txt")));
        assertFalse(DbDirectoryChangesetReader.CHANGES_WILDCARD_FILTER.accept(getFileSelectInfo("mytable.changes_my")));

    }

    private FileSelectInfo getFileSelectInfo(String fileBaseName) {
        FileName fileNameObject = mock(FileName.class);
        when(fileNameObject.getBaseName()).thenReturn(fileBaseName);

        org.apache.commons.vfs2.FileObject file = mock(org.apache.commons.vfs2.FileObject.class);
        when(file.getName()).thenReturn(fileNameObject);

        FileSelectInfo info = mock(FileSelectInfo.class);
        when(info.getFile()).thenReturn(file);

        return info;
    }
}
