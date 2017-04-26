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
package com.gs.obevo.impl;

import com.gs.obevo.api.appdata.DeployExecution;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultRollbackDetectorTest {
    private final DefaultRollbackDetector rollbackDetector = new DefaultRollbackDetector();

    @Test
    public void testDetermineRollbackForSchema() throws Exception {
        assertFalse("Deploying a new version (w/ no existing versions) is not a rollback", rollbackDetector.determineRollbackForSchema("new version", Sets.immutable.<DeployExecution>empty()));

        assertFalse("Deploying a new version is not a rollback", rollbackDetector.determineRollbackForSchema("new version", Sets.immutable.with(
                newExecution(3, "a")
                , newExecution(4, "b")
                , newExecution(5, "c")
        )));

        assertFalse("Deploying the same version as the latest is not a rollback", rollbackDetector.determineRollbackForSchema("c", Sets.immutable.with(
                newExecution(3, "a")
                , newExecution(4, "b")
                , newExecution(5, "c")
        )));

        assertTrue("Deploying earlier versions is a rollback", rollbackDetector.determineRollbackForSchema("b", Sets.immutable.with(
                newExecution(3, "a")
                , newExecution(4, "b")
                , newExecution(5, "c")
        )));

        assertTrue("Deploying earlier versions is a rollback", rollbackDetector.determineRollbackForSchema("a", Sets.immutable.with(
                newExecution(3, "a")
                , newExecution(4, "b")
                , newExecution(5, "c")
        )));

        assertTrue("Deploying earlier versions is a rollback", rollbackDetector.determineRollbackForSchema("b", Sets.immutable.with(
                newExecution(3, "a")
                , newExecution(4, "b")
                , newExecution(5, "c")
                , newExecution(6, "c")
        )));

        assertTrue("Deploying earlier versions is a rollback", rollbackDetector.determineRollbackForSchema("b", Sets.immutable.with(
                newExecution(3, "a")
                , newExecution(4, "b")
                , newExecution(5, "b")
                , newExecution(6, "c")
                , newExecution(7, "c")
        )));
    }

    @Test
    public void testGetActiveDeploymentsOnEmptyInput() throws Exception {
        assertEquals(Lists.immutable.empty(), rollbackDetector.getActiveDeployments(null));
        assertEquals(Lists.immutable.empty(), rollbackDetector.getActiveDeployments(Lists.immutable.<DeployExecution>empty()));
    }

    @Test
    public void testGetActiveDeploymentsOnNormalCase() throws Exception {
        assertEquals(Lists.immutable.with(1L), rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "a")
        )).collect(DeployExecution.TO_ID));

        assertEquals(Lists.immutable.with(1L, 2L, 3L), rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "a")
                , newExecution(2, "b")
                , newExecution(3, "c")
        )).collect(DeployExecution.TO_ID));
    }

    @Test
    public void testGetActiveDeploymentsWithRollback() throws Exception {
        assertEquals(Lists.immutable.with(3L), rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "a")
                , newExecution(2, "b")
                , newExecution(3, "a", true)  // we go back to a; hence, erasing the impact of b
        )).collect(DeployExecution.TO_ID));

        assertEquals(Lists.immutable.with(3L, 4L, 5L), rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "a")
                , newExecution(2, "b")
                , newExecution(3, "a", true)  // we go back to a; hence, erasing the impact of b
                , newExecution(4, "b")
                , newExecution(5, "c")
        )).collect(DeployExecution.TO_ID));
    }

    /**
     * This is for some legacy edge cases where the product version column was not consistently updated.
     */
    @Test
    public void testWithNullVersionNames() throws Exception {
        assertEquals(Lists.immutable.with(0L, 3L, 7L, 8L), rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(0, null)
                , newExecution(1, "a")
                , newExecution(2, null)
                , newExecution(3, "a", true)  // we go back to a; hence, erasing the impact of execution #2
                , newExecution(4, "c")
                , newExecution(5, "d")
                , newExecution(6, null)
                , newExecution(7, "c", true)
                , newExecution(8, null)
        )).collect(DeployExecution.TO_ID));
    }

    @Test
    public void testGetActiveDeploymentsWithManyRollbacks() throws Exception {
        assertEquals(Lists.immutable.with(3L, 7L, 8L), rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "a")
                , newExecution(2, "b")
                , newExecution(3, "a", true)  // we go back to a; hence, erasing the impact of b
                , newExecution(4, "c")
                , newExecution(5, "d")
                , newExecution(6, "e")
                , newExecution(7, "c", true)
                , newExecution(8, "e")
        )).collect(DeployExecution.TO_ID));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetActiveDeploymentsInvalidExtraRollback() throws Exception {
        rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "illegal", true)
        ));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetActiveDeploymentsInvalidExtraRollback2() throws Exception {
        rollbackDetector.getActiveDeployments(Sets.immutable.with(
                newExecution(1, "a")
                , newExecution(2, "b")
                , newExecution(3, "a", true)
                , newExecution(4, "c")
                , newExecution(5, "d")
                , newExecution(6, "e")
                , newExecution(7, "illegal", true)  // the unreferenced rollback
                , newExecution(8, "e")
        ));
    }

    private DeployExecution newExecution(long id, String versionName) {
        return newExecution(id, versionName, false);
    }

    private DeployExecution newExecution(long id, String versionName, boolean rollback) {
        DeployExecution exec = mock(DeployExecution.class);
        when(exec.getId()).thenReturn(id);
        when(exec.isRollback()).thenReturn(rollback);
        when(exec.getProductVersion()).thenReturn(versionName);

        return exec;
    }
}
