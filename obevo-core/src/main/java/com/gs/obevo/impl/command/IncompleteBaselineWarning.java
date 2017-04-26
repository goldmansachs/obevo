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
package com.gs.obevo.impl.command;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.impl.ChangeCommandWarning;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;

public class IncompleteBaselineWarning implements ChangeCommandWarning {
    private final ChangeIncremental source;
    private final ImmutableSet<String> nonDeployedChanges;

    public IncompleteBaselineWarning(ChangeIncremental source, ImmutableSet<String> nonDeployedChanges) {
        this.source = source;
        this.nonDeployedChanges = nonDeployedChanges;
    }

    @Override
    public String getCommandDescription() {
        return "Baseline change " + this.source + " specified these changes to deploy [" + this.source.getBaselinedChanges() + "], but these changes [" + this.nonDeployedChanges + "] were not actually deployed";
    }

    @Override
    public boolean isFatal() {
        return true;
    }

    @Override
    public ImmutableList<Change> getChanges() {
        return Lists.immutable.<Change>of(this.source);
    }

    /**
     * Only for testing
     */
    public ImmutableSet<String> getNonDeployedChanges() {
        return this.nonDeployedChanges;
    }
}
