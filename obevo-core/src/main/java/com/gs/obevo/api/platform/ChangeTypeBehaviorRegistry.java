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


import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;

/**
 * Registry for the changeTypeBehavior instances for the given environment.
 */
public class ChangeTypeBehaviorRegistry {

    private final ImmutableMap<String, ChangeTypeBehavior> changeTypeBehaviors;

    public ChangeTypeBehaviorRegistry(MutableMap<String, ChangeTypeBehavior> changeTypeBehaviors) {
        this.changeTypeBehaviors = changeTypeBehaviors.toImmutable();
    }

    public ChangeTypeBehavior getChangeTypeBehavior(String changeTypeName) {
        return changeTypeBehaviors.get(changeTypeName);
    }
}
