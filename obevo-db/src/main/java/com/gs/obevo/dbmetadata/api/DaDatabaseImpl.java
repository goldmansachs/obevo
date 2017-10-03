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
package com.gs.obevo.dbmetadata.api;

import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.impl.factory.Lists;

public class DaDatabaseImpl implements DaCatalog {
    private ImmutableCollection<DaTable> tables = Lists.immutable.empty();

    public DaDatabaseImpl() {
    }

    @Override
    public ImmutableCollection<DaTable> getTables() {
        return tables;
    }

    public void setTables(ImmutableCollection<DaTable> tables) {
        this.tables = tables;
    }

    @Override
    public ImmutableCollection<DaRoutine> getRoutines() {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaRule> getRules() {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<RuleBinding> getRuleBindings() {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaSequence> getSequences() {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaUserType> getUserTypes() {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaPackage> getPackages() {
        return Lists.immutable.empty();
    }
}
