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
package com.gs.obevo.impl.changesorter

import com.gs.obevo.api.appdata.ChangeIncremental
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.ObjectKey
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.Platform
import com.gs.obevo.impl.ExecuteChangeCommand
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.block.factory.Functions
import org.eclipse.collections.impl.factory.Sets
import org.hamcrest.Matchers
import org.junit.Assert.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ChangeCommandSorterImplTest {
    private val sorter: ChangeCommandSorterImpl

    init {
        val convertObjectName = Functions.getStringPassThru()

        val dialect = mock(Platform::class.java)
        `when`(dialect.isDropOrderRequired).thenReturn(false)
        `when`(dialect.convertDbObjectName()).thenReturn(convertObjectName)

        this.sorter = ChangeCommandSorterImpl(dialect)
    }

    @Test
    @Throws(Exception::class)
    fun testSortWithFk() {
        val aTab1 = newCommand(tableChangeType(), "ATab", "1", Sets.immutable.of())
        val aTab2 = newCommand(tableChangeType(), "ATab", "2", Sets.immutable.of(aTab1))
        val aTab3 = newCommand(tableChangeType(), "ATab", "3", Sets.immutable.of(aTab2))
        val bTab1 = newCommand(tableChangeType(), "BTab", "1", Sets.immutable.of())
        val bTab2 = newCommand(tableChangeType(), "BTab", "2", Sets.immutable.of(bTab1, aTab3))
        val bTab3 = newCommand(tableChangeType(), "BTab", "3", Sets.immutable.of(bTab2))
        val sortedCommands = sorter.sort(listOf(aTab1, aTab2, aTab3, bTab1, bTab2, bTab3))

        // assert basic order
        assertThat("aTab changes should be in order", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(aTab2)))
        assertThat("aTab changes should be in order", sortedCommands.indexOf(aTab2), Matchers.lessThan(sortedCommands.indexOf(aTab3)))
        assertThat("bTab changes should be in order", sortedCommands.indexOf(bTab1), Matchers.lessThan(sortedCommands.indexOf(bTab2)))
        assertThat("bTab changes should be in order", sortedCommands.indexOf(bTab2), Matchers.lessThan(sortedCommands.indexOf(bTab3)))

        // assert cross-object dependency
        assertThat("assert bTab change depending on aTab comes after tabA", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(bTab2)))
    }

    @Test
    @Throws(Exception::class)
    fun testSortWithMixedDependency() {
        val bTab1 = newCommand(tableChangeType(), "bTab", "1", Sets.immutable.of())
        val bTab2 = newCommand(tableChangeType(), "bTab", "2", Sets.immutable.of(bTab1))
        val aTab1 = newCommand(tableChangeType(), "aTab", "1", Sets.immutable.of(bTab1))
        val aTab2 = newCommand(tableChangeType(), "aTab", "2", Sets.immutable.of(aTab1, bTab2))
        val sortedCommands = sorter.sort(listOf(aTab1, aTab2, bTab1, bTab2))

        // assert basic order
        assertThat("bTab.1 comes before aTab.1 due to FK dependency", sortedCommands.indexOf(bTab1), Matchers.lessThan(sortedCommands.indexOf(aTab1)))
        assertThat("bTab.2 must come after aTab.1 and before aTab.2 as aTab.2 depends explicitly on bTab2.", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(bTab2)))
        assertThat("bTab.2 must come after aTab.1 and before aTab.2 as aTab.2 depends explicitly on bTab2.", sortedCommands.indexOf(bTab2), Matchers.lessThan(sortedCommands.indexOf(aTab2)))
    }

    //    @Test
    //    public void addTestForDropOrderToo() {
    //        // -Consider that drop order is needed on DB2 functions (i.e. across functions, func depend on table depend on func), though no such restrictions exist for views and sps
    //        throw new UnsupportedOperationException("Not yet implemented");
    //    }

    @Test
    @Throws(Exception::class)
    fun testFunc2TableToFuncDependency() {
        val func1 = newCommand(viewChangeType(), "Func1", "n/a", Sets.immutable.of())
        val func2 = newCommand(viewChangeType(), "Func2", "n/a", Sets.immutable.of(func1))
        val aTab1 = newCommand(tableChangeType(), "ATab", "1", Sets.immutable.of(func2))
        val aTab2 = newCommand(tableChangeType(), "ATab", "2", Sets.immutable.of(aTab1))
        val funcOnTab3 = newCommand(viewChangeType(), "FuncOnTab3", "n/a", Sets.immutable.of(aTab2))

        val sortedCommands = sorter.sort(listOf(func1, func2, aTab1, aTab2, funcOnTab3))

        assertThat("func1 should precede func2", sortedCommands.indexOf(func1), Matchers.lessThan(sortedCommands.indexOf(func2)))
        assertThat("func2 should precede atab1", sortedCommands.indexOf(func2), Matchers.lessThan(sortedCommands.indexOf(aTab1)))
        assertThat("aTab changes should remain in order", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(aTab2)))
        assertThat("function should be created after the table", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(funcOnTab3)))
    }

    private fun newCommand(changeType: ChangeType, objectName: String, changeName: String, dependencies: ImmutableSet<ExecuteChangeCommand>): ExecuteChangeCommand {
        val change = ChangeIncremental(ChangeKey(ObjectKey("schema", objectName, changeType), changeName), 99999, "n/a", "n/a")
        change.dependentChanges = dependencies.flatCollect { it.changes }

        val command = mock(ExecuteChangeCommand::class.java)
        `when`(command.isDrop).thenReturn(false)
        `when`(command.changes).thenReturn(listOf(change))

        return command
    }

    private fun tableChangeType(): ChangeType {
        val changeType = mock(ChangeType::class.java)
        `when`(changeType.name).thenReturn("table")
        `when`(changeType.isRerunnable).thenReturn(false)
        return changeType
    }

    private fun viewChangeType(): ChangeType {
        val changeType = mock(ChangeType::class.java)
        `when`(changeType.name).thenReturn("view")
        `when`(changeType.isRerunnable).thenReturn(true)
        return changeType
    }
}
