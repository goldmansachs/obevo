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
package com.gs.obevo.impl.changesorter;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.appdata.ObjectKey;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.impl.ExecuteChangeCommand;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangeCommandSorterImplTest {
    private ChangeCommandSorterImpl sorter;

    @Before
    public void setupSorter() {
        Platform dialect = mock(Platform.class);
        when(dialect.convertDbObjectName()).thenReturn(Functions.getStringPassThru());
        when(dialect.isDropOrderRequired()).thenReturn(false);

        this.sorter = new ChangeCommandSorterImpl(dialect);
    }


    @Test
    public void testSortWithFk() throws Exception {
        final ExecuteChangeCommand aTab1 = newIncrementalCommand(tableChangeType(), "ATab", "1", Sets.immutable.<String>of(), 1);
        final ExecuteChangeCommand aTab2 = newIncrementalCommand(tableChangeType(), "ATab", "2", Sets.immutable.<String>of(), 2);
        final ExecuteChangeCommand aTab3 = newIncrementalCommand(tableChangeType(), "ATab", "3", Sets.immutable.<String>of(), 3);
        final ExecuteChangeCommand bTab1 = newIncrementalCommand(tableChangeType(), "BTab", "1", Sets.immutable.<String>of(), 1);
        final ExecuteChangeCommand bTab2 = newIncrementalCommand(tableChangeType(), "BTab", "2", Sets.immutable.<String>of("ATab"), 2);
        final ExecuteChangeCommand bTab3 = newIncrementalCommand(tableChangeType(), "BTab", "3", Sets.immutable.<String>of(), 3);
        final ListIterable<ExecuteChangeCommand> sortedCommands = sorter.sort(Lists.mutable.of(
                aTab1
                , aTab2
                , aTab3
                , bTab1
                , bTab2
                , bTab3
        ), false);

        // assert basic order
        assertThat("aTab changes should be in order", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(aTab2)));
        assertThat("aTab changes should be in order", sortedCommands.indexOf(aTab2), Matchers.lessThan(sortedCommands.indexOf(aTab3)));
        assertThat("bTab changes should be in order", sortedCommands.indexOf(bTab1), Matchers.lessThan(sortedCommands.indexOf(bTab2)));
        assertThat("bTab changes should be in order", sortedCommands.indexOf(bTab2), Matchers.lessThan(sortedCommands.indexOf(bTab3)));

        // assert cross-object dependency
        assertThat("assert bTab change depending on aTab comes after tabA", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(bTab2)));
    }

    @Test
    public void testSortWithMixedDependency() throws Exception {
        final ExecuteChangeCommand aTab1 = newIncrementalCommand(tableChangeType(), "aTab", "1", Sets.immutable.<String>of("bTab.1"), 1);
        final ExecuteChangeCommand aTab2 = newIncrementalCommand(tableChangeType(), "aTab", "2", Sets.immutable.<String>of("bTab.2"), 2);
        final ExecuteChangeCommand bTab1 = newIncrementalCommand(tableChangeType(), "bTab", "1", Sets.immutable.<String>of(), 1);
        final ExecuteChangeCommand bTab2 = newIncrementalCommand(tableChangeType(), "bTab", "2", Sets.immutable.<String>of(), 2);
        final ListIterable<ExecuteChangeCommand> sortedCommands = sorter.sort(Lists.mutable.of(
                aTab1
                , aTab2
                , bTab1
                , bTab2
        ), false);

        // assert basic order
        assertThat("bTab.1 comes before aTab.1 due to FK dependency", sortedCommands.indexOf(bTab1), Matchers.lessThan(sortedCommands.indexOf(aTab1)));
        assertThat("bTab.2 must come after aTab.1 and before aTab.2 as aTab.2 depends explicitly on bTab2.", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(bTab2)));
        assertThat("bTab.2 must come after aTab.1 and before aTab.2 as aTab.2 depends explicitly on bTab2.", sortedCommands.indexOf(bTab2), Matchers.lessThan(sortedCommands.indexOf(aTab2)));
    }

