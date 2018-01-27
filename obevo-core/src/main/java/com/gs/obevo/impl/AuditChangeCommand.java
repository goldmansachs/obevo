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
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeCommand;

/**
 * A command that will only update the audit table in the {@link Environment}, but not actually modify anything else
 * pertinent to your application code.
 */
public interface AuditChangeCommand extends ChangeCommand {
    /**
     * Returns the schema that the change belongs to.
     */
    String getSchema();

    /**
     * Modifies the audit table (i.e. either manage or unmanage the changes).
     * @param changeTypeBehaviorRegistry
     * @param artifactDeployerDao
     * @param deployExecution
     */
    void markAuditTable(ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry, ChangeAuditDao artifactDeployerDao, DeployExecution deployExecution);
}
