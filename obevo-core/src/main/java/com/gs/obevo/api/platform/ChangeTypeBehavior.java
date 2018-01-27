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

/**
 * Defines the behavior for deploying a change. This class will be Platform-specific.
 */
public interface ChangeTypeBehavior {
    /**
     * Deploys the given change.
     */
    void deploy(Change change, CommandExecutionContext cec);

    /**
     * Undeploys the given change (i.e. removes from db or rolls back).
     */
    void undeploy(Change change);

    /**
     * Drops the object as a whole.
     *
     * @param change The change to drop
     * @param dropForRecreate whether the drop is being called to recreate the object (thus will hide exceptions at the
     * moment), or a permanent drop
     */
    void dropObject(Change change, boolean dropForRecreate);

    String getDefinitionFromEnvironment(Change exampleChange);
}
