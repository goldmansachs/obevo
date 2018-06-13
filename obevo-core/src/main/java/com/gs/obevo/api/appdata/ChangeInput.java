package com.gs.obevo.api.appdata;

/*
 * Copyright 2017 Goldman Sachs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.impl.text.TextDependencyExtractable;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;

public class ChangeInput implements TextDependencyExtractable, Restrictable {
    private transient ImmutableSet<String> includeDependencies = Sets.immutable.with();
    private transient ImmutableSet<String> excludeDependencies = Sets.immutable.with();
    private transient ImmutableSet<CodeDependency> dependencies;
    /**
     * We have this setter kludge here for the static data dependency calculation (where we derive it based on the
     * information in the associated table file, but the two objects are currently separated).
     */
    private String contentForDependencyCalculation;

    private transient FileObject fileLocation;
    private ImmutableList<ArtifactRestrictions> restrictions;

    private @NotNull ChangeKey changeKey;
    private String content;
    private String convertedContent;
    private String rollbackContent;
    private String convertedRollbackContent;

    private final boolean rerunnable;

    private transient String dropContent;
    private String permissionScheme;  // this is really a property of the DB object, not the individual change. We have this here until we refactor to having a "DB Object" class

    private int order = Change.DEFAULT_CHANGE_ORDER;

    private String rollbackIfAlreadyDeployedContent;

    private boolean active;

    private int orderWithinObject;

    private MutableCollection<String> baselinedChanges;

    private String parallelGroup;
    /**
     * The {@link #drop}, {@link #keepIncrementalOrder}, and manuallyCodedDrop fields are related
     * to help determine the deployment order, specifically around whether drops should be done in the reverse normal
     * order or if they should be executed in the explicit order they were coded in.
     */
    private boolean drop;

    /**
     * See javadoc in {@link #drop}.
     */
    private transient boolean keepIncrementalOrder;
    private transient Boolean applyGrants = null;
    private String changeset;

    // TODO these don't belong in this class; try to move it out
    private String contentHash;

    public ChangeInput(boolean rerunnable) {
        this.rerunnable = rerunnable;
    }

    public boolean isRerunnable() {
        return rerunnable;
    }

    @Override
    public ImmutableList<ArtifactRestrictions> getRestrictions() {
        return this.restrictions;
    }

    public void setRestrictions(ImmutableList<ArtifactRestrictions> restrictions) {
        this.restrictions = restrictions;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @NotNull
    public ChangeKey getChangeKey() {
        return changeKey;
    }

    public void setChangeKey(@NotNull ChangeKey changeKey) {
        this.changeKey = changeKey;
    }

    public String getObjectName() {
        return changeKey.getObjectKey().getObjectName();
    }

    public String getContentForDependencyCalculation() {
        if (this.contentForDependencyCalculation == null) {
            return this.content;
        } else {
            return this.contentForDependencyCalculation;
        }
    }

    public void setContentForDependencyCalculation(String contentForDependencyCalculation) {
        this.contentForDependencyCalculation = contentForDependencyCalculation;
    }

    public String getRollbackContent() {
        // Setting default as blank space for backwards-compatibility w/ Sybase 11.9 change
        // (Sybase 11.9 requires TEXT data type, which cannot accept null values)
        // This setting would work fine across DBs
        return this.rollbackContent != null ? this.rollbackContent : "";
    }

    public void setRollbackContent(String rollbackContent) {
        // Setting default as blank space for backwards-compatibility w/ Sybase 11.9 change
        this.rollbackContent = rollbackContent != null ? rollbackContent : "";
    }

    public void setConvertedContent(String convertedContent) {
        this.convertedContent = convertedContent;
    }

    public void setConvertedRollbackContent(String convertedRollbackContent) {
        this.convertedRollbackContent = convertedRollbackContent;
    }

    public ObjectKey getObjectKey() {
        return this.changeKey.getObjectKey();
    }

    @Override
    public ImmutableSet<CodeDependency> getCodeDependencies() {
        return dependencies;
    }

    public void setCodeDependencies(ImmutableSet<CodeDependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Only for log output purposes
     */
    public FileObject getFileLocation() {
        return this.fileLocation;
    }

    public void setFileLocation(FileObject fileLocation) {
        this.fileLocation = fileLocation;
    }

    @Override
    public ImmutableSet<String> getExcludeDependencies() {
        return this.excludeDependencies;
    }

    public void setExcludeDependencies(ImmutableSet<String> excludeDependencies) {
        this.excludeDependencies = excludeDependencies;
    }

    @Override
    public ImmutableSet<String> getIncludeDependencies() {
        return includeDependencies;
    }

    public void setIncludeDependencies(ImmutableSet<String> includeDependencies) {
        this.includeDependencies = includeDependencies;
    }

    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
/*
        if (isRollbackActivated()) {
            sb.append("ROLLING BACK: ");
        }
        sb.append(String.format(
                "Object [%s]; ChangeName [%s]; Type [%s]; LogicalSchema [%s]"
                , this.getObjectName()
                , this.getChangeName()
                , this.getChangeType().getName()
                , this.getSchema()
        ));
*/
        if (this.fileLocation != null) {
            sb.append("; File [" + this.fileLocation + "]");
        }

        return sb.toString();
    }

    /**
     * This getDbObjectKey() string concatenation is a kludge until we refactor the DB object stuff itself out to its
     * own object
     */
    public String getDbObjectKey() {
        return this.getObjectKey().getSchema() + ":" + this.getObjectName();
    }

    public String getChangeTypeName() {
        return this.getObjectKey().getChangeTypeName();
    }

    public String getPermissionScheme() {
        return this.permissionScheme == null ? this.getChangeTypeName() : this.permissionScheme;
    }

    public void setPermissionScheme(String permissionScheme) {
        this.permissionScheme = permissionScheme;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    private transient TextMarkupDocumentSection metadataSection;

    public TextMarkupDocumentSection getMetadataSection() {
        return metadataSection;
    }

    public void setMetadataSection(TextMarkupDocumentSection metadataSection) {
        this.metadataSection = metadataSection;
    }

    public String getDropContent() {
        return dropContent;
    }

    public void setDropContent(String dropContent) {
        this.dropContent = dropContent;
    }

    public String getRollbackIfAlreadyDeployedContent() {
        return this.rollbackIfAlreadyDeployedContent;
    }

    public void setRollbackIfAlreadyDeployedContent(String rollbackIfAlreadyDeployedContent) {
        this.rollbackIfAlreadyDeployedContent = rollbackIfAlreadyDeployedContent;
    }

    public boolean getActive() {
        return this.active;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getOrderWithinObject() {
        return this.orderWithinObject;
    }

    public void setOrderWithinObject(int orderWithinObject) {
        this.orderWithinObject = orderWithinObject;
    }

    public String getConvertedContent() {
        return convertedContent;
    }

    public String getConvertedRollbackContent() {
        return convertedRollbackContent;
    }

    public MutableCollection<String> getBaselinedChanges() {
        return baselinedChanges;
    }

    public void setBaselinedChanges(MutableCollection<String> baselinedChanges) {
        this.baselinedChanges = baselinedChanges;
    }

    public String getParallelGroup() {
        return parallelGroup;
    }

    public void setParallelGroup(String parallelGroup) {
        this.parallelGroup = parallelGroup;
    }

    public boolean isDrop() {
        return drop;
    }

    public void setDrop(boolean drop) {
        this.drop = drop;
    }

    public boolean isKeepIncrementalOrder() {
        return keepIncrementalOrder;
    }

    public void setKeepIncrementalOrder(boolean keepIncrementalOrder) {
        this.keepIncrementalOrder = keepIncrementalOrder;
    }

    public Boolean getApplyGrants() {
        return applyGrants;
    }

    public void setApplyGrants(Boolean applyGrants) {
        this.applyGrants = applyGrants;
    }

    public String getChangeset() {
        return changeset;
    }

    public void setChangeset(String changeset) {
        this.changeset = changeset;
    }

    public String getContentHash() {
        return this.contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("changeKey", changeKey)
                .append("includeDependencies", includeDependencies)
                .append("excludeDependencies", excludeDependencies)
                .append("fileLocation", fileLocation)
                .append("dependencies", dependencies)
                .append("contentForDependencyCalculation", contentForDependencyCalculation)
                .append("content", content)
                .append("convertedContent", convertedContent)
                .append("rollbackContent", rollbackContent)
                .append("convertedRollbackContent", convertedRollbackContent)
                .append("rerunnable", rerunnable)
                .append("restrictions", restrictions)
                .append("permissionScheme", permissionScheme)
                .append("order", order)
                .append("metadataSection", metadataSection)
                .append("dropContent", dropContent)
                .append("rollbackIfAlreadyDeployedContent", rollbackIfAlreadyDeployedContent)
                .append("active", active)
                .append("orderWithinObject", orderWithinObject)
                .append("baselinedChanges", baselinedChanges)
                .append("parallelGroup", parallelGroup)
                .append("drop", drop)
                .append("keepIncrementalOrder", keepIncrementalOrder)
                .append("applyGrants", applyGrants)
                .append("changeset", changeset)
                .append("contentHash", contentHash)
                .toString();
    }
}
