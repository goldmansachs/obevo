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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.api.collection.MutableCollection;

public class DeploySystem<T extends Environment> {
    private final MutableCollection<T> environments;

    public DeploySystem(MutableCollection<T> environments) {
        this.environments = environments;
    }

    public MutableCollection<T> getEnvironments() {
        return this.environments;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .toString();
    }

    public T getEnvironment(String envName) {
        return this.environments.detect(_this -> _this.getName().equals(envName));
    }
}
