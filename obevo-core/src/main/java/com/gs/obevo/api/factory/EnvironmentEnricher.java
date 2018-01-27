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

import com.gs.obevo.api.appdata.DeploySystem;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration.HierarchicalConfiguration;

public interface EnvironmentEnricher<T extends Environment> {
    /**
     * Reads the environment that is found in the configurations inside sourcePath
     * This would be called when the provided folder is already tokenized and only one environment is expected
     * In this case, no environment is passed in via command line
     *
     * An error will be thrown
     */
    DeploySystem<T> readSystem(HierarchicalConfiguration config, FileObject sourcePath);
}
