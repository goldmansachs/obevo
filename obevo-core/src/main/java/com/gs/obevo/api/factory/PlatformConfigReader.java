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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;

/**
 * Utility to read in properties from default and override locations.
 */
class PlatformConfigReader {
    public Properties readPlatformProperties(RichIterable<String> configPackages) {
        Properties props = new Properties();

        for (String configPackage : configPackages) {
            String defaultConfigPath = configPackage + "/default.properties";
            List<URL> defaultConfigUrls = FileRetrievalMode.getResourcesFromClasspath(defaultConfigPath);

            if (defaultConfigUrls.isEmpty()) {
                throw new IllegalStateException("Could not find default configuration " + defaultConfigPath + " in the classpath");
            }
            if (defaultConfigUrls.size() > 1) {
                throw new IllegalStateException("Found multiple default config files " + defaultConfigPath + " in the classpath; this is not allowed: " + defaultConfigUrls);
            }

            String overrideConfigPath = configPackage + "/override.properties";
            List<URL> overrideConfigUrls = FileRetrievalMode.getResourcesFromClasspath(overrideConfigPath);
            if (overrideConfigUrls.size() > 1) {
                throw new IllegalStateException("Found multiple default config files " + overrideConfigPath + " in the classpath; this is not allowed: " + overrideConfigUrls);
            }

            loadPropertiesFromUrl(defaultConfigUrls, props);

            if (overrideConfigUrls.size() == 1) {
                loadPropertiesFromUrl(overrideConfigUrls, props);
            }
        }

        return props;
    }

    private void loadPropertiesFromUrl(List<URL> defaultConfigUrls, Properties props) {
        InputStream defaultStream = null;
        try {
            defaultStream = defaultConfigUrls.get(0).openStream();
            props.load(defaultStream);
        } catch (IOException e) {
            throw new DeployerRuntimeException(e);
        } finally {
            IOUtils.closeQuietly(defaultStream);
        }
    }
}
