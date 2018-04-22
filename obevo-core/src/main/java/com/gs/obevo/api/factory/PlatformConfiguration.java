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
package com.gs.obevo.api.factory;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.api.platform.Platform;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains any common default configurations used by all Obevo implementations.
 */
public class PlatformConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(PlatformConfiguration.class);
    private static final PlatformConfiguration INSTANCE = new PlatformConfiguration();

    private final ImmutableHierarchicalConfiguration config;
    private final String toolVersion;
    private final ImmutableMap<String, Integer> featureToggleVersions;
    private final String sourceEncoding;
    private final ImmutableMap<String, ImmutableHierarchicalConfiguration> platformConfigs;

    public static PlatformConfiguration getInstance() {
        return INSTANCE;
    }

    protected PlatformConfiguration() {
        this.config = new PlatformConfigReader().readPlatformProperties(getConfigPackages());
        this.toolVersion = config.getString("tool.version");
        this.featureToggleVersions = createFeatureToggleVersions();
        this.sourceEncoding = config.getString("sourceEncoding");
        this.platformConfigs = getDbPlatformMap();
    }

    public Platform valueOf(String dbPlatformStr) {
        try {
            ImmutableHierarchicalConfiguration platformConfig = platformConfigs.get(dbPlatformStr);

            String resolvedDbPlatformClass = null;
            if (platformConfig != null) {
                resolvedDbPlatformClass = platformConfig.getString("class");
            }

            if (resolvedDbPlatformClass == null) {
                resolvedDbPlatformClass = dbPlatformStr;
            }

            return (Platform) Class.forName(resolvedDbPlatformClass).newInstance();
        } catch (InstantiationException e) {
            throw new DeployerRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new DeployerRuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    /**
     * Returns the default name-to-platform mappings. We put this in a separate protected method to allow external
     * distributions to override these values as needed.
     */
    private ImmutableMap<String, ImmutableHierarchicalConfiguration> getDbPlatformMap() {
        final String platformKey = "db.platforms";

        ListIterable<ImmutableHierarchicalConfiguration> platformConfigs = ListAdapter.adapt(config.immutableChildConfigurationsAt("db.platforms"));

        MutableMap<String, ImmutableHierarchicalConfiguration> platformByName = Maps.mutable.empty();

        for (ImmutableHierarchicalConfiguration platformConfig : platformConfigs) {
            String platformName = platformConfig.getRootElementName();
            String platformClass = platformConfig.getString("class");
            if (platformClass == null) {
                LOG.warn("Improper platform config under {} for platform {}: missing class property. Will skip", platformKey, platformName);
            } else {
                platformByName.put(platformName, platformConfig);
                LOG.debug("Registering platform {} at class {}", platformName, platformClass);
            }
        }

        return platformByName.toImmutable();
    }

    public ImmutableHierarchicalConfiguration getPlatformConfig(String platformName) {
        return platformConfigs.get(platformName);
    }

    /**
     * Returns the resources to read from to populate this instance. To be overriden by child implementations.
     */
    protected ImmutableList<String> getConfigPackages() {
        return Lists.immutable.of("com/gs/obevo/config", "com/gs/obevo/db/config");
    }

    protected ImmutableHierarchicalConfiguration getConfig() {
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

    public int getFeatureToggleVersion(String featureToggleName) {
        return featureToggleVersions.getIfAbsentValue(featureToggleName, 0);
    }

    private ImmutableMap<String, Integer> createFeatureToggleVersions() {
        MutableList<ImmutableHierarchicalConfiguration> featureToggles = ListAdapter.adapt(config.immutableChildConfigurationsAt("featureToggles"));

        return featureToggles.toMap(ImmutableHierarchicalConfiguration::getRootElementName, config -> config.getInt("defaultVersion")).toImmutable();
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }
}
