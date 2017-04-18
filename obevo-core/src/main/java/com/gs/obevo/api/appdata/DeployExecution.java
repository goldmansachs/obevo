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
package com.gs.obevo.api.appdata;

import java.sql.Timestamp;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;

public interface DeployExecution {
    Function<DeployExecution, Long> TO_ID = new Function<DeployExecution, Long>() {
        @Override
        public Long valueOf(DeployExecution object) {
            return object.getId();
        }
    };
    Function<DeployExecution, String> TO_PRODUCT_VERSION = new Function<DeployExecution, String>() {
        @Override
        public String valueOf(DeployExecution object) {
            return object.getProductVersion();
        }
    };

    long getId();

    String getSchema();

    DeployExecutionStatus getStatus();

    void setStatus(DeployExecutionStatus status);

    Timestamp getDeployTime();

    String getExecutorId();

    String getToolVersion();

    boolean isInit();

    boolean isRollback();

    String getRequesterId();

    String getReason();

    String getProductVersion();

    ImmutableSet<DeployExecutionAttribute> getAttributes();
}
