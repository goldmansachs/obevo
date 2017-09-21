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
package com.gs.obevo.api.factory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Contains any common default configurations used by all Obevo implementations.
 */
public class PlatformConfiguration {
    private static final PlatformConfiguration INSTANCE = new PlatformConfiguration();

    private final Config config;
    private final String toolVersion;

    public static PlatformConfiguration getInstance() {
        return INSTANCE;
    }

    protected PlatformConfiguration() {
        this.config = ConfigFactory.parseProperties(new PlatformConfigReader().readPlatformProperties(getConfigPackages()));
        this.toolVersion = config.getString("tool.version");
    }

    /**
     * Returns the resources to read from to populate this instance. To be overriden by child implementations.
     */
    protected ImmutableList<String> getConfigPackages() {
        return Lists.immutable.of("com/gs/obevo/config");
    }

    public Config getConfig() {
        return config;
    }

    /**
     * Returns the product name to use for output.
     */
    public String getToolName() {
        return "obevo";  // TODO read this from configuration to allow different distributions to name this accordingly
    }

    /**
     * Returns the product version to use for output.
     */
    public String getToolVersion() {
        return toolVersion;
    }
}
