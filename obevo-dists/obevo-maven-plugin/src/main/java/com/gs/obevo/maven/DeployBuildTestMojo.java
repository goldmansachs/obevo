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
package com.gs.obevo.maven;

import com.gs.obevo.util.inputreader.Credential;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST)
public class DeployBuildTestMojo extends AbstractDeployMojo {
    public void execute() throws MojoExecutionException {
        if (this.cleanFirst == null) {
            this.setCleanFirst(true);
        }
        this.setNoPrompt(true);
        Credential credential = this.getCredential();
        if (credential == null || !credential.isAuthenticationMethodProvided()) {
            throw new MojoExecutionException("Credential must be provided with authentication mode: " + credential);
        }

        this.setAllChangesets(true);

        this.validateIsPopulated(this.getSourcePath(), "sourcePath");
        super.execute();
    }
}
