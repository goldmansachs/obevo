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
package com.gs.obevo.api.platform;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.DeployExecution;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Interface to access the audit table for a given environment.
 */
public interface ChangeAuditDao {
    String CHANGE_AUDIT_TABLE_NAME = "ARTIFACTDEPLOYMENT";

    /**
     * Called at the start of the deployment step. This is here to create the audit table in the schema if it does not
     * already exist
     */
    void init();

    /**
     * Name of the physical artifact that contains the audit table. This is needed in case the deploy implementation
     * needs to know about this table somehow.
     */
    String getAuditContainerName();

    /**
     * Get the list of changes already deployed to this environment. Note that this call should be able to work even if
     * the audit table hasn't been initialized yet (in that, return an empty list) - this is so that this call can work
     * with the "PREVIEW" command against an uninitialized schema (i.e. deploying to prod for the first time).
     */
    ImmutableList<Change> getDeployedChanges();

    /**
     * Adds a change to the audit data to mark it as deployed. The change must not already be deployed (see
     * {@link Change#getChangeKey()} - this is suitable for incremental changes.
     */
    void insertNewChange(Change change, DeployExecution deployExecution);

    /**
     * Adds a change to the audit data to mark it as deployed - in case the change is already deployed (see
     * {@link Change#getChangeKey()}, this will update the status, e.g. new hash code. This is suitable for
     * rerunnable changes.
     */
    void updateOrInsertChange(Change change, DeployExecution deployExecution);

    /**
     * Removes the change from the audit data based on the {@link Change#getChangeKey()}.
     */
    void deleteChange(Change change);

    /**
     * Removes all changes related to the incoming changed object based on the {@link Change#getObjectKey()}.
     */
    void deleteObjectChanges(Change change);
}
