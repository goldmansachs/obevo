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

import com.gs.obevo.api.factory.PlatformConfiguration;

/**
 * Utility class to access the version of Obevo.
 * @deprecated use {@link PlatformConfiguration}
 */
@Deprecated
public class ToolVersion {
    /**
     * Returns the product name to use for output.
     * @deprecated use {@link PlatformConfiguration#getToolName()}
     */
    @Deprecated
    public static synchronized String getToolName() {
        return PlatformConfiguration.getInstance().getToolName();
    }

    /**
     * Returns the product version to use for output.
     * @deprecated use {@link PlatformConfiguration#getToolVersion()} ()}
     */
    @Deprecated
    public static synchronized String getToolVersion() {
        return PlatformConfiguration.getInstance().getToolVersion();
    }
}
