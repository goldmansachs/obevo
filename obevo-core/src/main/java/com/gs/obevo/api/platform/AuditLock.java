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
package com.gs.obevo.api.platform;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.impl.MainDeployer;

/**
 * Component to ensure that only a single client can invoke a deploy on an Obevo environment.
 * This component is mainly called from {@link MainDeployer}.
 * As of today, the lock is environment wide (i.e. not per {@link PhysicalSchema}); this may be refactored in the future.
 */
public interface AuditLock {
    /**
     * Acquire a lock on the environment.
     */
    void lock();

    /**
     * Release the lock on the environment. Okay to throw exceptions here, as {@link MainDeployer} will handle
     * and ignore exceptions when calling this.
     */
    void unlock();
}
