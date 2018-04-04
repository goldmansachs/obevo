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
package com.gs.obevo.db.impl.core.envinfrasetup;

import com.gs.obevo.api.appdata.Environment;

/**
 * Command interface for setting up an environment instance infrastructure, i.e. not the code objects in your system,
 * but the environment itself as defined in your system configuration.
 *
 * Typically this is a one-time activity for the lifetime of the environment, after which the major activities are at
 * the object level.
 *
 * This should be an idempotent operation (i.e. if called after the first setup, then subsequent calls should either be
 * no-ops or applying changes incrementally if there is a diff).
 *
 * In many cases, this may be a no-op implementation if in your firm the infrastructure is setup by the infrastructure
 * team (e.g. database administrators for DB platform) and not by the application team.
 */
public interface EnvironmentInfraSetup<E extends Environment> {
    /**
     * Sets up the environment - should be an idempotent call. See {@link EnvironmentInfraSetup} javadoc for details.
     */
    void setupEnvInfra(boolean failOnSetupException, boolean forceCreation);
}
