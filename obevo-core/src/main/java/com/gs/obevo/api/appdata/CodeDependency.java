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

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;

/**
 * Indicates a dependency from one object to another object.
 */
public class CodeDependency {
    public static final Function<CodeDependency, String> TO_TARGET = new Function<CodeDependency, String>() {
        @Override
        public String valueOf(CodeDependency object) {
            return object.getTarget();
        }
    };
    public static final Function2<String, CodeDependencyType, CodeDependency> CREATE_WITH_TYPE = new Function2<String, CodeDependencyType, CodeDependency>() {
        @Override
        public CodeDependency value(String target, CodeDependencyType codeDependencyType) {
            return new CodeDependency(target, codeDependencyType);
        }
    };

    private final String target;
    private final CodeDependencyType codeDependencyType;

    public CodeDependency(String target, CodeDependencyType codeDependencyType) {
        this.target = target;
        this.codeDependencyType = codeDependencyType;
    }

    /**
     * The target object that this depends on.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Dependency type - mainly used for debugging/logging for end-users.
     */
    public CodeDependencyType getCodeDependencyType() {
        return codeDependencyType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CodeDependency that = (CodeDependency) o;
        return Objects.equals(target, that.target) &&
                codeDependencyType == that.codeDependencyType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, codeDependencyType);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("target", target)
                .append("codeDependencyType", codeDependencyType)
                .toString();
    }
}