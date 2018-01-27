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

import com.gs.obevo.api.appdata.DeployExecution;
import com.gs.obevo.api.appdata.PhysicalSchema;
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * DAO for creating new instances of deploy executions, which mark the beginning/end of a deployment.
 */
public interface DeployExecutionDao {
    // keeping the ARTIFACT name in the prefix to be consistent w/ the pre-existing ARTIFACTDEPLOYMENT table
    String DEPLOY_EXECUTION_TABLE_NAME = "ARTIFACTEXECUTION";
    String DEPLOY_EXECUTION_ATTRIBUTE_TABLE_NAME = "ARTIFACTEXECUTIONATTR";

    void init();

    void persistNew(DeployExecution deployExecution, PhysicalSchema physicalSchema);

    void update(DeployExecution deployExecution);

    ImmutableCollection<DeployExecution> getDeployExecutions(String schema);

    DeployExecution getLatestDeployExecution(String schema);

    String getExecutionContainerName();

    String getExecutionAttributeContainerName();
}
