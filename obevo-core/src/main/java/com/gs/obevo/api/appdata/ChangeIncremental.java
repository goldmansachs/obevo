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

import com.gs.obevo.api.platform.ChangeType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.impl.factory.Lists;

public class ChangeIncremental extends Change {
    private String rollbackIfAlreadyDeployedContent;
    private transient boolean rollbackActivated = false;
    /**
     * The {@link #drop}, {@link #keepIncrementalOrder}, and {@link #manuallyCodedDrop} fields are related
     * to help determine the deployment order, specifically around whether drops should be done in the reverse normal
     * order or if they should be executed in the explicit order they were coded in.
     */
    private boolean drop;
    /**
     * See javadoc in {@link #drop}.
     */
    private transient boolean keepIncrementalOrder;
    /**
     * See javadoc in {@link #drop}.
     */
    private transient boolean manuallyCodedDrop;
    private MutableCollection<String> baselinedChanges;
    private String parallelGroup;
    /**
     * Whether the table drop should be forced. By default, it is false, allowing the IncrementalChangeTypeCommandCalculator
     * to not actually execute the full object SQL for regular deploys. But for cleaning the environment, we must force
     * the change.
     */
    private transient boolean forceDropForEnvCleaning;

    public MutableCollection<String> getBaselinedChanges() {
        return baselinedChanges == null ? Lists.mutable.<String>empty() : baselinedChanges;
    }

    public void setBaselinedChanges(MutableCollection<String> baselinedChanges) {
        this.baselinedChanges = baselinedChanges;
    }

    public Change withBaselines(MutableCollection<String> baselines) {
        this.setBaselinedChanges(baselines);
        return this;
    }

    public ChangeIncremental() {
        this.rollbackIfAlreadyDeployedContent = null;
        this.setActive(true);
    }

    public ChangeIncremental(ChangeType changeType, String schema, String objectName, String changeName,
            int orderWithinObject, String hash, String content) {
        this(new ChangeKey(schema, changeType, objectName, changeName), orderWithinObject, hash, content, null, true);
    }

    public ChangeIncremental(ChangeKey changeKey,
            int orderWithinObject, String hash, String content) {
        this(changeKey, orderWithinObject, hash, content, null, true);
    }

    public ChangeIncremental(ChangeType changeType, String schema, String objectName, String changeName,
            int orderWithinObject, String hash, String content, String rollbackIfAlreadyDeployedContent, boolean active) {
        this(new ChangeKey(schema, changeType, objectName, changeName), orderWithinObject, hash, content, rollbackIfAlreadyDeployedContent, active);
    }

    public ChangeIncremental(ChangeKey changeKey,
            int orderWithinObject, String hash, String content, String rollbackIfAlreadyDeployedContent, boolean active) {
        this.setChangeKey(changeKey);
        this.setContentHash(hash);
        this.setContent(content);
        this.setOrderWithinObject(orderWithinObject);
        this.rollbackIfAlreadyDeployedContent = rollbackIfAlreadyDeployedContent;
        this.setActive(active);
    }

    public String getRollbackIfAlreadyDeployedContent() {
        return this.rollbackIfAlreadyDeployedContent;
    }

    public void setRollbackIfAlreadyDeployedContent(String rollbackIfAlreadyDeployedContent) {
        this.rollbackIfAlreadyDeployedContent = rollbackIfAlreadyDeployedContent;
    }

    @Override
    public boolean isRollbackIfAlreadyDeployed() {
        return this.rollbackIfAlreadyDeployedContent != null;
    }

    @Override
    protected ToStringBuilder toStringBuilder() {
        return super.toStringBuilder()
                .append(this.isRollbackIfAlreadyDeployed());
    }

    public void setRollbackActivated(boolean rollbackActivated) {
        this.rollbackActivated = rollbackActivated;
    }

    @Override
    public boolean isRollbackActivated() {
        return this.rollbackIfAlreadyDeployedContent != null || this.rollbackActivated;
    }

    @Override
    public String getRollbackToBeExecutedContent() {
        if (this.rollbackIfAlreadyDeployedContent != null) {
            return this.rollbackIfAlreadyDeployedContent;
        } else if (this.rollbackActivated) {
            return this.getConvertedRollbackContent();
        } else {
            return null;
        }
    }

    public boolean isDrop() {
        return this.drop;
    }

    public void setDrop(boolean drop) {
        this.drop = drop;
    }

    public ChangeIncremental withDrop(boolean drop) {
        this.setDrop(drop);
        return this;
    }

    public boolean isKeepIncrementalOrder() {
        return keepIncrementalOrder;
    }

    public void setKeepIncrementalOrder(boolean keepIncrementalOrder) {
        this.keepIncrementalOrder = keepIncrementalOrder;
    }

    public ChangeIncremental withKeepIncrementalOrder(boolean keepIncrementalOrder) {
        this.setKeepIncrementalOrder(keepIncrementalOrder);
        return this;
    }

    public boolean isManuallyCodedDrop() {
        return manuallyCodedDrop;
    }

    public void setManuallyCodedDrop(boolean manuallyCodedDrop) {
        this.manuallyCodedDrop = manuallyCodedDrop;
    }

    public String getParallelGroup() {
        return parallelGroup;
    }

    public void setParallelGroup(String parallelGroup) {
        this.parallelGroup = parallelGroup;
    }

    public boolean isForceDropForEnvCleaning() {
        return forceDropForEnvCleaning;
    }

    public void setForceDropForEnvCleaning(boolean forceDropForEnvCleaning) {
        this.forceDropForEnvCleaning = forceDropForEnvCleaning;
    }
}
