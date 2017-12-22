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

import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.DeploySystem;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.RegexUtil;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class that facilitates reading environments from a given input file path.
 *
 * @since 7.0.0
 */
public final class EnvironmentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentFactory.class);
    private static final EnvironmentFactory INSTANCE = new EnvironmentFactory();

    public static EnvironmentFactory getInstance() {
        return INSTANCE;
    }

    private EnvironmentFactory() {}

    public <E extends Environment> ImmutableCollection<E> readFromSourcePath(String sourcePath, String... envNames) {
        EnvironmentLocator dbEnvironmentLocator = new EnvironmentLocator();
        DeploySystem<E> environmentDeploySystem = dbEnvironmentLocator.readSystem(sourcePath);
        MutableCollection<E> environments = environmentDeploySystem.getEnvironments();

        MutableList<E> requestedEnvs = Lists.mutable.empty();

        for (E env : environments) {
            if (envNames == null || envNames.length == 0) {
                requestedEnvs.add(env);
            } else {
                for (String envPattern : envNames) {
                    if (Pattern.compile(RegexUtil.convertWildcardPatternToRegex(envPattern))
                            .matcher(env.getName())
                            .matches()) {
                        requestedEnvs.add(env);
                    }
                }
            }
        }

        if (requestedEnvs.isEmpty()) {
            throw new IllegalArgumentException("No environment with name/s "
                    + Lists.mutable.with(envNames).makeString("(", ",", ")") + " found");
        }

        return requestedEnvs.toImmutable();
    }

    public <E extends Environment> E readOneFromSourcePath(String sourcePath, String... envNames) {
        return CollectionUtil.returnOnlyOne(this.<E>readFromSourcePath(sourcePath, envNames), "Expecting only 1 environment to be found");
    }
}
