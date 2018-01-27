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
package com.gs.obevo.impl.changecalc;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeIncremental;
import com.gs.obevo.impl.AuditChangeCommand;
import com.gs.obevo.impl.ChangeCommandWarning;
import com.gs.obevo.impl.ExecuteChangeCommand;
import com.gs.obevo.impl.command.CurrentDeactivationWarning;
import com.gs.obevo.impl.command.DeployChangeCommand;
import com.gs.obevo.impl.command.DropObjectChangeCommand;
import com.gs.obevo.impl.command.ImproperlyRemovedWarning;
import com.gs.obevo.impl.command.UndeployChangeCommand;
import com.gs.obevo.impl.command.UnrolledbackChangeWarning;
import com.gs.obevo.impl.command.UpdateAuditTableOnlyCommand;

public final class ChangeCommandFactory {
    public ChangeCommandFactory() {
    }

    public ExecuteChangeCommand createRemove(Change droppedObject) {
        return new DropObjectChangeCommand(droppedObject);
    }

    public ExecuteChangeCommand createDeployCommand(Change source) {
        return new DeployChangeCommand(source);
    }

    public ExecuteChangeCommand createDeployCommand(Change source, String deployMessage) {
        return new DeployChangeCommand(source, deployMessage);
    }

    public ExecuteChangeCommand createRollback(ChangeIncremental incrementalDeployed, String undeployMessage) {
        return new UndeployChangeCommand(incrementalDeployed, undeployMessage);
    }

    public ChangeCommandWarning createUnrolledbackWarning(ChangeIncremental incrementalDeployed) {
        return new UnrolledbackChangeWarning(incrementalDeployed);
    }

    public ChangeCommandWarning createImproperlyRemovedWarning(ChangeIncremental incrementalDeployed) {
        return new ImproperlyRemovedWarning(incrementalDeployed);
    }

    public ChangeCommandWarning createCurrentDeactivationWarning(ChangeIncremental incrementalDeployed) {
        return new CurrentDeactivationWarning(incrementalDeployed);
    }

    public AuditChangeCommand createUpdateAuditTableOnly(Change incrementalDeployed, String message) {
        return new UpdateAuditTableOnlyCommand(incrementalDeployed, message);
    }
}
