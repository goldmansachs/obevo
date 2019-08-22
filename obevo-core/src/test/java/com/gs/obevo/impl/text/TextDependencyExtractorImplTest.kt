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
package com.gs.obevo.impl.text

import com.gs.obevo.api.appdata.CodeDependency
import com.gs.obevo.api.appdata.CodeDependencyType
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.block.factory.Functions
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Sets
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.empty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TextDependencyExtractorImplTest {
    private val enricher = TextDependencyExtractorImpl(Functions.getStringPassThru()::valueOf)

    @Test
    fun testCalculateDependencies() {
        val sp1 = newObject("sp1", "sp1 sp2('a')", Sets.immutable.empty(), Sets.immutable.with("manuallyAddedDependency"))
        val sp2 = newObject("sp2", "sp2")
        val spChar = newObject("sp#", "sp# sp1 ('a')sp2('a')")
        val spA = newObject("spA", "spA sp1('a') ('a')sp2('a') sp#", Sets.immutable.with("sp1", "sp2"), Sets.immutable.empty())

        val dependencies = enricher.calculateDependencies(Lists.mutable.with(sp1, sp2, spChar, spA))

        assertThat(dependencies.get(sp1), containsInAnyOrder(CodeDependency("sp2", CodeDependencyType.DISCOVERED), CodeDependency("manuallyAddedDependency", CodeDependencyType.EXPLICIT)))
        assertThat(dependencies.get(sp2), empty())
        assertThat(dependencies.get(spChar), containsInAnyOrder(CodeDependency("sp2", CodeDependencyType.DISCOVERED), CodeDependency("sp1", CodeDependencyType.DISCOVERED)))
        assertThat(dependencies.get(spA), containsInAnyOrder(CodeDependency("sp#", CodeDependencyType.DISCOVERED)))
    }

    @Test
    fun testCalculateDependenciesAcrossSchemas() {
        val sp1 = newObject(SCHEMA1, "sp1", "sp1 sp2", Sets.immutable.empty(), Sets.immutable.with("manuallyAddedDependency"))
        val sp2 = newObject(SCHEMA2, "sp2", "sp2")
        val sp3 = newObject(SCHEMA1, "sp3", "sp3 sp1 sp2")
        val spA = newObject(SCHEMA2, "spA", "spA sp1 sp2 sp3", Sets.immutable.with("sp1", "sp2"), Sets.immutable.empty())

        val dependencies = enricher.calculateDependencies(Lists.mutable.with(sp1, sp2, sp3, spA))

        assertThat(dependencies.get(sp1), containsInAnyOrder(CodeDependency("sp2", CodeDependencyType.DISCOVERED), CodeDependency("manuallyAddedDependency", CodeDependencyType.EXPLICIT)))
        assertThat(dependencies.get(sp2), empty())
        assertThat(dependencies.get(sp3), containsInAnyOrder(CodeDependency("sp2", CodeDependencyType.DISCOVERED), CodeDependency("sp1", CodeDependencyType.DISCOVERED)))
        assertThat(dependencies.get(spA), containsInAnyOrder(CodeDependency("sp3", CodeDependencyType.DISCOVERED)))
    }

    @Test
    fun testCalculateDependenciesForChange() {
        val dependencies = enricher.calculateDependencies("test1", "create procedure sp1\n" +
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
                "", Sets.mutable.with("sp1", "sp2", "sp_3", "sp4", "sp5"))
        assertEquals(Sets.mutable.with("sp1", "sp_3", "sp4"), dependencies)
    }

    @Test
    fun testCalculateDependenciesForPrefix() {
        val dependencies = enricher.calculateDependencies("testPrefix",
                "create procedure sp1\nobj1\nobj1_prefix1\nobj1_prefix1_prefix2\n",
                Sets.mutable.with("sp1", "obj1"))
        assertEquals(Sets.mutable.with("sp1", "obj1"), dependencies)
    }

    private fun newObject(objectName: String, content: String): TextDependencyExtractable {
        return newObject(SCHEMA1, objectName, content)
    }

    private fun newObject(objectName: String, content: String, excludeDependencies: ImmutableSet<String>, includeDependencies: ImmutableSet<String>): TextDependencyExtractable {
        return newObject(SCHEMA1, objectName, content, excludeDependencies, includeDependencies)
    }

    private fun newObject(schema: String, objectName: String, content: String, excludeDependencies: ImmutableSet<String>? = null, includeDependencies: ImmutableSet<String>? = null): TextDependencyExtractable {
        val item = mock(TextDependencyExtractable::class.java)
        `when`(item.objectName).thenReturn(objectName)
        `when`(item.contentForDependencyCalculation).thenReturn(content)
        `when`(item.excludeDependencies).thenReturn(excludeDependencies ?: Sets.immutable.with())
        `when`(item.includeDependencies).thenReturn(includeDependencies ?: Sets.immutable.with())
        `when`(item.toString()).thenReturn(objectName)

        return item
    }

    companion object {
        private val SCHEMA1 = "schema1"
        private val SCHEMA2 = "schema2"
    }
}
