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
package com.gs.obevo.db.api.appdata;

import java.util.Objects;

import com.gs.obevo.dbmetadata.api.DaDatabaseObject;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents a Directory object, particularly used in Oracle; see <a href="https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_5007.htm">Oracle doc</a>.
 *
 * Note that a directory is an object for the server, not just a schema. Hence, this does not extend {@link DaDatabaseObject}
 * like many other DB object types.
 */
public class ServerDirectory {
    private final String name;
    private final String directoryPath;

    public ServerDirectory(String name, String directoryPath) {
        this.name = Objects.requireNonNull(name);
        this.directoryPath = Objects.requireNonNull(directoryPath);
    }

    public String getName() {
        return name;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerDirectory that = (ServerDirectory) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(directoryPath, that.directoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, directoryPath);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("directoryPath", directoryPath)
                .toString();
    }
}
