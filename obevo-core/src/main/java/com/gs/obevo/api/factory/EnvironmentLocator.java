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

import com.gs.obevo.api.appdata.DeploySystem;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import com.gs.obevo.util.vfs.VFSFileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentLocator<T extends Environment> {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentLocator.class);

    private final EnvironmentEnricher<T> envEnricher;

    public EnvironmentLocator(EnvironmentEnricher<T> envEnricher) {
        this.envEnricher = envEnricher;
    }

    /**
     * This version reading from the cache is here to help w/ performance for unit tests (e.g. avoid excessive calls
     * to read from the file system; see DEPLOYANY-201
     */
    public DeploySystem<T> readSystem(String sourcePathStr) {
        for (FileRetrievalMode fileRetrievalMode : FileRetrievalMode.values()) {
            FileObject sourcePath;
            try {
                sourcePath = fileRetrievalMode.resolveSingleFileObject(sourcePathStr);
                if (sourcePath == null) {
                    LOG.debug("Source not found using " + fileRetrievalMode.name()
                                            + " retrieval mode; will try the next ones");
                    continue;
                }
            } catch (VFSFileSystemException exc) {
                LOG.debug("Unable to read the path from " + fileRetrievalMode.name()
                        + "; will try the next ones", exc);
                continue;
            }

            if (sourcePath.exists() && this.envEnricher.isEnvironmentOfThisTypeHere(sourcePath)) {
                LOG.info("Checker for environment type w/ enricher "
                        + this.envEnricher.getClass().getSimpleName() + " here");
                DeploySystem<T> sys = this.envEnricher.readSystem(sourcePath);
                if (sys != null) {
                    return sys;
                }
            }
        }

        throw new IllegalArgumentException("Could not find valid system at this location (neither in classpath nor file system): " + sourcePathStr + ". Possible diagnosis: check if there is a valid system-config.xml file?");
    }
}
