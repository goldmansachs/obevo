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
package com.gs.obevo.db.api.appdata;

import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.Multimap;

/**
 * Represents a set of privileges that should be provided to the given grantTargets (i.e. the users/groups).
 * The object would be specified separately.
 */
public class Grant {
    private ImmutableCollection<String> privileges;
    private ImmutableMultimap<GrantTargetType, String> grantTargets;

    public Grant(ImmutableCollection<String> privileges, ImmutableMultimap<GrantTargetType, String> grantTargets) {
        this.privileges = Validate.notNull(privileges);
        this.grantTargets = Validate.notNull(grantTargets);
    }

    public ImmutableCollection<String> getPrivileges() {
        return this.privileges;
    }

    public Multimap<GrantTargetType, String> getGrantTargets() {
        return grantTargets;
    }

    public void validate() {
        if (privileges.isEmpty()) {
            throw new RuntimeException("privileges cannot be empty!!!");
        }
        if (grantTargets.isEmpty()) {
            throw new RuntimeException("grantTargets (i.e. user or group list for a grant) cannot be empty!!!");
        }
    }
}
