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

import java.io.File;
import java.util.Collection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.util.inputreader.Credential;

public interface DeployerAppContext<E extends Environment, Self extends DeployerAppContext<E, Self>> {
    E getEnvironment();

    Self setEnvironment(E env);

    Self setCredential(Credential credential);

    File getWorkDir();

    Self setWorkDir(File workDir);

    Self buildDbContext();

    Self buildFileContext();

    Self build();

    Self deploy();

    Self deploy(MainDeployerArgs deployerArgs);

    Self deploy(Collection<Change> changes);

    Self deploy(Collection<Change> changes, MainDeployerArgs deployerArgs);

    DeployMetrics getDeployMetrics();

    Self setFileSourceContext(FileSourceContext source);

    Self setupEnvInfra();

    Self setupEnvInfra(boolean strictSetupEnvInfra);

    Self cleanEnvironment();
}
