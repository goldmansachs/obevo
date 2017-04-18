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
package com.gs.obevo.impl.graph;

import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphEnricherImplTest {
    protected GraphEnricher enricher = new GraphEnricherImpl(Functions.getStringPassThru());
    ;

    private final String schema1 = "schema1";
    private final String schema2 = "schema2";
    private final String type1 = "type1";
    private final String type2 = "type2";

    @Test
    public void testSchemaObjectDependenciesCaseSensitive() {
        testSchemaObjectDependencies(false);
    }

    @Test
    public void testSchemaObjectDependenciesCaseInsensitive() {
        testSchemaObjectDependencies(true);
    }

    /**
     * The test data in this class is all written w/ case-sensitivy as the default.
     * If we pass caseInsensitive == true, then we enable that mode in the graph enricher and tweak the object names
     * a bit so that we can verify that the resolution works either way.
     */
    public void testSchemaObjectDependencies(boolean caseInsensitive) {
        this.enricher = new GraphEnricherImpl(caseInsensitive ? StringFunctions.toUpperCase() : Functions.getStringPassThru());

        SortableDependencyGroup sch1Obj1 = newChange(schema1, type1, "obj1", Sets.immutable.with("obj3", schema2 + ".obj2"));
        SortableDependencyGroup sch1Obj2 = newChange(schema1, type2, "obj2", Sets.immutable.<String>with());
        // change the case of the object name to ensure others can still point to it
        SortableDependencyGroup sch1Obj3 = newChange(schema1, type1, caseInsensitive ? "obj3".toUpperCase() : "obj3", Sets.immutable.with("obj2"));
        // change the case of the dependency name to ensure that it can point to others
        SortableDependencyGroup sch2Obj1 = newChange(schema2, type1, "obj1", Sets.immutable.with(caseInsensitive ? "obj2".toUpperCase() : "obj2"));
        SortableDependencyGroup sch2Obj2 = newChange(schema2, type2, "obj2", Sets.immutable.with(schema1 + ".obj3"));

        DirectedGraph<SortableDependencyGroup, DefaultEdge> sortGraph = enricher.createDependencyGraph(Lists.mutable.with(
                sch1Obj1, sch1Obj2, sch1Obj3, sch2Obj1, sch2Obj2), false);

        validateChange(sortGraph, sch1Obj1, Sets.immutable.with(sch1Obj3, sch2Obj2), Sets.immutable.<SortableDependencyGroup>with());
        validateChange(sortGraph, sch1Obj2, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj3));
        validateChange(sortGraph, sch1Obj3, Sets.immutable.with(sch1Obj2), Sets.immutable.with(sch1Obj1, sch2Obj2));
        validateChange(sortGraph, sch2Obj1, Sets.immutable.with(sch2Obj2), Sets.immutable.<SortableDependencyGroup>with());
        validateChange(sortGraph, sch2Obj2, Sets.immutable.with(sch1Obj3), Sets.immutable.with(sch1Obj1, sch2Obj1));
    }


    @Test
    public void testScheamObjectChangeDependenciesCaseSensitiveAndRegular() {
        testSchemaObjectChangeDependencies(false, false);
    }

    @Test
    public void testScheamObjectChangeDependenciesCaseInsensitiveAndRegular() {
        testSchemaObjectChangeDependencies(true, false);
    }

    @Test
    public void testScheamObjectChangeDependenciesCaseSensitiveAndRollback() {
        testSchemaObjectChangeDependencies(false, true);
    }

    @Test
    public void testScheamObjectChangeDependenciesCaseInsensitiveAndRollback() {
        testSchemaObjectChangeDependencies(true, true);
    }

    private void testSchemaObjectChangeDependencies(boolean caseInsensitive, boolean rollback) {
        this.enricher = new GraphEnricherImpl(caseInsensitive ? StringFunctions.toUpperCase() : Functions.getStringPassThru());

        SortableDependencyGroup sch1Obj1Chng0 = newChange(schema1, type1, "obj1", "c0", 0, Sets.immutable.<String>with());
        SortableDependencyGroup sch1Obj1Chng1 = newChange(schema1, type1, "obj1", "c1", 1, Sets.immutable.<String>with());
        // A) Check that an incremental change can depend on a rerunnable object creation
        SortableDependencyGroup sch1Obj1Chng2 = newChange(schema1, type1, "obj1", "c2", 2, Sets.immutable.with("singleObj5"));
        SortableDependencyGroup sch1Obj2Chng0 = newChange(schema1, type2, "obj2", "c0", 0, Sets.immutable.<String>with());
        // B) Check that we can depend on another incremental object (and that the object can later depend on this via a later change)
        SortableDependencyGroup sch1Obj2Chng1 = newChange(schema1, type2, "obj2", "c1", 1, Sets.immutable.<String>with(schema2 + "." + "obj1.c1"));
        SortableDependencyGroup sch1Obj2Chng2 = newChange(schema1, type2, "obj2", caseInsensitive ? "c2".toUpperCase() : "c2", 2, Sets.immutable.<String>with());
        SortableDependencyGroup sch1Obj3Chng0 = newChange(schema1, type2, "obj3", "c0", 0, Sets.immutable.<String>with());
        SortableDependencyGroup sch1Obj3Chng1 = newChange(schema1, type2, "obj3", "c1", 1, Sets.immutable.<String>with());
        SortableDependencyGroup sch2Obj1Chng0 = newChange(schema2, type1, "obj1", "c0", 0, Sets.immutable.<String>with());
        SortableDependencyGroup sch2Obj1Chng1 = newChange(schema2, type1, "obj1", "c1", 1, Sets.immutable.<String>with());
        // C) Check that we can depend on another object (relating to test case B above)
        SortableDependencyGroup sch2Obj1Chng2 = newChange(schema2, type1, "obj1", "c2", 2, Sets.immutable.<String>with(schema1 + "." + "obj2.c2"));

        SortableDependencyGroup sch1SingleObj5 = newChange(schema1, type1, "singleObj5", Sets.immutable.<String>with());
        // D) Check that a rerunnable object can depend on an incremental object; this would put this dependency at the end
        SortableDependencyGroup sch1SingleObj6 = newChange(schema1, type1, "singleObj6", Sets.immutable.with("obj2"));

        DirectedGraph<SortableDependencyGroup, DefaultEdge> sortGraph = enricher.createDependencyGraph(Lists.mutable.with(
                sch1Obj1Chng0, sch1Obj1Chng1, sch1Obj1Chng2, sch1Obj2Chng0, sch1Obj2Chng1, sch1Obj2Chng2, sch1Obj3Chng0, sch1Obj3Chng1, sch2Obj1Chng0, sch2Obj1Chng1, sch2Obj1Chng2, sch1SingleObj5, sch1SingleObj6), rollback);

        if (!rollback) {
            // For non-rollback, the incremental dependencies would be in regular order (i.e. change0 then change1 then change2)
            validateChange(sortGraph, sch1Obj1Chng0, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj1Chng1));
            validateChange(sortGraph, sch1Obj1Chng1, Sets.immutable.with(sch1Obj1Chng0), Sets.immutable.with(sch1Obj1Chng2));
            validateChange(sortGraph, sch1Obj1Chng2, Sets.immutable.with(sch1Obj1Chng1, sch1SingleObj5), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch1Obj2Chng0, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj2Chng1));
            validateChange(sortGraph, sch1Obj2Chng1, Sets.immutable.with(sch1Obj2Chng0, sch2Obj1Chng1), Sets.immutable.with(sch1Obj2Chng2));
            validateChange(sortGraph, sch1Obj2Chng2, Sets.immutable.with(sch1Obj2Chng1), Sets.immutable.with(sch2Obj1Chng2, sch1SingleObj6));
            validateChange(sortGraph, sch1Obj3Chng0, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj3Chng1));
            validateChange(sortGraph, sch1Obj3Chng1, Sets.immutable.with(sch1Obj3Chng0), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch2Obj1Chng0, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch2Obj1Chng1));
            validateChange(sortGraph, sch2Obj1Chng1, Sets.immutable.with(sch2Obj1Chng0), Sets.immutable.with(sch2Obj1Chng2, sch1Obj2Chng1));
            validateChange(sortGraph, sch2Obj1Chng2, Sets.immutable.with(sch2Obj1Chng1, sch1Obj2Chng2), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch1SingleObj5, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj1Chng2));
            validateChange(sortGraph, sch1SingleObj6, Sets.immutable.with(sch1Obj2Chng2), Sets.immutable.<SortableDependencyGroup>with());
        } else {
            // For rollback, the incremental dependencies would be inverted (i.e. change2 then change1 then change0)
            validateChange(sortGraph, sch1Obj1Chng0, Sets.immutable.with(sch1Obj1Chng1), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch1Obj1Chng1, Sets.immutable.with(sch1Obj1Chng2), Sets.immutable.with(sch1Obj1Chng0));
            validateChange(sortGraph, sch1Obj1Chng2, Sets.immutable.with(sch1SingleObj5), Sets.immutable.with(sch1Obj1Chng1));
            validateChange(sortGraph, sch1Obj2Chng0, Sets.immutable.with(sch1Obj2Chng1), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch1Obj2Chng1, Sets.immutable.with(sch1Obj2Chng2, sch2Obj1Chng1), Sets.immutable.with(sch1Obj2Chng0));
            validateChange(sortGraph, sch1Obj2Chng2, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj2Chng1, sch2Obj1Chng2, sch1SingleObj6));
            validateChange(sortGraph, sch1Obj3Chng0, Sets.immutable.with(sch1Obj3Chng1), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch1Obj3Chng1, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj3Chng0));
            validateChange(sortGraph, sch2Obj1Chng0, Sets.immutable.with(sch2Obj1Chng1), Sets.immutable.<SortableDependencyGroup>with());
            validateChange(sortGraph, sch2Obj1Chng1, Sets.immutable.with(sch2Obj1Chng2), Sets.immutable.with(sch2Obj1Chng0, sch1Obj2Chng1));
            validateChange(sortGraph, sch2Obj1Chng2, Sets.immutable.with(sch1Obj2Chng2), Sets.immutable.with(sch2Obj1Chng1));
            validateChange(sortGraph, sch1SingleObj5, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sch1Obj1Chng2));
            validateChange(sortGraph, sch1SingleObj6, Sets.immutable.with(sch1Obj2Chng2), Sets.immutable.<SortableDependencyGroup>with());
        }
    }


    @Test
    public void testGetChangesCaseInsensitive() {
        this.enricher = new GraphEnricherImpl(StringFunctions.toUpperCase());
        String schema1 = "schema1";
        String schema2 = "schema2";
        String type1 = "type1";

        SortableDependencyGroup sp1 = newChange(schema1, type1, "SP1", "n/a", 0, Sets.immutable.with("sp2"));
        SortableDependencyGroup sp2 = newChange(schema1, type1, "SP2", Sets.immutable.<String>with());
        SortableDependencyGroup sp3 = newChange(schema1, type1, "SP3", Sets.immutable.with("sp1", "sp2"));
        SortableDependencyGroup spA = newChange(schema1, type1, "SPA", Sets.immutable.with("sp3"));
        SortableDependencyGroup sp1Schema2 = newChange(schema2, type1, "sp1", "n/a", 0, Sets.immutable.with("sp2", schema1 + ".sp3"));
        SortableDependencyGroup sp2Schema2 = newChange(schema2, type1, "sP2", "n/a", 0, Sets.immutable.<String>with());

        DirectedGraph<SortableDependencyGroup, DefaultEdge> sortGraph = enricher.createDependencyGraph(Lists.mutable.with(
                sp1, sp2, sp3, spA, sp1Schema2, sp2Schema2), false);

        validateChange(sortGraph, sp1, Sets.immutable.with(sp2), Sets.immutable.with(sp3));
        validateChange(sortGraph, sp2, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sp1, sp3));
        validateChange(sortGraph, sp3, Sets.immutable.with(sp1, sp2), Sets.immutable.with(spA, sp1Schema2));
        validateChange(sortGraph, spA, Sets.immutable.with(sp3), Sets.immutable.<SortableDependencyGroup>with());
        validateChange(sortGraph, sp1Schema2, Sets.immutable.with(sp2Schema2, sp3), Sets.immutable.<SortableDependencyGroup>with());
        validateChange(sortGraph, sp2Schema2, Sets.immutable.<SortableDependencyGroup>with(), Sets.immutable.with(sp1Schema2));
    }



    @Test
    public void testCycleValidation() {
        this.enricher = new GraphEnricherImpl(Functions.getStringPassThru());

        SortableDependencyGroup sch1Obj1 = newChange(schema1, type1, "obj1", Sets.immutable.with(schema2 + ".obj3"));
        SortableDependencyGroup sch1Obj2 = newChange(schema1, type2, "obj2", Sets.immutable.with("obj1"));
        SortableDependencyGroup sch2Obj3 = newChange(schema2, type1, "obj3", Sets.immutable.with(schema1 + ".obj2"));
        SortableDependencyGroup sch2Obj4 = newChange(schema2, type1, "obj4", Sets.immutable.with("obj3"));

        try {
            enricher.createDependencyGraph(Lists.mutable.with(
                    sch1Obj1, sch1Obj2, sch2Obj3, sch2Obj4), false);
            fail("Expecting an exception here due to a cycle exception, but a cycle exception was not found");
        } catch (IllegalArgumentException exc) {
            assertThat(exc.getMessage(), containsString("Found cycles"));
        }
    }

    private void validateChange(DirectedGraph<SortableDependencyGroup, DefaultEdge> changes, SortableDependencyGroup change, RichIterable<SortableDependencyGroup> precedingChanges, RichIterable<SortableDependencyGroup> followingChanges) {
        assertThat("testing preceding changes on " + change, GraphUtil.getDependencyNodes(changes, change), equalTo(precedingChanges));
        assertThat("testing following changes on " + change, GraphUtil.getDependentNodes(changes, change), equalTo(followingChanges));
    }

    protected SortableDependencyGroup newChange(String objectName, ImmutableSet<String> dependencies) {
        return newChange("schema1", "SP", objectName, "n/a", 0, dependencies);
    }

    protected SortableDependencyGroup newChange(String schema, String changeType, String objectName, ImmutableSet<String> dependencies) {
        return newChange(schema, changeType, objectName, "n/a", 0, dependencies);
    }

    protected SortableDependencyGroup newChange(String schema, String changeTypeName, String objectName, String changeName, int orderWithinObject, ImmutableSet<String> dependencies) {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn(changeTypeName);
        when(changeType.isRerunnable()).thenReturn(true);

        SortableDependency sort = mock(SortableDependency.class);
        ObjectKey key = new ObjectKey(schema, changeType, objectName);
        when(sort.getObjectKey()).thenReturn(key);
        when(sort.getDependencies()).thenReturn(dependencies);
        when(sort.getChangeName()).thenReturn(changeName);
        when(sort.getOrderWithinObject()).thenReturn(orderWithinObject);

        // to print out a nice message for the mock; we do need the string variable on a separate line
        String keyString = key.toString();
        when(sort.toString()).thenReturn(keyString);

        SortableDependencyGroup depGroup = mock(SortableDependencyGroup.class);
        when(depGroup.getComponents()).thenReturn(Sets.immutable.<SortableDependency>with(sort));
        return depGroup;
    }
}
