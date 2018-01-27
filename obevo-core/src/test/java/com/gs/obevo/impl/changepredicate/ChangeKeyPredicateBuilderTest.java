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
package com.gs.obevo.impl.changepredicate;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangeKeyPredicateBuilderTest {
    private final MutableList<Change> allChanges = Lists.mutable.empty();
    private final Change tableSch1ObjAChng1 = change("sch1", "table", "objA", "chng1");
    private final Change tableSch1ObjAChng2 = change("sch1", "table", "objA", "chng2");
    private final Change tableSch1ObjAChng3 = change("sch1", "table", "objA", "chng3");
    private final Change tableSch1ObjBChng1 = change("sch1", "table", "objB", "CCCChange1");
    private final Change tableSch1ObjCChng1 = change("sch1", "table", "mytableC", "chng1");
    private final Change viewSch1ObjD = change("sch1", "view", "objD", "");
    private final Change viewSch1ObjE = change("sch1", "view", "viewE", "");
    private final Change tableSch2ObjAChng1 = change("sch2", "table", "objA", "CCCChange1");
    private final Change viewSch2ObjF = change("sch2", "view", "objF", "");

    @Test
    public void testFullWildcard() {
        Predicate<? super Change> allPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%");

        assertEquals(allChanges.toSet(),
                allChanges.select(allPredicate).toSet());
    }

    @Test
    public void testSchema() {
        Predicate<? super Change> allSchemaPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("sch%");
        assertEquals(allChanges.toSet(),
                allChanges.select(allSchemaPredicate).toSet());

        Predicate<? super Change> schema1Predicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("sch1%~%~%~%");

        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjBChng1, tableSch1ObjCChng1, viewSch1ObjD, viewSch1ObjE),
                allChanges.select(schema1Predicate).toSet());

        Predicate<? super Change> schema2Predicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%2~%~%~%");

        assertEquals(Sets.mutable.with(tableSch2ObjAChng1, viewSch2ObjF),
                allChanges.select(schema2Predicate).toSet());
    }

    @Test
    public void testObjectType() {
        Predicate<? super Change> tablePredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~table");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjBChng1, tableSch1ObjCChng1, tableSch2ObjAChng1),
                allChanges.select(tablePredicate).toSet());

        Predicate<? super Change> viewPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~view~%~%");
        assertEquals(Sets.mutable.with(viewSch1ObjD, viewSch1ObjE, viewSch2ObjF),
                allChanges.select(viewPredicate).toSet());
    }

    @Test
    public void testObjName() {
        Predicate<? super Change> allObjPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~%~obj%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjBChng1, viewSch1ObjD, tableSch2ObjAChng1, viewSch2ObjF),
                allChanges.select(allObjPredicate).toSet());

        Predicate<? super Change> objAviewEPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~%~objA,viewE~%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, viewSch1ObjE, tableSch2ObjAChng1),
                allChanges.select(objAviewEPredicate).toSet());
    }

    @Test
    public void testChangeName() {
        Predicate<? super Change> changeNamePredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~%~%~CCCC%");
        assertEquals(Sets.mutable.with(tableSch1ObjBChng1, tableSch2ObjAChng1),
                allChanges.select(changeNamePredicate).toSet());
    }

    @Test
    public void testComboInSinglePredicate() {
        Predicate<? super Change> comboPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("sc%1~%~objA,mytableC~%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjCChng1),
                allChanges.select(comboPredicate).toSet());
    }

    @Test
    public void testComboInFullPredicate() {
        Predicate<? super Change> singleComboPredicate1 =
                ChangeKeyPredicateBuilder.parseFullPredicate("sc%1~%~objA,mytableC~%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjCChng1),
                allChanges.select(singleComboPredicate1).toSet());

        Predicate<? super Change> pred2 =
                ChangeKeyPredicateBuilder.parseFullPredicate("%~%~%~CCCC%");
        assertEquals(Sets.mutable.with(tableSch1ObjBChng1, tableSch2ObjAChng1),
                allChanges.select(pred2).toSet());

        Predicate<? super Change> fullComboPredicate =
                ChangeKeyPredicateBuilder.parseFullPredicate("sc%1~%~objA,mytableC~%;%~%~%~CCCC%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjCChng1, tableSch1ObjBChng1, tableSch2ObjAChng1),
                allChanges.select(fullComboPredicate).toSet());
    }

    private Change change(String schema, String changeTypeName, String objectName, String changeName) {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn(changeTypeName);

        Change change = mock(Change.class);
        when(change.getSchema()).thenReturn(schema);
        when(change.getChangeType()).thenReturn(changeType);
        when(change.getObjectName()).thenReturn(objectName);
        when(change.getChangeName()).thenReturn(changeName);

        allChanges.add(change);
        return change;
    }
}