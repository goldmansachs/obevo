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

import org.apache.commons.lang3.Validate;

/**
 * The fields that consist the identity of an individual change within a client codebase.
 * (Objects can have multiple changes).
 */
public class ChangeKey {
    private final ObjectKey objectKey;
    private final String changeName;

    public ChangeKey(ObjectKey objectKey, String changeName) {
        this.objectKey = Validate.notNull(objectKey);
        this.changeName = Validate.notNull(changeName);
    }

    public ObjectKey getObjectKey() {
        return objectKey;
    }

    public String getChangeName() {
        return changeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeKey)) {
            return false;
        }

        ChangeKey that = (ChangeKey) o;

        if (!objectKey.equals(that.objectKey)) {
            return false;
        }
        return changeName.equals(that.changeName);
    }

    @Override
    public int hashCode() {
        int result = objectKey.hashCode();
        result = 31 * result + changeName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ChangeKey{" +
                "objectKey=" + objectKey +
                ", changeName='" + changeName + '\'' +
                '}';
    }
}
