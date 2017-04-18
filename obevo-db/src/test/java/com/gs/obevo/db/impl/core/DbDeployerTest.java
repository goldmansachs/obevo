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
package com.gs.obevo.db.impl.core;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaTable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.junit.Before;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DbDeployerTest {
    public static final String AUDIT_TABLE = "audit";
    private final String schema1 = "schema1";
    private final String schema2 = "schema2";
    private final ChangeType tableChangeType = mock(ChangeType.class);
    private final Platform platform = mock(Platform.class);

    @Before
    public void setup() {
        when(tableChangeType.getName()).thenReturn(ChangeType.TABLE_STR);
        when(platform.getChangeType(ChangeType.TABLE_STR)).thenReturn(tableChangeType);
        when(platform.convertDbObjectName()).thenReturn(Functions.getStringPassThru());
    }

//    @Test
//    public void testUnmanagedTableCheckNoIssues() {
//        // the tables in the DB are accounted for in the source and deployed changes, so we are good; no diffs found
//        SetIterable<ObjectKey> unmanagedTables = DbDeployer.findUnmanagedTables(platform,
//                Lists.mutable.with(
//                        table(schema1, "tab1"),
//                        table(schema1, "tab2")
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1"),
//                        change(schema1, "tab2")
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1"),
//                        change(schema1, "tab2")
//                ),
//                AUDIT_TABLE
//        );
//
//        assertEquals(Sets.immutable.empty(), unmanagedTables);
//    }
//
//    @Test
//    public void testUnmanagedTableCheckWithAuditTableNoIssues() {
//        // the tables in the DB are accounted for in the source and deployed changes and we can ignore the audit table, so we are good; no diffs found
//        SetIterable<ObjectKey> unmanagedTables = DbDeployer.findUnmanagedTables(platform,
//                Lists.mutable.with(
//                        table(schema1, "tab1"),
//                        table(schema1, "tab2"),
//                        table(schema1, AUDIT_TABLE)
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1"),
//                        change(schema1, "tab2")
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1"),
//                        change(schema1, "tab2")
//                ),
//                AUDIT_TABLE
//        );
//
//        assertEquals(Sets.immutable.empty(), unmanagedTables);
//    }
//
//    @Test
//    public void testUnmanagedTableCheckWithEmptyAuditTableNoIssues() {
//        // nothing in the actual DB, so this check is good.
//        // (Granted, if we have something in the audit table and not the DB, it is a problem on its own, but not in this case)
//        SetIterable<ObjectKey> unmanagedTables = DbDeployer.findUnmanagedTables(platform,
//                Lists.mutable.<DaTable>with(
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1"),
//                        change(schema1, "tab2")
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1"),
//                        change(schema1, "tab2")
//                ),
//                AUDIT_TABLE
//        );
//
//        assertEquals(Sets.immutable.empty(), unmanagedTables);
//    }
//
//    @Test
//    public void testUnmanagedTableCheckWithUnmanagedTables() {
//        // Here we have tables in the DB that are not accounted for in the code, so we flag this as a problem.
//        // Note the identity should go by table name AND schema (hence the checks on "tab2" below)
//        SetIterable<ObjectKey> unmanagedTables = DbDeployer.findUnmanagedTables(platform,
//                Lists.mutable.with(
//                        table(schema1, "tab1"),
//                        table(schema1, "tab2"),
//                        table(schema2, "tab2"),
//                        table(schema1, "tab3")
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab2")
//                ),
//                Lists.mutable.<Change>with(
//                        change(schema1, "tab1")
//                ),
//                AUDIT_TABLE
//        );
//
//        assertEquals(Sets.immutable.with(new ObjectKey(schema1, tableChangeType, "tab3"), new ObjectKey(schema2, tableChangeType, "tab2")),
//                unmanagedTables);
//    }

    private DaTable table(String schema, String objectName) {
        DaSchema schemaObject = mock(DaSchema.class);
        when(schemaObject.getName()).thenReturn(schema);

        DaTable table = mock(DaTable.class);
        when(table.getSchema()).thenReturn(schemaObject);
        when(table.getName()).thenReturn(objectName);
        return table;
    }

    private Change change(String schema, String objectName) {
        Change change = mock(Change.class);
        when(change.getObjectKey()).thenReturn(new ObjectKey(schema, tableChangeType, objectName));
        when(change.getChangeType()).thenReturn(tableChangeType);
        when(change.getObjectName()).thenReturn(objectName);
        when(change.getPhysicalSchema()).thenReturn(new PhysicalSchema(schema));
        return change;
    }
}