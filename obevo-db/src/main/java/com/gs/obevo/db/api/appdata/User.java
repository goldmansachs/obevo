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

public class User {
    public static final Function<User, String> TO_NAME = new Function<User, String>() {
        @Override
        public String valueOf(User object) {
            return object.getName();
        }
    };

    private final String name;
    private final String password;
    private final boolean admin;

    public User(String name, String password, boolean admin) {
        this.name = Validate.notNull(name);
        this.password = password;
        this.admin = admin;
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean isAdmin() {
        return this.admin;
    }
}
