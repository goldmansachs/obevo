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

import org.eclipse.collections.api.map.ImmutableMap;

/**
 * Read-only container for statistics of a particular deploy execution.
 */
public interface DeployMetrics {
    String WARNINGS_PREFIX = "warnings";  // clients can key off this constant to find all warnings that need to be handled
    String BAD_FILE_FORMAT_WARNINGS = WARNINGS_PREFIX + ".badFileFormat";
    String UNEXPECTED_FILE_EXTENSIONS = WARNINGS_PREFIX + ".unexpectedFileExtensions";

    ImmutableMap<String, Object> toSerializedForm();
}
