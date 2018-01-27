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
import com.gs.obevo.impl.text.TextDependencyExtractable;
import com.gs.obevo.util.hash.DbChangeHashStrategy;
import com.gs.obevo.util.hash.ExactDbChangeHashStrategy;
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

public abstract class Change implements Restrictable, SortableDependency, SortableDependencyGroup, TextDependencyExtractable {
    public static final int DEFAULT_CHANGE_ORDER = 500;  // only used to control relative order changes (e.g. within a given class of changes like stored procs)

    public static final Function<Change, String> TO_SCHEMA = new Function<Change, String>() {
        @Override
        public String valueOf(Change arg0) {
            return arg0.getSchema();
        }
    };

    public static final Function<Change, String> TO_DB_OBJECT_KEY = new Function<Change, String>() {
        @Override
        public String valueOf(Change arg0) {
            return arg0.getDbObjectKey();
        }
    };
    public static final Function<Change, String> TO_DISPLAY_STRING = new Function<Change, String>() {
        @Override
        public String valueOf(Change object) {
            return object.getDisplayString();
        }
    };

    public static final Function<Change, String> TO_CONTENT = new Function<Change, String>() {
        @Override
        public String valueOf(Change object) {
            return object.getContent();
        }
    };

    public static final Function<Change, ChangeType> TO_CHANGE_TYPE = new Function<Change, ChangeType>() {
        @Override
        public ChangeType valueOf(Change object) {
            return object.getChangeType();
        }
    };

    public static final Function<Change, String> TO_CHANGE_TYPE_NAME = new Function<Change, String>() {
        @Override
        public String valueOf(Change arg0) {
            return arg0.getChangeType().getName();
        }
    };

    public static final Function<Change, ObjectKey> TO_OBJECT_KEY = new Function<Change, ObjectKey>() {
        @Override
        public ObjectKey valueOf(Change arg0) {
            return arg0.getObjectKey();
        }
    };

    public static final Function<Change, ChangeKey> TO_CHANGE_KEY = new Function<Change, ChangeKey>() {
        @Override
        public ChangeKey valueOf(Change object) {
            return object.getChangeKey();
        }
    };

    public static final Function<Change, Timestamp> TO_TIME_INSERTED = new Function<Change, Timestamp>() {
        @Override
        public Timestamp valueOf(Change object) {
            return object.getTimeInserted();
        }
    };

    public static final Function<Change, String> TO_CHANGESET = new Function<Change, String>() {
        @Override
        public String valueOf(Change arg0) {
            return arg0.getChangeset();
        }
    };

    public static final Predicate<Change> IS_CREATE_OR_REPLACE = new Predicate<Change>() {
        @Override
        public boolean accept(Change each) {
            return each.isCreateOrReplace();
        }
    };

    private static final Pattern CREATE_OR_REPLACE_PATTERN = Pattern.compile("(?i)create\\s+or\\s+replace");

    private transient ObjectKey objectKey;
    private transient ChangeKey changeKey;
    private String changeName;
    private String objectName;

    private String contentHash;

    private transient FileObject fileLocation;

    private boolean active;

    private String schema;

    private ChangeType changeType;

    private ImmutableList<ArtifactRestrictions> restrictions;

    private String content;
    private String convertedContent;
    private String rollbackContent;
    private String convertedRollbackContent;
    private int order = DEFAULT_CHANGE_ORDER;

    private String permissionScheme;  // this is really a property of the DB object, not the individual change. We have this here until we refactor to having a "DB Object" class
    private transient TextMarkupDocumentSection metadataSection;
    private transient String dropContent;
    private transient ImmutableSet<CodeDependency> dependencies;
    private transient ImmutableSet<String> includeDependencies = Sets.immutable.with();
    private transient ImmutableSet<String> excludeDependencies = Sets.immutable.with();
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

