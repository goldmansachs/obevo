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

import java.sql.Connection;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeTypeBehavior;
import com.gs.obevo.db.api.appdata.Permission;
import org.eclipse.collections.api.RichIterable;

public interface DbChangeTypeBehavior extends ChangeTypeBehavior {

    /**
     * Applies tie grants to the given object. This method should not throw an exception if the grant execution fails,
     * at least until we re-evaluate the behavior as part of issue #3 in Github - https://github.com/goldmansachs/obevo/issues/3
     */
    void applyGrants(Connection conn, PhysicalSchema schema, String objectName, RichIterable<Permission> permsToApply);
}
