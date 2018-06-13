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

import com.gs.obevo.api.appdata.ChangeKey;
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
    private final MutableList<ChangeKey> allChanges = Lists.mutable.empty();
    private final ChangeKey tableSch1ObjAChng1 = change("sch1", "table", "objA", "chng1");
    private final ChangeKey tableSch1ObjAChng2 = change("sch1", "table", "objA", "chng2");
    private final ChangeKey tableSch1ObjAChng3 = change("sch1", "table", "objA", "chng3");
    private final ChangeKey tableSch1ObjBChng1 = change("sch1", "table", "objB", "CCCChange1");
    private final ChangeKey tableSch1ObjCChng1 = change("sch1", "table", "mytableC", "chng1");
    private final ChangeKey viewSch1ObjD = change("sch1", "view", "objD", "");
    private final ChangeKey viewSch1ObjE = change("sch1", "view", "viewE", "");
    private final ChangeKey tableSch2ObjAChng1 = change("sch2", "table", "objA", "CCCChange1");
    private final ChangeKey viewSch2ObjF = change("sch2", "view", "objF", "");

    @Test
    public void testFullWildcard() {
        Predicate<ChangeKey> allPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%");

        assertEquals(allChanges.toSet(),
                allChanges.select(allPredicate).toSet());
    }

    @Test
    public void testSchema() {
        Predicate<ChangeKey> allSchemaPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("sch%");
        assertEquals(allChanges.toSet(),
                allChanges.select(allSchemaPredicate).toSet());

        Predicate<ChangeKey> schema1Predicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("sch1%~%~%~%");

        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjBChng1, tableSch1ObjCChng1, viewSch1ObjD, viewSch1ObjE),
                allChanges.select(schema1Predicate).toSet());

        Predicate<ChangeKey> schema2Predicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%2~%~%~%");

        assertEquals(Sets.mutable.with(tableSch2ObjAChng1, viewSch2ObjF),
                allChanges.select(schema2Predicate).toSet());
    }

    @Test
    public void testObjectType() {
        Predicate<ChangeKey> tablePredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~table");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjBChng1, tableSch1ObjCChng1, tableSch2ObjAChng1),
                allChanges.select(tablePredicate).toSet());

        Predicate<ChangeKey> viewPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~view~%~%");
        assertEquals(Sets.mutable.with(viewSch1ObjD, viewSch1ObjE, viewSch2ObjF),
                allChanges.select(viewPredicate).toSet());
    }

    @Test
    public void testObjName() {
        Predicate<ChangeKey> allObjPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~%~obj%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjBChng1, viewSch1ObjD, tableSch2ObjAChng1, viewSch2ObjF),
                allChanges.select(allObjPredicate).toSet());

        Predicate<ChangeKey> objAviewEPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~%~objA,viewE~%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, viewSch1ObjE, tableSch2ObjAChng1),
                allChanges.select(objAviewEPredicate).toSet());
    }

    @Test
    public void testChangeName() {
        Predicate<ChangeKey> changeNamePredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("%~%~%~CCCC%");
        assertEquals(Sets.mutable.with(tableSch1ObjBChng1, tableSch2ObjAChng1),
                allChanges.select(changeNamePredicate).toSet());
    }

    @Test
    public void testComboInSinglePredicate() {
        Predicate<ChangeKey> comboPredicate =
                ChangeKeyPredicateBuilder.parseSinglePredicate("sc%1~%~objA,mytableC~%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjCChng1),
                allChanges.select(comboPredicate).toSet());
    }

    @Test
    public void testComboInFullPredicate() {
        Predicate<ChangeKey> singleComboPredicate1 =
                ChangeKeyPredicateBuilder.parseFullPredicate("sc%1~%~objA,mytableC~%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjCChng1),
                allChanges.select(singleComboPredicate1).toSet());

        Predicate<ChangeKey> pred2 =
                ChangeKeyPredicateBuilder.parseFullPredicate("%~%~%~CCCC%");
        assertEquals(Sets.mutable.with(tableSch1ObjBChng1, tableSch2ObjAChng1),
                allChanges.select(pred2).toSet());

        Predicate<ChangeKey> fullComboPredicate =
                ChangeKeyPredicateBuilder.parseFullPredicate("sc%1~%~objA,mytableC~%;%~%~%~CCCC%");
        assertEquals(Sets.mutable.with(tableSch1ObjAChng1, tableSch1ObjAChng2, tableSch1ObjAChng3, tableSch1ObjCChng1, tableSch1ObjBChng1, tableSch2ObjAChng1),
                allChanges.select(fullComboPredicate).toSet());
    }

    private ChangeKey change(String schema, String changeTypeName, String objectName, String changeName) {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn(changeTypeName);

        ChangeKey changeKey = new ChangeKey(schema, changeType, objectName, changeName);
        allChanges.add(changeKey);
        return changeKey;
    }
}