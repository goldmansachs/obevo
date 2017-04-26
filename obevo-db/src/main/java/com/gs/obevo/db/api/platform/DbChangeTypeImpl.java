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
package com.gs.obevo.db.api.platform;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeImpl;

/**
 * {@link ChangeType} implementation that specifically applies for SQL-based DB change types.
 */
public class DbChangeTypeImpl extends ChangeTypeImpl implements DbChangeType {
    private final String grantObjectQualifier;
    private final String defaultObjectKeyword;

    protected DbChangeTypeImpl(String name, boolean rerunnable, int deployOrderPriority, String directoryNameOld, boolean enrichableForDependenciesInText, boolean dependentObjectRecalculationRequired, String grantObjectQualifier, String defaultObjectKeyword, String directoryName) {
        super(name, rerunnable, deployOrderPriority, directoryNameOld, enrichableForDependenciesInText, dependentObjectRecalculationRequired, directoryName);
        this.grantObjectQualifier = grantObjectQualifier;
        this.defaultObjectKeyword = defaultObjectKeyword;
    }

    /**
     * In the grant pattern "GRANT [permission] ON [objectType] [objectName] TO [grantTargetType] [grantTargetName]",
     * returns the [objectType] to use. May differ depending on the dialect.
     */
    @Override
    public String getGrantObjectQualifier() {
        return grantObjectQualifier;
    }

    @Override
    public String getDefaultObjectKeyword() {
        return defaultObjectKeyword;
    }

    public static DbChangeTypeBuilder newDbChangeType(String name, boolean rerunnable, int deployOrderPriority, String defaultObjectKeyword) {
        return new DbChangeTypeBuilder(name, rerunnable, deployOrderPriority, defaultObjectKeyword);
    }

    public static DbChangeTypeBuilder newDbChangeType(DbChangeType dbChangeType) {
        return new DbChangeTypeBuilder(dbChangeType);
    }

    public static class DbChangeTypeBuilder {
        private String name;
        private boolean rerunnable;
        private int deployOrderPriority;
        private String defaultObjectKeyword;
        private String directoryName = null;
        private String directoryNameOld = null;
        private boolean enrichableForDependenciesInText = true;
        private boolean dependentObjectRecalculationRequired = true;
        private String grantObjectQualifier = "";

        protected DbChangeTypeBuilder(String name, boolean rerunnable, int deployOrderPriority, String defaultObjectKeyword) {
            this.name = name;
            this.rerunnable = rerunnable;
            this.deployOrderPriority = deployOrderPriority;
            this.defaultObjectKeyword = defaultObjectKeyword;
        }

        protected DbChangeTypeBuilder(DbChangeType dbChangeType) {
            this.name = dbChangeType.getName();
            this.rerunnable = dbChangeType.isRerunnable();
            this.deployOrderPriority = dbChangeType.getDeployOrderPriority();
            this.defaultObjectKeyword = dbChangeType.getDefaultObjectKeyword();
            this.directoryNameOld = dbChangeType.getDirectoryNameOld();
            this.enrichableForDependenciesInText = dbChangeType.isEnrichableForDependenciesInText();
            this.dependentObjectRecalculationRequired = dbChangeType.isDependentObjectRecalculationRequired();
            this.grantObjectQualifier = dbChangeType.getGrantObjectQualifier();
        }

        public DbChangeTypeBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public DbChangeTypeBuilder setRerunnable(boolean rerunnable) {
            this.rerunnable = rerunnable;
            return this;
        }

        public DbChangeTypeBuilder setDeployOrderPriority(int deployOrderPriority) {
            this.deployOrderPriority = deployOrderPriority;
            return this;
        }

        public DbChangeTypeBuilder setDefaultObjectKeyword(String defaultObjectKeyword) {
            this.defaultObjectKeyword = defaultObjectKeyword;
            return this;
        }

        public DbChangeTypeBuilder setDirectoryName(String directoryName) {
            this.directoryName = directoryName;
            return this;
        }

        public DbChangeTypeBuilder setDirectoryNameOld(String directoryNameOld) {
            this.directoryNameOld = directoryNameOld;
            return this;
        }

        public DbChangeTypeBuilder setEnrichableForDependenciesInText(boolean enrichableForDependenciesInText) {
            this.enrichableForDependenciesInText = enrichableForDependenciesInText;
            return this;
        }

        public DbChangeTypeBuilder setDependentObjectRecalculationRequired(boolean dependentObjectRecalculationRequired) {
            this.dependentObjectRecalculationRequired = dependentObjectRecalculationRequired;
            return this;
        }

        public DbChangeTypeBuilder setGrantObjectQualifier(String grantObjectQualifier) {
            this.grantObjectQualifier = grantObjectQualifier;
            return this;
        }

        public DbChangeType build() {
            return new DbChangeTypeImpl(name, rerunnable, deployOrderPriority, directoryNameOld, enrichableForDependenciesInText, dependentObjectRecalculationRequired, grantObjectQualifier, defaultObjectKeyword, directoryName);
        }
    }
}
