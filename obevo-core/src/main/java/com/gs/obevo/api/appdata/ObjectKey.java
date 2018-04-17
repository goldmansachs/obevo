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
package com.gs.obevo.api.appdata;

import com.gs.obevo.api.platform.ChangeType;
import org.apache.commons.lang3.Validate;

/**
 * The fields that consist the identity of objects within a client codebase.
 */
public final class ObjectKey {
    private final String schema;
    private final ChangeType changeType;
    private final String objectName;

    public ObjectKey(String schema, ChangeType changeType, String objectName) {
        this.schema = Validate.notNull(schema);
        this.changeType = Validate.notNull(changeType);
        this.objectName = Validate.notNull(objectName);
    }

    public String getSchema() {
        return schema;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getChangeTypeName() {
        return changeType.getName();
    }

    public String getObjectName() {
        return objectName;
    }

    @Override
    public String toString() {
        return "[schema: " + schema + ", changeTypeName: " + changeType.getName() + ", objectName: " + objectName + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectKey that = (ObjectKey) o;

        if (!schema.equals(that.schema)) {
            return false;
        }
        if (!changeType.getName().equals(that.changeType.getName())) {
            return false;
        }
        return objectName.equals(that.objectName);
    }

    @Override
    public int hashCode() {
        int result = schema.hashCode();
        result = 31 * result + changeType.getName().hashCode();
        result = 31 * result + objectName.hashCode();
        return result;
    }
}
