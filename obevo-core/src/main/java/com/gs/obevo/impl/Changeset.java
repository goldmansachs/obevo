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
package com.gs.obevo.impl;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;

public class Changeset {
    private final ImmutableList<ExecuteChangeCommand> inserts;
    private final ImmutableList<ExecuteChangeCommand> deferredChanges;
    private final RichIterable<AuditChangeCommand> auditChanges;
    private final RichIterable<ChangeCommandWarning> changeWarnings;

    public Changeset(ImmutableList<ExecuteChangeCommand> inserts, ImmutableList<ExecuteChangeCommand> deferredChanges, RichIterable<AuditChangeCommand> auditChanges, RichIterable<ChangeCommandWarning> changeWarnings) {
        this.inserts = inserts;
        this.deferredChanges = deferredChanges;
        this.auditChanges = auditChanges;
        this.changeWarnings = changeWarnings;
    }

    public ImmutableList<ExecuteChangeCommand> getInserts() {
        return this.inserts;
    }

    public ImmutableList<ExecuteChangeCommand> getDeferredChanges() {
        return deferredChanges;
    }

    public RichIterable<AuditChangeCommand> getAuditChanges() {
        return this.auditChanges;
    }

    public RichIterable<ChangeCommandWarning> getChangeWarnings() {
        return this.changeWarnings;
    }

    public void validateForDeployment() {
        RichIterable<ChangeCommandWarning> fatalWarnings = this.changeWarnings.select(ChangeCommandWarning.IS_FATAL);

        if (!fatalWarnings.isEmpty()) {
            // check for serious exceptions
            throw new IllegalArgumentException("Found exceptions:\n"
                    + fatalWarnings.collect(ChangeCommandWarning.TO_COMMAND_DESCRIPTION).makeString("\n"));
        }
    }

    public boolean isDeploymentNeeded() {
        return !this.inserts.isEmpty() || !this.auditChanges.isEmpty();
    }
}
