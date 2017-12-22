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

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.vfs2.FileType;

public class XmlFileConfigReader implements FileConfigReader {

    private FileObject getEnvFileToRead(FileObject sourcePath) {
        if (sourcePath.getType() == FileType.FILE && sourcePath.isReadable() && sourcePath.getName().getExtension().equalsIgnoreCase("xml")) {
            return sourcePath;
        } else {
            return sourcePath.getChild("system-config.xml");
        }
    }

    @Override
    public boolean isEnvironmentOfThisTypeHere(FileObject sourcePath) {
        return this.getEnvFileToRead(sourcePath) != null;
    }


    @Override
    public HierarchicalConfiguration getConfig(FileObject checkoutFolder) {
        XMLConfiguration config;
        try {
            config = new XMLConfiguration();
            config.load(getEnvFileToRead(checkoutFolder).getURLDa());
            return config;
        } catch (ConfigurationException exc) {
            throw new DeployerRuntimeException(exc);
        }
    }
}
