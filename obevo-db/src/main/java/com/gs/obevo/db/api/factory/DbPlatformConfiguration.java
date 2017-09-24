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
package com.gs.obevo.db.api.factory;

import com.gs.obevo.api.factory.PlatformConfiguration;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.typesafe.config.Config;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbPlatformConfiguration extends PlatformConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DbPlatformConfiguration.class);
    private static final DbPlatformConfiguration INSTANCE = new DbPlatformConfiguration();

    private final Config platformConfigs;
    private final ImmutableMap<String, String> dbPlatformMap;
    private final ImmutableSet<String> extraEnvAttrs;
    private final ImmutableMap<String, Integer> featureToggleVersions;
    private final String sourceEncoding;

    public static DbPlatformConfiguration getInstance() {
        return INSTANCE;
    }

    private DbPlatformConfiguration() {
        super();
        this.platformConfigs = this.getConfig().getConfig("db").getConfig("platforms");
        this.dbPlatformMap = getDbPlatformMap();
        this.extraEnvAttrs = createExtraEnvAttrs();
        this.featureToggleVersions = createFeatureToggleVersions();
        this.sourceEncoding = this.getConfig().getString("sourceEncoding");
    }

    @Override
    protected ImmutableList<String> getConfigPackages() {
        return super.getConfigPackages().newWith("com/gs/obevo/db/config");
    }

    public DbPlatform valueOf(String dbPlatformStr) {
        try {
            String resolvedDbPlatformClass = dbPlatformMap.get(dbPlatformStr);
            if (resolvedDbPlatformClass == null) {
                resolvedDbPlatformClass = dbPlatformStr;
            }
            return (DbPlatform) Class.forName(resolvedDbPlatformClass).newInstance();
        } catch (InstantiationException e) {
            throw new DeployerRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new DeployerRuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    public String getSourceEncoding() {
        return sourceEncoding;
    }

    /**
     * Returns the default name-to-platform mappings. We put this in a separate protected method to allow external
     * distributions to override these values as needed.
     */
    protected ImmutableMap<String, String> getDbPlatformMap() {
        MutableMap<String, String> platformByName = Maps.mutable.empty();

        for (String platformName : platformConfigs.root().keySet()) {
            String platformClass = getPlatformConfig(platformName).getString("class");
            platformByName.put(platformName, platformClass);
            LOG.debug("Registering platform {} at class {}", platformName, platformClass);
        }

        return platformByName.toImmutable();
    }

    public Config getPlatformConfig(String platformName) {
        return platformConfigs.getConfig(platformName);
    }

    public ImmutableSet<String> getExtraEnvAttrs() {
        return extraEnvAttrs;
    }

    public int getFeatureToggleVersion(String featureToggleName) {
        return featureToggleVersions.getIfAbsentValue(featureToggleName, 0);
    }

    protected ImmutableSet<String> createExtraEnvAttrs() {
        if (!this.getConfig().hasPath("extraEnvAttrs")) {
            return Sets.immutable.empty();
        }
        final Config attrConfig = this.getConfig().getConfig("extraEnvAttrs");
        return SetAdapter.adapt(attrConfig.root().keySet()).collect(new Function<String, String>() {
            @Override
            public String valueOf(String attrNumber) {
                return attrConfig.getConfig(attrNumber).getString("name");
            }
        }).toImmutable();
    }

    private ImmutableMap<String, Integer> createFeatureToggleVersions() {
        if (!this.getConfig().hasPath("featureToggles")) {
            return Maps.immutable.empty();
        }
        final Config attrConfig = this.getConfig().getConfig("featureToggles");
        return SetAdapter.adapt(attrConfig.root().keySet()).toMap(Functions.<String>getPassThru(), new Function<String, Integer>() {
            @Override
            public Integer valueOf(String featureToggleName) {
                return attrConfig.getConfig(featureToggleName).getInt("defaultVersion");
            }
        }).toImmutable();
    }
}
