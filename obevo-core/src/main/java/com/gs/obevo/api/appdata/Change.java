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
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.impl.graph.SortableDependency;
import com.gs.obevo.impl.graph.SortableDependencyGroup;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
import com.gs.obevo.util.hash.ExactDbChangeHashStrategy;
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;

public abstract class Change implements SortableDependency, SortableDependencyGroup {
    public static final int DEFAULT_CHANGE_ORDER = 500;  // only used to control relative order changes (e.g. within a given class of changes like stored procs)

    private static final Pattern CREATE_OR_REPLACE_PATTERN = Pattern.compile("(?i)create\\s+or\\s+replace");

    private @NotNull ChangeKey changeKey;

    private String contentHash;

    private boolean active;

    private String content;
    private String convertedContent;
    private String rollbackContent;
    private String convertedRollbackContent;
    private int order = DEFAULT_CHANGE_ORDER;

    private String permissionScheme;  // this is really a property of the DB object, not the individual change. We have this here until we refactor to having a "DB Object" class
    private transient TextMarkupDocumentSection metadataSection;
    private transient String dropContent;
    private transient ImmutableSet<CodeDependency> dependencies;
    private transient Boolean applyGrants = null;
    private int orderWithinObject = 0;
    private Timestamp timeUpdated;
    private Timestamp timeInserted;
    private String changeset;
    /**
     * We have this setter kludge here for the static data dependency calculation (where we derive it based on the
     * information in the associated table file, but the two objects are currently separated).
     */
    private String contentForDependencyCalculation;

    private ChangeInput changeInput;
    private SetIterable<Change> dependentChanges;

    /**
     * @deprecated replace with {@link #getChangeInput()}
     */
    @Deprecated
    public ChangeInput getSourceLocation() {
        return changeInput;
    }

    public ChangeInput getChangeInput() {
        return changeInput;
    }

    public void setChangeInput(ChangeInput changeInput) {
        this.changeInput = changeInput;
    }

    private final ImmutableList<DbChangeHashStrategy> contentHashStrategies = Lists.immutable.with(
            new OldWhitespaceAgnosticDbChangeHashStrategy(),
            new ExactDbChangeHashStrategy()
    );

    protected Change() {
    }

    public ObjectKey getObjectKey() {
        return this.changeKey.getObjectKey();
    }

    @Override
    public ChangeKey getChangeKey() {
        return this.changeKey;
    }

    public void setChangeKey(ChangeKey changeKey) {
        this.changeKey = changeKey;
    }

    public String getSchema() {
        return this.changeKey.getObjectKey().getSchema();
    }

    public String getObjectName() {
        return this.changeKey.getObjectKey().getObjectName();
    }

    public String getChangeName() {
        return this.changeKey.getChangeName();
    }

