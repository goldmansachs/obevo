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
package com.gs.obevo.util.vfs;

import org.eclipse.collections.api.list.ListIterable;

/**
 * Strategy pattern for retrieving files given an input string. This is here so that various clients can interpret
 * an input file string differently (e.g. for custom deploy mechanisms within firms that don't rely on standard
 * file/classpath lookups).
 */
public interface FileResolverStrategy {
    /**
     * Attempts to find a FileObject for the given path. Returns null if it cannot find it.
     */
    ListIterable<FileObject> resolveFileObjects(String path);
}
