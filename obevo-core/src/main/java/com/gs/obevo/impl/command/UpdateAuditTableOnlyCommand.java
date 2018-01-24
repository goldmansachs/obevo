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
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.api.platform.ChangeTypeBehaviorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateAuditTableOnlyCommand extends AuditOnlyChangeCommand {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateAuditTableOnlyCommand.class);
    private final String message;

    public UpdateAuditTableOnlyCommand(Change artifact, String message) {
        super(artifact);
        this.message = message;
    }

    @Override
    public void markAuditTable(ChangeTypeBehaviorRegistry changeTypeBehaviorRegistry, ChangeAuditDao artifactDeployerDao, DeployExecution deployExecution) {
        LOG.info("Marking audit table due to reason: {}", this.message);
        artifactDeployerDao.updateOrInsertChange(getArtifact(), deployExecution);
    }
}
