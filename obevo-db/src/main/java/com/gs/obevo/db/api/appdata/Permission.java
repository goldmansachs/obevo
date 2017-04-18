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
package com.gs.obevo.db.api.appdata;

import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;

public class Permission {
    public static final Function<Permission, String> TO_SCHEME = new Function<Permission, String>() {
        @Override
        public String valueOf(Permission permission) {
            return permission.getScheme();
        }
    };
    public static final Function<Permission, ImmutableList<Grant>> TO_GRANTS = new Function<Permission, ImmutableList<Grant>>() {
        @Override
        public ImmutableList<Grant> valueOf(Permission permission) {
            return permission.getGrants();
        }
    };

    private final String scheme;
    private final ImmutableList<Grant> grants;

    public Permission(String scheme, ImmutableList<Grant> grants) {
        this.scheme = scheme;
        this.grants = Validate.notNull(grants);
    }

    public String getScheme() {
        return this.scheme;
    }

    public ImmutableList<Grant> getGrants() {
        return this.grants;
    }
}
