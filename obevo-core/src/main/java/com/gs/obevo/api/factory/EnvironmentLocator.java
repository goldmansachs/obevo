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
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import com.gs.obevo.util.vfs.VFSFileSystemException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EnvironmentLocator {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentLocator.class);

    private final RichIterable<FileConfigReader> configReaders;
    private final PlatformConfiguration platformConfiguration;

    public EnvironmentLocator() {
        this(Lists.immutable.<FileConfigReader>of(new XmlFileConfigReader()));
    }

    public EnvironmentLocator(RichIterable<FileConfigReader> configReaders) {
        this.configReaders = configReaders;
        this.platformConfiguration = PlatformConfiguration.getInstance();
    }

    public <T extends Environment> DeploySystem<T> readSystem(String sourcePathStr) {
        DeploySystem<T> deploySystem = readSystemOptional(sourcePathStr);
        if (deploySystem != null) {
            return deploySystem;
        } else {
            throw new IllegalArgumentException("Could not find valid system at this location (neither in classpath nor file system): " + sourcePathStr + ". Possible diagnosis: check if there is a valid system-config.xml file?");
        }
    }

    public <T extends Environment> DeploySystem<T> readSystemOptional(String sourcePathStr) {
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

            for (FileConfigReader configReader : configReaders) {
                if (sourcePath.exists() && configReader.isEnvironmentOfThisTypeHere(sourcePath)) {
                    LOG.info("Checker for environment type w/ enricher "
                            + configReader.getClass().getSimpleName() + " here");
                    HierarchicalConfiguration config = configReader.getConfig(sourcePath);
                    Platform platform = platformConfiguration.valueOf(config.getString("[@type]"));
                    DeploySystem<T> sys = platform.getEnvironmentEnricher().readSystem(config, sourcePath);

                    if (sys != null) {
                        return sys;
                    }
                }
            }

        }

        return null;
    }
}