    private final ImmutableList<DbChangeHashStrategy> contentHashStrategies = Lists.immutable.with(
            new OldWhitespaceAgnosticDbChangeHashStrategy(),
            new ExactDbChangeHashStrategy()
    );

    protected Change() {
    }

    @Override
    public ObjectKey getObjectKey() {
        if (this.objectKey == null) {
            this.objectKey = new ObjectKey(schema, changeType, objectName);
        }
        return this.objectKey;
    }

    public ChangeKey getChangeKey() {
        if (this.changeKey == null) {
            this.changeKey = new ChangeKey(getObjectKey(), changeName);
        }
        return this.changeKey;
    }

    public String getSchema() {
        return this.schema;
    }

    public void setSchema(String dbSchema) {
        this.schema = dbSchema;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getChangeName() {
        return this.changeName;
    }

    public void setChangeName(String changeName) {
        this.changeName = changeName;
    }

    public String getContentHash() {
        return this.contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public PhysicalSchema getPhysicalSchema(Environment env) {
        return env.getPhysicalSchema(this.schema);
    }

    /**
     * This getDbObjectKey() string concatenation is a kludge until we refactor the DB object stuff itself out to its
     * own object
     */
    public String getDbObjectKey() {
        return this.getSchema() + ":" + this.getObjectName();
    }

    public ChangeType getChangeType() {
        return this.changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    @Override
    public ImmutableList<ArtifactRestrictions> getRestrictions() {
        return this.restrictions;
    }

    public void setRestrictions(ImmutableList<ArtifactRestrictions> restrictions) {
        this.restrictions = restrictions;
    }

    Change withRestrictions(ImmutableList<ArtifactRestrictions> restrictions) {
        this.setRestrictions(restrictions);
        return this;
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
        if (this.fileLocation != null) {
            sb.append("; File [" + this.fileLocation + "]");
        }

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

    public static Function<Change, String> changeName() {
        return new Function<Change, String>() {
            @Override
            public String valueOf(Change arg0) {
                return arg0.getChangeName();
            }
        };
    }

    public static Function<Change, Boolean> active() {
        return new Function<Change, Boolean>() {
            @Override
            public Boolean valueOf(Change arg0) {
                return arg0.isActive();
            }
        };
    }

    public static Function<Change, String> objectName() {
        return new Function<Change, String>() {
            @Override
            public String valueOf(Change arg0) {
                return arg0.getObjectName();
            }
        };
    }

    public static Function<Change, String> contentHash() {
        return new Function<Change, String>() {
            @Override
            public String valueOf(Change arg0) {
                return arg0.getContentHash();
            }
        };
    }

    public static Function<Change, String> schema() {
        return new Function<Change, String>() {
            @Override
            public String valueOf(Change arg0) {
                return arg0.getSchema();
            }
        };
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
                MutableSet<String> acceptableHashes = UnifiedSet.newSet();
                acceptableHashes.add(hashStrategy.hashContent(content));
                if (convertedContent != null) {
                    acceptableHashes.add(hashStrategy.hashContent(convertedContent));
                }
                return acceptableHashes;
            }
        }).toSet().toImmutable();
    }

    public String getPermissionScheme() {
        return this.permissionScheme == null ? this.changeType.getName() : this.permissionScheme;
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

    @Override
    public void setCodeDependencies(ImmutableSet<CodeDependency> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public ImmutableSet<String> getDependencies() {
        return this.dependencies != null ? this.dependencies.collect(CodeDependency.TO_TARGET) : null;
    }

    @Override
    public void setDependencies(ImmutableSet<String> dependencies) {
        this.dependencies = dependencies == null ? null : dependencies.collectWith(CodeDependency.CREATE_WITH_TYPE, CodeDependencyType.EXPLICIT);
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

    /**
     * Only for log output purposes
     */
    public FileObject getFileLocation() {
        return this.fileLocation;
    }

    public void setFileLocation(FileObject fileLocation) {
        this.fileLocation = fileLocation;
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

    private Timestamp getTimeInserted() {
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
}
