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

import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration2.HierarchicalConfiguration;

interface FileConfigReader {
    HierarchicalConfiguration getConfig(FileObject checkoutFolder);

    /**
     * Returns true if we should try to build an environment from here
     * This is here so that we can facilitate the autodetection of the env type based on the input location
     */
    boolean isEnvironmentOfThisTypeHere(FileObject sourcePath);
}
