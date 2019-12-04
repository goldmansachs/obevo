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
package com.gs.obevo.db.impl.core.changeauditdao;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.platform.AuditLock;
import com.gs.obevo.api.platform.ChangeAuditDao;
import com.gs.obevo.impl.changeauditdao.InMemLock;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * No-op used for in-memory db implementations where we don't need to keep audit.
 */
public class NoOpChangeAuditDao implements ChangeAuditDao {
    @Override
    public String getAuditContainerName() {
        return "no-op";
    }

    @Override
    public void init() {
    }

    @Override
    public void insertNewChange(Change change, DeployExecution deployExecution) {
    }

    @Override
    public void updateOrInsertChange(Change change, DeployExecution deployExecution) {
    }

    @Override
    public ImmutableList<Change> getDeployedChanges() {
        return Lists.immutable.with();
    }

    @Override
    public void deleteChange(Change change) {
    }

    @Override
    public void deleteObjectChanges(Change change) {
    }

    @Override
    public AuditLock acquireLock() {
        return new InMemLock();
    }
}
