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
package com.gs.obevo.dbmetadata.api;

/**
 * Represents a Directory object, particularly used in Oracle; see <a href="https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_5007.htm">Oracle doc</a>.
 *
 * Note that a directory is an object for the server, not just a schema. Hence, this does not extend {@link DaDatabaseObject}
 * like many other DB object types.
 */
public interface DaDirectory extends DaNamedObject {
    String getDirectoryPath();
}
