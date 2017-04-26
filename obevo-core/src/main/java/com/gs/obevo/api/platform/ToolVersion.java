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
package com.gs.obevo.api.platform;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang3.Validate;

public class ToolVersion {
    private static final String PROPERTY_PATH = "com/gs/obevo/config/default.properties";

    private static String TOOL_VERSION = null;

    public static synchronized String getToolName() {
        return "obevo";  // TODO read this from configuration to allow different distributions to name this accordingly
    }

    public static synchronized String getToolVersion() {
        if (TOOL_VERSION == null) {

            Properties props = new Properties();
            try {
                URL resource = Validate.notNull(ToolVersion.class.getClassLoader().getResource(PROPERTY_PATH), "Could not read the required propertyPath: " + PROPERTY_PATH);
                props.load(resource.openStream());
            } catch (IOException e) {
                throw new RuntimeException("Could not open the required propertyPath: " + PROPERTY_PATH, e);
            }
            TOOL_VERSION = props.getProperty("tool.version");
        }

        return TOOL_VERSION;
    }
}
