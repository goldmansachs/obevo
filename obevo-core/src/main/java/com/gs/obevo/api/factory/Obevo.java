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

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.inputreader.Credential;
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * Entry point to Obevo API.
 *
 * @since 6.5.0
 */
public final class Obevo {
    private Obevo() {
    }

    public static DeployerAppContext buildContext(Environment env, String username, String password) {
        return buildContext(env, new Credential(username, password));
    }

    public static DeployerAppContext buildContext(Environment env, Credential credential) {
        DeployerAppContext appContextBuilder = getAppContextBuilder(env, credential);
        appContextBuilder.build();
        return appContextBuilder;
    }

    public static <E extends Environment> E readEnvironment(String sourcePath, String... envNames) {
        return EnvironmentFactory.getInstance().readOneFromSourcePath(sourcePath, envNames);
    }

    public static <E extends Environment> ImmutableCollection<E> readEnvironments(String sourcePath, String... envNames) {
        return EnvironmentFactory.getInstance().readFromSourcePath(sourcePath, envNames);
    }

    private static DeployerAppContext getAppContextBuilder(Environment env, Credential credential) {
        DeployerAppContext deployerAppContext;
        try {
            deployerAppContext = env.getPlatform().getAppContextBuilderClass().newInstance();
        } catch (InstantiationException e) {
            throw new DeployerRuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new DeployerRuntimeException(e);
        }
        deployerAppContext.setEnvironment(env);

        if (credential == null) {
            credential = getDefaultCredential(env);
        }
        deployerAppContext.setCredential(credential);

        return deployerAppContext;
    }

    private static Credential getDefaultCredential(Environment env) {
        if (env.getDefaultUserId() != null && env.getDefaultPassword() != null) {
            return new Credential(env.getDefaultUserId(), env.getDefaultPassword());
        }
        return null;
    }
}
