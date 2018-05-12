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
package com.gs.obevo.db.api.factory;

import com.gs.obevo.api.factory.PlatformConfiguration;
import com.gs.obevo.db.api.platform.DbPlatform;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbPlatformConfiguration extends PlatformConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DbPlatformConfiguration.class);
    private static final DbPlatformConfiguration INSTANCE = new DbPlatformConfiguration();

    private final ImmutableSet<String> extraEnvAttrs;

    public static DbPlatformConfiguration getInstance() {
        return INSTANCE;
    }

    private DbPlatformConfiguration() {
        super();
        this.extraEnvAttrs = createExtraEnvAttrs();
    }

    @Override
    protected ImmutableList<String> getConfigPackages() {
        return super.getConfigPackages().newWith("com/gs/obevo/db/config");
    }

    public DbPlatform valueOf(String dbPlatformStr) {
        return (DbPlatform) super.valueOf(dbPlatformStr);
    }

    public ImmutableSet<String> getExtraEnvAttrs() {
        return extraEnvAttrs;
    }

    private ImmutableSet<String> createExtraEnvAttrs() {
        ListIterable<ImmutableHierarchicalConfiguration> extraEnvAttrs = ListAdapter.adapt(getConfig().immutableChildConfigurationsAt("extraEnvAttrs"));
        return extraEnvAttrs.collect(new Function<ImmutableHierarchicalConfiguration, String>() {
            @Override
            public String valueOf(ImmutableHierarchicalConfiguration c) {
                return c.getString("name");
            }
        }).toSet().toImmutable();
    }
}
