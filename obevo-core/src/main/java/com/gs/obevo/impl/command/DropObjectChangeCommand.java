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
import com.gs.obevo.api.platform.CommandExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DropObjectChangeCommand extends AbstractExecuteChangeCommand {
    private static final Logger LOG = LoggerFactory.getLogger(DropObjectChangeCommand.class);

    public DropObjectChangeCommand(Change artifact) {
        super(artifact);
    }

    @Override
    public void execute(CommandExecutionContext cec) {
        getArtifact().dropObject();
    }

    @Override
    public String getCommandDescription() {
        return "DROPPING OBJECT: " + super.getCommandDescription();
    }

    @Override
    public void markAuditTable(ChangeAuditDao changeAuditDao, DeployExecution deployExecution) {
        getArtifact().unmanageObject(changeAuditDao);
    }
}
