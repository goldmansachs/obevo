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

import java.util.regex.Pattern;

import com.gs.obevo.api.factory.EnvironmentLocator;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.RegexUtil;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbEnvironmentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DbEnvironmentFactory.class);
    private static final DbEnvironmentFactory INSTANCE = new DbEnvironmentFactory();

    public static DbEnvironmentFactory getInstance() {
        return INSTANCE;
    }

    protected DbEnvironmentFactory() {}

    public MutableCollection<DbEnvironment> readFromSourcePath(String sourcePath, String... envNames) {
        EnvironmentLocator<DbEnvironment> dbEnvironmentLocator = new EnvironmentLocator<DbEnvironment>(new DbEnvironmentXmlEnricher());
        MutableCollection<DbEnvironment> environments = dbEnvironmentLocator.readSystem(sourcePath).getEnvironments();

        MutableList<DbEnvironment> requestedEnvs = Lists.mutable.empty();

        for (DbEnvironment env : environments) {
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

        return requestedEnvs;
    }

    public DbEnvironment readOneFromSourcePath(String sourcePath, String... envNames) {
        return CollectionUtil.returnOnlyOne(readFromSourcePath(sourcePath, envNames), "Expecting only 1 environment to be found");
    }
}
