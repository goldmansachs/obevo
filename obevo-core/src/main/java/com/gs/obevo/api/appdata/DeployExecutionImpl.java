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
package com.gs.obevo.api.appdata;

import java.sql.Timestamp;

import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;

/**
 * Represents the invocation of a db deployment job execution. Tracks who requested it and the attributes of the request.
 */
public class DeployExecutionImpl implements DeployExecution {
    private long id;
    private DeployExecutionStatus status;
    private final String schema;
    private final Timestamp deployTime;
    private final String executorId;
    private final String toolVersion;
    private final boolean init;
    private final boolean rollback;
    private final String requesterId;
    private final String productVersion;
    private final String reason;
    private final ImmutableSet<? extends DeployExecutionAttribute> attributes;

    public DeployExecutionImpl(String requesterId, String deployExecutorId, String schema, String toolVersion, Timestamp deployTime, boolean init, boolean rollback, String productVersion, String reason, ImmutableSet<? extends DeployExecutionAttribute> attributes) {
        this.requesterId = requesterId;
        this.executorId = deployExecutorId;
        this.schema = schema;
        this.toolVersion = toolVersion;
        this.deployTime = deployTime;
        this.init = init;
        this.rollback = rollback;
        this.productVersion = productVersion;
        this.reason = reason;
        this.attributes = attributes != null ? attributes : Sets.immutable.<DeployExecutionAttribute>empty();
    }

    @Override
    public long getId() {
        if (id == 0) {
            throw new IllegalStateException("cannot get id without setting it first");
        }
        return id;
    }

    public void setId(long id) {
        if (this.id != 0) {
            throw new IllegalStateException("id has already been set [value=" + this.id + "], cannot set it again");
        }
        this.id = id;
    }

    @Override
    public DeployExecutionStatus getStatus() {
        return Validate.notNull(status, "cannot get status field without setting it first");
    }

    @Override
    public void setStatus(DeployExecutionStatus status) {
        this.status = status;
    }

    @Override
    public Timestamp getDeployTime() {
        return deployTime;
    }

    @Override
    public String getExecutorId() {
        return executorId;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public String getToolVersion() {
        return toolVersion;
    }

    @Override
    public boolean isInit() {
        return init;
    }

    @Override
    public boolean isRollback() {
        return rollback;
    }

    @Override
    public String getRequesterId() {
        return requesterId;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public String getProductVersion() {
        return productVersion;
    }

    @Override
    public ImmutableSet<DeployExecutionAttribute> getAttributes() {
        return (ImmutableSet<DeployExecutionAttribute>) attributes;
    }
}