//    @Test
//    public void addTestForDropOrderToo() {
//        // -Consider that drop order is needed on DB2 functions (i.e. across functions, func depend on table depend on func), though no such restrictions exist for views and sps
//        throw new UnsupportedOperationException("Not yet implemented");
//    }

    @Test
    public void testFunc2TableToFuncDependency() throws Exception {
        final ExecuteChangeCommand func1 = newCommand(viewChangeType(), "Func1", "n/a", Sets.immutable.<String>of());
        final ExecuteChangeCommand func2 = newCommand(viewChangeType(), "Func2", "n/a", Sets.immutable.<String>of("Func1"));
        final ExecuteChangeCommand aTab1 = newIncrementalCommand(tableChangeType(), "ATab", "1", Sets.immutable.<String>of("Func2"), 1);
        final ExecuteChangeCommand aTab2 = newIncrementalCommand(tableChangeType(), "ATab", "2", Sets.immutable.<String>of(), 2);
        final ExecuteChangeCommand funcOnTab3 = newCommand(viewChangeType(), "FuncOnTab3", "n/a", Sets.immutable.<String>of("ATab"));


        ListIterable<ExecuteChangeCommand> sortedCommands = sorter.sort(Lists.mutable.of(
                func1, func2, aTab1, aTab2, funcOnTab3
        ), false);

        assertThat("func1 should precede func2", sortedCommands.indexOf(func1), Matchers.lessThan(sortedCommands.indexOf(func2)));
        assertThat("func2 should precede atab1", sortedCommands.indexOf(func2), Matchers.lessThan(sortedCommands.indexOf(aTab1)));
        assertThat("aTab changes should remain in order", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(aTab2)));
        assertThat("function should be created after the table", sortedCommands.indexOf(aTab1), Matchers.lessThan(sortedCommands.indexOf(funcOnTab3)));
    }

    private ExecuteChangeCommand newCommand(ChangeType changeType, String objectName, String changeName, ImmutableSet<String> dependencies) {
        Change change = mock(Change.class);
        when(change.getObjectKey()).thenReturn(new ObjectKey("schema", changeType, objectName));
        when(change.getSchema()).thenReturn("schema");
        when(change.getChangeType()).thenReturn(changeType);
        when(change.getObjectName()).thenReturn(objectName);
        when(change.getChangeName()).thenReturn(changeName);
        when(change.getDependencies()).thenReturn(dependencies);
        change.getContentForDependencyCalculation();


        ExecuteChangeCommand command = mock(ExecuteChangeCommand.class);
        when(command.isDrop()).thenReturn(false);
        when(command.getChanges()).thenReturn(Lists.immutable.of(change));

        return command;
    }

    private ExecuteChangeCommand newIncrementalCommand(ChangeType changeType, String objectName, String changeName, ImmutableSet<String> dependencies, int orderWithinObject) {
        ChangeIncremental change = mock(ChangeIncremental.class);
        when(change.getObjectKey()).thenReturn(new ObjectKey("schema", changeType, objectName));
        when(change.getSchema()).thenReturn("schema");
        when(change.getChangeType()).thenReturn(changeType);
        when(change.getObjectName()).thenReturn(objectName);
        when(change.getChangeName()).thenReturn(changeName);
        when(change.getDependencies()).thenReturn(dependencies);
        when(change.getOrderWithinObject()).thenReturn(orderWithinObject);


        ExecuteChangeCommand command = mock(ExecuteChangeCommand.class);
        when(command.isDrop()).thenReturn(false);
        when(command.getChanges()).thenReturn(Lists.immutable.<Change>of(change));

        return command;
    }

    private ChangeType tableChangeType() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("table");
        when(changeType.isRerunnable()).thenReturn(false);
        return changeType;
    }

    private ChangeType viewChangeType() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("view");
        when(changeType.isRerunnable()).thenReturn(true);
        return changeType;
    }

}
