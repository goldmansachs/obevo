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
package com.gs.obevo.api.appdata;

import com.gs.obevo.api.platform.Platform;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactPlatformRestrictionsTest {

    @Test
    public void testAppliesForEnvironment() throws Exception {
        assertTest(true, "HSQL", UnifiedSet.newSetWith("HSQL", "DB2"), UnifiedSet.<String>newSetWith());
        assertTest(true, "HSQL", UnifiedSet.newSetWith("HSQL*", "HIVE"), UnifiedSet.<String>newSetWith());
        assertTest(true, "SYBASE_IQ", UnifiedSet.newSetWith("HSQL", "SYBASE*"), UnifiedSet.<String>newSetWith());
        assertTest(false, "DB2", UnifiedSet.newSetWith("HSQL"), UnifiedSet.<String>newSetWith());

        assertTest(false, "HSQL", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("HSQL", "DB2"));
        assertTest(false, "HSQL", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("HSQL*", "HIVE"));
        assertTest(false, "SYBASE_IQ", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("HSQL", "SYBASE*"));
        assertTest(true, "SYBASE_IQ", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("SYBASE"));
        assertTest(true, "SYBASE_IQ", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("SYBASE_ASE"));
    }

    private void assertTest(boolean result, final String platformName, final MutableSet<String> includes,
                            final MutableSet<String> excludes) {
        Environment env = new Environment();

        Platform platform = mock(Platform.class);
        when(platform.getName()).thenReturn(platformName);
        env.setPlatform(platform);

        Restrictable restrictable = new Restrictable() {
            @Override
            public ImmutableList<ArtifactRestrictions> getRestrictions() {
                return Lists.immutable.<ArtifactRestrictions>of(new ArtifactPlatformRestrictions(includes, excludes));
            }
        };

        Assert.assertEquals(result, ArtifactRestrictions.apply().accept(restrictable, env));
    }


}