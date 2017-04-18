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
package com.gs.obevo.impl.changetypes;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeRerunnable;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ChangePair;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.command.DeployChangeCommand;
import com.gs.obevo.impl.command.DropObjectChangeCommand;
import com.gs.obevo.impl.graph.GraphEnricher;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.test.Verify;
import org.junit.Test;

import static com.gs.obevo.impl.changetypes.IncrementalChangeTypeCommandCalculatorTest.assertValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RerunnableChangeTypeCommandCalculatorTest {
    private static final String CONTENT = "";

    private final RerunnableChangeTypeCommandCalculator cmdCalc = new RerunnableChangeTypeCommandCalculator(mock(GraphEnricher.class));


    @Test
    public void testSimpleViews() {
        Change view1Dep = new ChangeRerunnable(viewChangeType(), "schema", "viewA", "hash", CONTENT);
        Change view1Src = new ChangeRerunnable(viewChangeType(), "schema", "viewA", "hashdiff", CONTENT);
        Change view2Dep = new ChangeRerunnable(viewChangeType(), "schema", "viewB", "samehash", CONTENT);
        Change view2Src = new ChangeRerunnable(viewChangeType(), "schema", "viewB", "samehash", CONTENT);
        Change view3Dep = new ChangeRerunnable(viewChangeType(), "schema", "viewC", "deletion", CONTENT);
        Change view4Src = new ChangeRerunnable(viewChangeType(), "schema", "viewD", "addition", CONTENT);

        MutableList<Change> allSourceChanges = Lists.mutable.with(
                view1Src, view2Src, view4Src
        );

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(viewChangeType(), Lists.mutable.of(
                new ChangePair(view1Src, view1Dep)
                , new ChangePair(view2Src, view2Dep)
                , new ChangePair(null, view3Dep)
                , new ChangePair(view4Src, null)
        ), allSourceChanges, false, false);

        assertEquals(3, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, view1Src));
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, view4Src));
        Verify.assertAnySatisfy(changeset, assertValue(DropObjectChangeCommand.class, view3Dep));
    }

    private ChangeType viewChangeType() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("view");
        when(changeType.isRerunnable()).thenReturn(true);
        return changeType;
    }

}