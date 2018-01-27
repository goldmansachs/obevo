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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.Platform;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;

public interface DeployerPlugin<P extends Platform, E extends Environment<P>> {
    void validateSetup();

    void initializeSchema(final Environment env, PhysicalSchema schema);

    void printArtifactsToProcessForUser2(Changeset artifactsToProcess, DeployStrategy deployStrategy, E env, ImmutableCollection<Change> deployedChanges, ImmutableCollection<Change> sourceChanges);

    void validatePriorToDeployment(E env, DeployStrategy deployStrategy, ImmutableList<Change> sourceChanges, ImmutableCollection<Change> deployedChanges, Changeset artifactsToProcess);

    void doPostDeployAction(E env, ImmutableList<Change> sourceChanges);

    void logEnvironmentMetrics(E env);
}
