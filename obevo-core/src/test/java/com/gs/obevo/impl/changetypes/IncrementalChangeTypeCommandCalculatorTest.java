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

import java.util.concurrent.atomic.AtomicInteger;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.api.platform.ChangeCommand;
import com.gs.obevo.api.platform.ChangePair;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.DeployMetricsCollectorImpl;
import com.gs.obevo.impl.command.AlreadyDroppedTableWarning;
import com.gs.obevo.impl.command.BaselineChangeCommand;
import com.gs.obevo.impl.command.DeployChangeCommand;
import com.gs.obevo.impl.command.DropObjectChangeCommand;
import com.gs.obevo.impl.command.ImproperlyRemovedWarning;
import com.gs.obevo.impl.command.IncompleteBaselineWarning;
import com.gs.obevo.impl.command.UndeployChangeCommand;
import com.gs.obevo.impl.command.UnrolledbackChangeWarning;
import com.gs.obevo.impl.command.UpdateAuditTableOnlyCommand;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.test.Verify;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IncrementalChangeTypeCommandCalculatorTest {
    private static final String CONTENT = "";

    private final IncrementalChangeTypeCommandCalculator cmdCalc = new IncrementalChangeTypeCommandCalculator(1);

    private final ImmutableList<Change> unusedChangesArg = Lists.immutable.with();
    private final AtomicInteger r = new AtomicInteger(0);

    @Test
    public void testNewTableAdd() {
        Change tabA1Src = new ChangeIncremental(tableChangeType(), "schema", "tabA", "new", 0, "chng1", CONTENT);

        ListIterable<ChangeCommand> changeCommands = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.<ChangePair>of(
                new ChangePair(tabA1Src, null)
        ), unusedChangesArg, false, false);

        assertEquals(1, changeCommands.size());
        Verify.assertAnySatisfy(changeCommands, assertValue(DeployChangeCommand.class, tabA1Src));
    }

    /**
     * For regular deploys, if a TABLE_DROP is the last for an object and there is no corresponding deployed change,
     * then we should not count this as part of the changeset to deploy.
     */
    @Test
    public void testTableDropsNormalDeploy() {
        testTableDrops(false);
    }

    /**
     * For environment cleaning, we do want to force the drops to happen.
     */
    @Test
    public void testTableDropsForceDropDuringCleaning() {
        testTableDrops(true);
    }

    private void testTableDrops(boolean forceDrop) {
        Change tabC1Dep = new ChangeIncremental(tableChangeType(), "schema", "tabC", "chng1", 0, "tabCExistingToBeDropped", CONTENT);
        Change tabC1Src = new ChangeIncremental(tableChangeType(), "schema", "tabC", tabC1Dep.getChangeName(), 0, "tabCExistingToBeDropped", CONTENT);
        Change tabC2Src = new ChangeIncremental(tableChangeType(), "schema", "tabC", "chng2", 1, "tabCExistingToBeDropped", CONTENT);
        Change tabC3Src = new ChangeIncremental(tableChangeType(), "schema", "tabC", "chng3", 2,
                "tabCExistingToBeDropped", CONTENT).withDrop(true).withKeepIncrementalOrder(true);
        Change tabD1Src = new ChangeIncremental(tableChangeType(), "schema", "tabD", "cdrop1", 0, "tabDNewTableIsDropped", CONTENT);
        ChangeIncremental tabD2Src = new ChangeIncremental(tableChangeType(), "schema", "tabD", "cdrop2", 1,
                "tabDNewTableIsDropped", CONTENT).withDrop(true).withKeepIncrementalOrder(true);
        tabD2Src.setForceDropForEnvCleaning(forceDrop);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.<ChangePair>of(
                new ChangePair(tabC1Src, tabC1Dep)
                , new ChangePair(tabC2Src, null)
                , new ChangePair(tabC3Src, null)
                , new ChangePair(tabD1Src, null)
                , new ChangePair(tabD2Src, null)
        ), unusedChangesArg, false, false);

        assertEquals(4, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, tabC2Src));
        Verify.assertAnySatisfy(changeset, assertValue(DropObjectChangeCommand.class, tabC3Src));
        if (forceDrop) {
            // if forceDrop, then ensure that the drops are actually executed
            Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, tabD1Src));
            Verify.assertAnySatisfy(changeset, assertValue(DropObjectChangeCommand.class, tabD2Src));
        } else {
            // as tabD was never previously deployed, we should not try to deploy it again
            Verify.assertAnySatisfy(changeset, assertValue(AlreadyDroppedTableWarning.class, tabD1Src));
            Verify.assertAnySatisfy(changeset, assertValue(AlreadyDroppedTableWarning.class, tabD2Src));
        }
    }

    @Test
    public void testTableActivationsAndRollbacks() {
        Change tabE0Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "0", 0, "chng0", CONTENT);
        Change tabE0Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "0", 0, "chng0", CONTENT);
        Change tabE1Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "1", 1, "chng1ToActivate", CONTENT, null, false);
        Change tabE1Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "1", 1, "chng1ToActivate", CONTENT, null, true);
        Change tabE2Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "2", 2, "chng2ToDeactivate", CONTENT, null, true);
        Change tabE2Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "2", 2, "chng2ToDeactivate", CONTENT, null, false);
        Change tabE3Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "3", 3, "chng3alreadyDeprecated,DoNotDeploy", CONTENT, null, false);
        Change tabE4Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "4", 4, "chng4ActualChange", CONTENT);

        // in this case, a change is rolled back but also deactivated. The result is that we just deactivate. We do not proceed w/ the rollback until the change is activated
        Change tabE5Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "5", 5, "chng5RollbackButInactive", CONTENT, null, true);
        Change tabE5Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "5", 5, "chng5RollbackButInactive", CONTENT, "rollback", false);
        // once the change is activated, we will roll back
        Change tabE6Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "6", 6, "chng6ActivateAndThenRollback", CONTENT, null, false);
        Change tabE6Src = new ChangeIncremental(tableChangeType(), "schema", "tabE", "6", 6, "chng6ActivateAndThenRollback", CONTENT, "rollback", true);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.of(
                new ChangePair(tabE0Src, tabE0Dep)
                , new ChangePair(tabE1Src, tabE1Dep)
                , new ChangePair(tabE2Src, tabE2Dep)
                , new ChangePair(tabE3Src, null)
                , new ChangePair(tabE4Src, null)
                , new ChangePair(tabE5Src, tabE5Dep)
                , new ChangePair(tabE6Src, tabE6Dep)
        ), unusedChangesArg, false, false);

        // Verify the activation statuses were changed accordingly
        assertEquals(true, tabE1Dep.isActive());
        assertEquals(true, tabE6Dep.isActive());
        assertEquals(false, tabE2Dep.isActive());
        assertEquals(false, tabE5Dep.isActive());

        assertEquals(6, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, tabE4Src));
        Verify.assertAnySatisfy(changeset, assertValue(UndeployChangeCommand.class, tabE6Src));
        Verify.assertAnySatisfy(changeset, assertValue(UpdateAuditTableOnlyCommand.class, tabE6Dep));
        Verify.assertAnySatisfy(changeset, assertValue(UpdateAuditTableOnlyCommand.class, tabE5Dep));
        Verify.assertAnySatisfy(changeset, assertValue(UpdateAuditTableOnlyCommand.class, tabE2Dep));
        Verify.assertAnySatisfy(changeset, assertValue(UpdateAuditTableOnlyCommand.class, tabE1Dep));
    }

    @Test
    public void testImproperlyDroppedSourceChange() {
        Change tabE0Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "0", 0, "chng0", CONTENT);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.<ChangePair>of(
                new ChangePair(null, tabE0Dep)
        ), unusedChangesArg, false, false);

        assertEquals(1, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(ImproperlyRemovedWarning.class, tabE0Dep));
    }

    /**
     * Same input data as the testImproperDroppedSourceChange test, but this time, we run with rollback. Hence, we allow it to go through
     * without an exception, btu we log a warning message
     */
    @Test
    public void testDroppedSourceChangeWithRollback() {
        Change tabE0Dep = new ChangeIncremental(tableChangeType(), "schema", "tabE", "0", 0, "chng0", CONTENT);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.<ChangePair>of(
                new ChangePair(null, tabE0Dep)
        ), unusedChangesArg, true, false);

        assertEquals(1, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(UnrolledbackChangeWarning.class, tabE0Dep));
    }

    @Test
    public void testIncrementalTableChange() {
        Change tabB0Dep = new ChangeIncremental(tableChangeType(), "schema", "tabB", "b0", 0, "chng1", CONTENT);
        Change tabB0Src = new ChangeIncremental(tableChangeType(), "schema", "tabB", "b0", 0, "chng1", CONTENT);
        Change tabB1FkSrc = new ChangeIncremental(foreignKeyChangeType(), "schema", "tabB", "b1", 1, "chng1.5fk", CONTENT);
        Change tabB3Dep = new ChangeIncremental(tableChangeType(), "schema", "tabB", "b3", 3, "chng3ToBeRolledback1", CONTENT);
        Change tabB3Src = new ChangeIncremental(tableChangeType(), "schema", "tabB", tabB3Dep.getChangeName(), 3, "chng3ToBeRolledback1", CONTENT, "rollback", true);
        Change tabB4Src = new ChangeIncremental(tableChangeType(), "schema", "tabB", "b4", 4, "chng4Insertion", CONTENT);
        Change tabB5Src = new ChangeIncremental(tableChangeType(), "schema", "tabB", "b5", 5, "chng5AlreadyRolledBack", CONTENT, "rollback", true);
        Change tabB6Dep = new ChangeIncremental(tableChangeType(), "schema", "tabB", "b6", 6, "chng6ToBeRolledback2", CONTENT);
        Change tabB6Src = new ChangeIncremental(tableChangeType(), "schema", "tabB", tabB6Dep.getChangeName(), 6, "chng6ToBeRolledback2", CONTENT, "rollback", true);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.of(
                new ChangePair(tabB0Src, tabB0Dep)
                , new ChangePair(tabB1FkSrc, null)
                , new ChangePair(tabB3Src, tabB3Dep)
                , new ChangePair(tabB4Src, null)
                , new ChangePair(tabB5Src, null)
                , new ChangePair(tabB6Src, tabB6Dep)
        ), unusedChangesArg, false, false);

        assertEquals(4, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(UndeployChangeCommand.class, tabB3Src));
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, tabB4Src));
        Verify.assertAnySatisfy(changeset, assertValue(UndeployChangeCommand.class, tabB6Src));
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, tabB1FkSrc));
    }

    @Test
    public void testBaseline() {
        Change dep1 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch1", 0, "chng1", CONTENT);
        Change dep2 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch2", 0, "chng1", CONTENT);
        Change dep3 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch3", 0, "chng1", CONTENT);
        Change srcB = new ChangeIncremental(tableChangeType(), "schema", "tabB", "bas1", 0, "chng1", CONTENT)
                .withBaselines(Lists.mutable.with("ch1", "ch2", "ch3"));
        Change src4 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch4", 1, "chng1", CONTENT);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.of(
                new ChangePair(srcB, null)
                , new ChangePair(src4, null)
                , new ChangePair(null, dep1)
                , new ChangePair(null, dep2)
                , new ChangePair(null, dep3)
        ), unusedChangesArg, false, false);

        assertEquals(2, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, src4));

        Predicate<ChangeCommand> baselineCommandPredicate = assertValue(BaselineChangeCommand.class, srcB);
        Verify.assertAnySatisfy(changeset, baselineCommandPredicate);

        BaselineChangeCommand baselineCommand = (BaselineChangeCommand) changeset.detect(baselineCommandPredicate);

        assertEquals(Sets.mutable.with(dep1, dep2, dep3), baselineCommand.getReplacedChanges().toSet());
    }

    @Test
    public void testBaselineNewAddition() {
        // dep1, dep2, dep3 are not deployed - hence, we should deploy the baseline we find in the source
        Change srcB = new ChangeIncremental(tableChangeType(), "schema", "tabB", "bas1", 0, "chng1", CONTENT)
                .withBaselines(Lists.mutable.with("ch1", "ch2", "ch3"));
        Change src4 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch4", 1, "chng1", CONTENT);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.of(
                new ChangePair(srcB, null)
                , new ChangePair(src4, null)
        ), unusedChangesArg, false, false);

        assertEquals(2, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, srcB));
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, src4));
    }

    @Test
    public void testBaselineException() {
        // In this use case, srcB is the baseline change w/ ch1 ch2 ch3 marked. However, we only see ch1 and ch2 deployed, so we throw an exception

        Change dep1 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch1", 0, "chng1", CONTENT);
        Change dep2 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch2", 0, "chng1", CONTENT);
        // hiding dep3 as to show the exception use case
        //Change dep3 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch3", 0, "chng1", CONTENT);
        Change srcB = new ChangeIncremental(tableChangeType(), "schema", "tabB", "bas1", 0, "chng1", CONTENT)
                .withBaselines(Lists.mutable.with("ch1", "ch2", "ch3"));
        Change src4 = new ChangeIncremental(tableChangeType(), "schema", "tabB", "ch4", 1, "chng1", CONTENT);

        ListIterable<ChangeCommand> changeset = cmdCalc.calculateCommands(tableChangeType(), Lists.mutable.of(
                new ChangePair(srcB, null)
                , new ChangePair(src4, null)
                , new ChangePair(null, dep1)
                , new ChangePair(null, dep2)
        ), unusedChangesArg, false, false);

        assertEquals(2, changeset.size());
        Verify.assertAnySatisfy(changeset, assertValue(DeployChangeCommand.class, src4));
        Predicate<ChangeCommand> baselineWarningPredicate = assertValue(IncompleteBaselineWarning.class, srcB);
        Verify.assertAnySatisfy(changeset, baselineWarningPredicate);
        IncompleteBaselineWarning baselineWarning = (IncompleteBaselineWarning) changeset.detect(baselineWarningPredicate);
        assertEquals(Sets.mutable.with("ch3"), baselineWarning.getNonDeployedChanges());
    }

    private ChangeType tableChangeType() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("table");
        return changeType;
    }

    private ChangeType foreignKeyChangeType() {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn("fk");
        return changeType;
    }


    static Predicate<ChangeCommand> assertValue(final Class expectedClass, final Change expectedArtifact) {
        return new Predicate<ChangeCommand>() {
            @Override
            public boolean accept(ChangeCommand command) {
                return expectedClass.isAssignableFrom(command.getClass())
                        && expectedArtifact.getObjectName().equals(command.getChanges().getFirst().getObjectName())
                        && expectedArtifact.getChangeName().equals(command.getChanges().getFirst().getChangeName());
            }
        };
    }
}