    public String getContentHash() {
        return this.contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public PhysicalSchema getPhysicalSchema(Environment env) {
        return env.getPhysicalSchema(this.getSchema());
    }

    /**
     * This getDbObjectKey() string concatenation is a kludge until we refactor the DB object stuff itself out to its
     * own object
     */
    public String getDbObjectKey() {
        return this.getSchema() + ":" + this.getObjectName();
    }

    public ChangeType getChangeType() {
        return this.changeKey.getObjectKey().getChangeType();
    }

    public String getChangeTypeName() {
        return this.getChangeType().getName();
    }

    public String getContent() {
        return this.content;
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

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * TODO rename this to something more appropriate (i.e. not hiding the convertedContent field)
     */
    public String getConvertedContent() {
        return this.isRollbackActivated() ? this.getRollbackToBeExecutedContent() : this.convertedContent != null
                ? this.convertedContent : this.content;
    }

    public void setConvertedContent(String convertedContent) {
        this.convertedContent = convertedContent;
    }

    public void setConvertedRollbackContent(String convertedRollbackContent) {
        this.convertedRollbackContent = convertedRollbackContent;
    }

    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
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

        return sb.toString();
    }

    public boolean isRollbackIfAlreadyDeployed() {
        return false;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean equalsOnContent(Change other) {
        return new EqualsBuilder()
                .append(this.getSchema(), other.getSchema())
                .append(this.getObjectName(), other.getObjectName())
                .append(this.getChangeType(), other.getChangeType())
                .append(this.getContentHash(), other.getContentHash())
                .isEquals();
    }

    @Override
    public String toString() {
        return this.toStringBuilder().toString();
    }

    ToStringBuilder toStringBuilder() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append(this.getSchema())
                .append(this.getChangeName())
                .append(this.getObjectName())
                .append(this.getChangeType())
                .append(this.getContent())
                .append(this.getContentHash())
                .append(this.getOrderWithinObject())
                ;
    }

    public ImmutableSet<String> getAcceptableHashes() {
        /**
         * This is here for backwards-compatibility w/ systems that were doing the hashing prior to making all the
         * hashing agnostic of the white-space (before, we only had the table changes be white-space agnostic).
         * We need the various contentHashStrategies to account for past versions of the algorithm.
         */
        return this.contentHashStrategies.flatCollect(new Function<DbChangeHashStrategy, Iterable<String>>() {
            @Override
            public Iterable<String> valueOf(DbChangeHashStrategy hashStrategy) {
                MutableSet<String> acceptableHashes = Sets.mutable.empty();
                acceptableHashes.add(hashStrategy.hashContent(content));
                if (convertedContent != null) {
                    acceptableHashes.add(hashStrategy.hashContent(convertedContent));
                }
                return acceptableHashes;
            }
        }).toSet().toImmutable();
    }

    public String getPermissionScheme() {
        return this.permissionScheme == null ? this.getChangeType().getName() : this.permissionScheme;
    }

    public void setPermissionScheme(String permissionScheme) {
        this.permissionScheme = permissionScheme;
    }

    public TextMarkupDocumentSection getMetadataSection() {
        return this.metadataSection;
    }

    public String getMetadataAttribute(String attrName) {
        return this.metadataSection == null ? null : this.metadataSection.getAttr(attrName);
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

    @Override
    public ImmutableSet<CodeDependency> getCodeDependencies() {
        return dependencies;
    }

    public void setCodeDependencies(ImmutableSet<CodeDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    boolean isRollbackActivated() {
        return false;
    }

    String getRollbackToBeExecutedContent() {
        return null;
    }

    String getConvertedRollbackContent() {
        return this.convertedRollbackContent == null ? this.rollbackContent : this.convertedRollbackContent;
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

    public int getOrderWithinObject() {
        return this.orderWithinObject;
    }

    public void setOrderWithinObject(int orderWithinObject) {
        this.orderWithinObject = orderWithinObject;
    }

    public Timestamp getTimeInserted() {
        return timeInserted != null ? timeInserted : timeUpdated;
    }

    public void setTimeInserted(Timestamp timeInserted) {
        this.timeInserted = timeInserted;
    }

    public Timestamp getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(Timestamp timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    /**
     * Determines if we should force-apply (or force-not-apply) the grants. If not specified, it will default to
     * whatever the default check in the code. This is here as a failsafe option.
     */
    public Boolean getApplyGrants() {
        return applyGrants;
    }

    public void setApplyGrants(Boolean applyGrants) {
        this.applyGrants = applyGrants;
    }

    /**
     * The changeset name for this change; this change will only be deployed if this particular changeset is requested
     * in the input arguments (or if the "all-changesets" override is applied).
     */
    public String getChangeset() {
        return changeset;
    }

    /**
     * @see #getChangeset()
     */
    public void setChangeset(String changeset) {
        this.changeset = changeset;
    }

    @Override
    public ImmutableSet<SortableDependency> getComponents() {
        return Sets.immutable.<SortableDependency>with(this);
    }

    public boolean isCreateOrReplace() {
        if (content == null) {
            return false;
        }
        return CREATE_OR_REPLACE_PATTERN.matcher(content).find();
    }

    public void setDependentChanges(SetIterable<Change> dependentChanges) {
        this.dependentChanges = dependentChanges;
    }

    public SetIterable<Change> getDependentChanges() {
        return dependentChanges;
    }
}
