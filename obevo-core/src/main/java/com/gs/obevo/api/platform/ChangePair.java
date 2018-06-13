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
import com.gs.obevo.api.appdata.ObjectKey;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ChangePair {
    private Change sourceChange;
    private Change deployedChange;

    public ChangePair() {
    }

    /**
     * Used during tests.
     */
    public ChangePair(Change sourceChange, Change deployedChange) {
        this.deployedChange = deployedChange;
        this.sourceChange = sourceChange;
    }

    public ObjectKey getObjectKey() {
        return new ObjectKey(getSchema(), getObjectName(), getChangeType());
    }

    private String getSchema() {
        return this.getArtifact().getSchema();
    }

    private ChangeType getChangeType() {
        return this.getArtifact().getChangeType();
    }

    private String getObjectName() {
        return this.getArtifact().getObjectName();
    }

    private Change getArtifact() {
        return deployedChange == null ? sourceChange : deployedChange;
    }

    public Change getSourceChange() {
        return sourceChange;
    }

    public void setSourceChange(Change sourceChange) {
        if (this.sourceChange != null) {
            throw new IllegalArgumentException(
                    String.format("sourceChange field could not be set again - something wrong w/ your keys:\n" +
                                    "Source Artifact 1 [%s] at location [%s]\n" +
                                    "Source Artifact 2 [%s] at location [%s]\n",
                            sourceChange.getDisplayString(), sourceChange.getSourceLocation(),
                            this.sourceChange.getDisplayString(), this.sourceChange.getSourceLocation()));
        }
        this.sourceChange = sourceChange;
    }

    public Change getDeployedChange() {
        return deployedChange;
    }

    public void setDeployedChange(Change deployedChange) {
        if (this.deployedChange != null) {
            throw new IllegalArgumentException(
                    "deployed field could not be set again - something wrong w/ your keys:\n   "
                            + deployedChange.getDisplayString() + "\nvs: " + this.deployedChange.getDisplayString());
        }
        this.deployedChange = deployedChange;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("sourceChange", sourceChange)
                .append("deployedChange", deployedChange)
                .toString();
    }
}
