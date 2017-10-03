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
package com.gs.obevo.api.platform;

/**
 * Basic implementation of {@link ChangeType} that clients can quickly define a set of types with.
 */
public class ChangeTypeImpl implements ChangeType {
    private ChangeTypeBehavior changeTypeBehavior;

    private final String name;
    private final boolean rerunnable;
    private final int deployOrderPriority;
    private final String directoryNameOld;
    private final boolean enrichableForDependenciesInText;
    private final boolean dependentObjectRecalculationRequired;
    private final String directoryName;
    private final ChangeType bodyChangeType;

    protected ChangeTypeImpl(String name, boolean rerunnable, int deployOrderPriority, String directoryNameOld, boolean enrichableForDependenciesInText, boolean dependentObjectRecalculationRequired, String directoryName, ChangeType bodyChangeType) {
        this.name = name;
        this.rerunnable = rerunnable;
        this.deployOrderPriority = deployOrderPriority;
        this.directoryName = directoryName != null ? directoryName : getName().toLowerCase();
        this.directoryNameOld = directoryNameOld;
        this.enrichableForDependenciesInText = enrichableForDependenciesInText;
        this.dependentObjectRecalculationRequired = dependentObjectRecalculationRequired;
        this.bodyChangeType = bodyChangeType;
    }

    public static ChangeTypeImplBuilder newChangeType(String name, boolean rerunnable, int deployOrderPriority) {
        return new ChangeTypeImplBuilder(name, rerunnable, deployOrderPriority);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isRerunnable() {
        return rerunnable;
    }

    @Override
    public int getDeployOrderPriority() {
        return deployOrderPriority;
    }

    @Override
    public String getDirectoryName() {
        return directoryName;
    }

    @Override
    public String getDirectoryNameOld() {
        return directoryNameOld;
    }

    @Override
    public boolean isEnrichableForDependenciesInText() {
        return enrichableForDependenciesInText;
    }

    @Override
    public boolean isDependentObjectRecalculationRequired() {
        return dependentObjectRecalculationRequired;
    }

    @Override
    public ChangeType getBodyChangeType() {
        return bodyChangeType;
    }

    protected ChangeTypeBehavior getChangeTypeBehavior() {
        return changeTypeBehavior;
    }

    public static class ChangeTypeImplBuilder {
        private final String name;
        private final boolean rerunnable;
        private final int deployOrderPriority;
        private String directoryName = null;
        private String directoryNameOld = null;
        private boolean enrichableForDependenciesInText = true;
        private boolean dependentObjectRecalculationRequired = true;
        private ChangeType bodyChangeType;

        protected ChangeTypeImplBuilder(String name, boolean rerunnable, int deployOrderPriority) {
            this.name = name;
            this.rerunnable = rerunnable;
            this.deployOrderPriority = deployOrderPriority;
        }

        /**
         * The directory name where the change type files will reside. Optional - by default, the value will be the
         * lowercase name of the change.
         */
        public ChangeTypeImplBuilder setDirectoryName(String directoryName) {
            this.directoryName = directoryName;
            return this;
        }

        public ChangeTypeImplBuilder setDirectoryNameOld(String directoryNameOld) {
            this.directoryNameOld = directoryNameOld;
            return this;
        }

        public ChangeTypeImplBuilder setEnrichableForDependenciesInText(boolean enrichableForDependenciesInText) {
            this.enrichableForDependenciesInText = enrichableForDependenciesInText;
            return this;
        }

        public ChangeTypeImplBuilder setDependentObjectRecalculationRequired(boolean dependentObjectRecalculationRequired) {
            this.dependentObjectRecalculationRequired = dependentObjectRecalculationRequired;
            return this;
        }

        public ChangeTypeImplBuilder setBodyChangeType(ChangeType bodyChangeType) {
            this.bodyChangeType = bodyChangeType;
            return this;
        }

        public ChangeTypeImpl build() {
            return new ChangeTypeImpl(name, rerunnable, deployOrderPriority, directoryNameOld, enrichableForDependenciesInText, dependentObjectRecalculationRequired, directoryName, bodyChangeType);
        }
    }

    @Override
    public String toString() {
        return "ChangeTypeImpl{" +
                "name='" + name + '\'' +
                ", rerunnable=" + rerunnable +
                ", deployOrderPriority=" + deployOrderPriority +
                ", directoryNameOld='" + directoryNameOld + '\'' +
                ", enrichableForDependenciesInText=" + enrichableForDependenciesInText +
                ", dependentObjectRecalculationRequired=" + dependentObjectRecalculationRequired +
                '}';
    }


}
