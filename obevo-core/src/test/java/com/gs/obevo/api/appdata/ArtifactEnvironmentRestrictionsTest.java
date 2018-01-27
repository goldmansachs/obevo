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
package com.gs.obevo.api.appdata;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactEnvironmentRestrictionsTest {
    @Test
    public void testAppliesForEnvironment() throws Exception {
        this.assertTest(true, "repo", UnifiedSet.newSetWith("repo", "abc"), UnifiedSet.<String>newSetWith());
        this.assertTest(true, "repo", UnifiedSet.newSetWith("repo*", "def"), UnifiedSet.<String>newSetWith());
        this.assertTest(true, "repo12345", UnifiedSet.newSetWith("ghi", "repo*"), UnifiedSet.<String>newSetWith());
        this.assertTest(false, "repo12345", UnifiedSet.newSetWith("repo"), UnifiedSet.<String>newSetWith());
        this.assertTest(false, "repa", UnifiedSet.newSetWith("repo"), UnifiedSet.<String>newSetWith());

        this.assertTest(false, "repo", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("repo", "abc"));
        this.assertTest(false, "repo", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("repo*", "def"));
        this.assertTest(false, "repo12345", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("ghi", "repo*"));
        this.assertTest(true, "repo12345", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("repo"));
        this.assertTest(true, "repa", UnifiedSet.<String>newSetWith(), UnifiedSet.newSetWith("repo"));
    }

    private void assertTest(boolean result, String envName, final MutableSet<String> includes,
            final MutableSet<String> excludes) {
        Environment env = new Environment();
        env.setName(envName);

        Restrictable restrictable = new Restrictable() {
            @Override
            public ImmutableList<ArtifactRestrictions> getRestrictions() {
                return Lists.immutable.<ArtifactRestrictions>of(new ArtifactEnvironmentRestrictions(includes, excludes));
            }
        };

        Assert.assertEquals(result, ArtifactRestrictions.apply().accept(restrictable, env));
    }
}
