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
package com.gs.obevo.impl.text;

import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.platform.ChangeType;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TextDependencyExtractorImplTest {
    protected static final String SCHEMA1 = "schema1";
    protected static final String SCHEMA2 = "schema2";

    private final TextDependencyExtractorImpl enricher = new TextDependencyExtractorImpl(Functions.getStringPassThru());

    @Test
    public void testCalculateDependencies() {
        TextDependencyExtractable sp1 = newObject("sp1", "sp1 sp2('a')", Sets.immutable.<String>empty(), Sets.immutable.with("manuallyAddedDependency"));
        TextDependencyExtractable sp2 = newObject("sp2", "sp2");
        TextDependencyExtractable sp3 = newObject("sp3", "sp3 sp1 ('a')sp2('a')");
        TextDependencyExtractable spA = newObject("spA", "spA sp1('a') ('a')sp2('a') sp3", Sets.immutable.with("sp1", "sp2"), Sets.immutable.<String>empty());

        enricher.calculateDependencies(Lists.mutable.with(sp1, sp2, sp3, spA));

        verifyExpectedDependencies(sp1, Sets.mutable.with("sp2", "manuallyAddedDependency"));
        verifyExpectedDependencies(sp2, Sets.mutable.<String>with());
        verifyExpectedDependencies(sp3, Sets.mutable.with("sp2", "sp1"));
        verifyExpectedDependencies(spA, Sets.mutable.with("sp3"));
    }

    @Test
    public void testCalculateDependenciesAcrossSchemas() {
        TextDependencyExtractable sp1 = newObject(SCHEMA1, "sp1", "sp1 sp2", Sets.immutable.<String>empty(), Sets.immutable.with("manuallyAddedDependency"));
        TextDependencyExtractable sp2 = newObject(SCHEMA2, "sp2", "sp2");
        TextDependencyExtractable sp3 = newObject(SCHEMA1, "sp3", "sp3 sp1 sp2");
        TextDependencyExtractable spA = newObject(SCHEMA2, "spA", "spA sp1 sp2 sp3", Sets.immutable.with("sp1", "sp2"), Sets.immutable.<String>empty());

        enricher.calculateDependencies(Lists.mutable.with(sp1, sp2, sp3, spA));

        verifyExpectedDependencies(sp1, Sets.mutable.with("sp2", "manuallyAddedDependency"));
        verifyExpectedDependencies(sp2, Sets.mutable.<String>with());
        verifyExpectedDependencies(sp3, Sets.mutable.with("sp2", "sp1"));
        verifyExpectedDependencies(spA, Sets.mutable.with("sp3"));
    }

    @Test
    public void testCalculateDependenciesForChange() {
        SetIterable<String> dependencies = enricher.calculateDependencies("test1", "create procedure sp1\n" +
                "// Comment sp2\n" +
                "-- Comment sp2\n" +
                "call sp_3(1234)  -- end of line comment sp5\n" +
                "/* Comment sp5 */\n" +
                "/* Comment\n" +
                "sp5\n" +
                "\n" +
                "sp5 */\n" +
                "call sp4(1234)\n" +
                "end\n" +
                "", Sets.mutable.with("sp1", "sp2", "sp_3", "sp4", "sp5"));
        assertEquals(Sets.mutable.with("sp1", "sp_3", "sp4"), dependencies);
    }

    protected void verifyExpectedDependencies(TextDependencyExtractable item, SetIterable<String> expectedDependencies) {
        ArgumentCaptor<ImmutableSet> setDependencies = ArgumentCaptor.forClass(ImmutableSet.class);
        verify(item).setDependencies(setDependencies.capture());

        assertEquals(expectedDependencies, setDependencies.getValue());
    }

    protected TextDependencyExtractable newObject(String objectName, String content) {
        return newObject(SCHEMA1, objectName, content);
    }

    protected TextDependencyExtractable newObject(String schema, String objectName, String content) {
        return newObject(schema, objectName, content, null, null);
    }

    protected TextDependencyExtractable newObject(String objectName, String content, ImmutableSet<String> excludeDependencies, ImmutableSet<String> includeDependencies) {
        return newObject(SCHEMA1, objectName, content, excludeDependencies, includeDependencies);
    }

    protected TextDependencyExtractable newObject(String schema, String objectName, String content, ImmutableSet<String> excludeDependencies, ImmutableSet<String> includeDependencies) {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.isEnrichableForDependenciesInText()).thenReturn(true);

        ObjectKey key = new ObjectKey(schema, changeType, objectName);

        TextDependencyExtractable item = mock(TextDependencyExtractable.class);
        when(item.getObjectKey()).thenReturn(key);
        when(item.getContentForDependencyCalculation()).thenReturn(content);
        when(item.getExcludeDependencies()).thenReturn(excludeDependencies != null ? excludeDependencies : Sets.immutable.<String>with());
        when(item.getIncludeDependencies()).thenReturn(includeDependencies != null ? includeDependencies : Sets.immutable.<String>with());

        String keyString = key.toString();
        when(item.toString()).thenReturn(keyString);

        return item;
    }
}
