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
package com.gs.obevo.db.impl.core.envinfrasetup;

import com.gs.obevo.api.appdata.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default no-op environment setup. This is for platforms that are not setup programmatically, but instead by an
 * external party (i.e. DBA team).
 */
public class NoOpEnvironmentInfraSetup implements EnvironmentInfraSetup<Environment> {
    private static final Logger LOG = LoggerFactory.getLogger(NoOpEnvironmentInfraSetup.class);

    @Override
    public void setupEnvInfra(boolean failOnSetupException) {
        LOG.info("This environment is configured not to need any additional setup");
    }
}
