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
package com.gs.obevo.impl.command;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.GroupChange;
import com.gs.obevo.impl.AuditChangeCommand;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

public abstract class AuditOnlyChangeCommand implements AuditChangeCommand {
    private final Change artifact;

    AuditOnlyChangeCommand(Change artifact) {
        this.artifact = artifact;
    }

    Change getArtifact() {
        return this.artifact;
    }

    @Override
    public String getCommandDescription() {
        return this.artifact.getDisplayString();
    }

    @Override
    public ImmutableList<Change> getChanges() {
        if (this.artifact instanceof GroupChange) {
            return ((GroupChange) artifact).getChanges();
        } else {
            return Lists.immutable.with(this.artifact);
        }
    }

    @Override
    public String getSchema() {
        return getChanges().getFirst().getSchema();
    }
}
