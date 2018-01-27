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
package com.gs.obevo.db.impl.core.checksum;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.util.DAStringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;

/**
 * Represents the checksum value for a particular object defined in your target environment. This object can represent
 * the checksum both of the object definition itself and the persisted checksum value.
 */
public class ChecksumEntry {
    private final PhysicalSchema physicalSchema;
    private final String objectType;
    private final String name1;
    private final String name2;
    private final String checksum;

    /**
     * Static-constructor for the given parameters, with adding the convenience of getting the hash checksum from the
     * given text input in a standard manner.
     */
    public static ChecksumEntry createFromText(PhysicalSchema physicalSchema, String objectType, String name1, String name2, String checksum) {
        final String data = DAStringUtil.normalizeWhiteSpaceFromString(checksum);
        final String checksumData = StringUtils.isBlank(data) ? "" : DigestUtils.md5Hex(data);
        return new ChecksumEntry(physicalSchema, objectType, name1, name2, checksumData);
    }

    /**
     * Static-constructor for the given parameters, while taking in the checksum directly; this should be used when
     * creating these object inputs from the persistence store (i.e. where checksum is already calculated).
     */
    public static ChecksumEntry createFromPersistence(PhysicalSchema physicalSchema, String objectType, String name1, String name2, String checksum) {
        return new ChecksumEntry(physicalSchema, objectType, name1, name2, checksum);
    }

    private ChecksumEntry(PhysicalSchema physicalSchema, String objectType, String name1, String name2, String checksum) {
        this.physicalSchema = physicalSchema;
        this.objectType = objectType;
        this.name1 = name1;
        this.name2 = name2;
        this.checksum = checksum;
    }

    public static final Function<ChecksumEntry, String> TO_KEY = new Function<ChecksumEntry, String>() {
        @Override
        public String valueOf(ChecksumEntry object) {
            return object.getKey();
        }
    };

    public String getKey() {
        return physicalSchema.getPhysicalName() + ":" + objectType + ":" + name1 + ":" + name2;
    }

    public PhysicalSchema getPhysicalSchema() {
        return physicalSchema;
    }

    public static final Function<ChecksumEntry, String> TO_OBJECT_TYPE = new Function<ChecksumEntry, String>() {
        @Override
        public String valueOf(ChecksumEntry object) {
            return object.getObjectType();
        }
    };

    /**
     * The object type that the checksum is taken on.
     */
    public String getObjectType() {
        return objectType;
    }

    public static final Function<ChecksumEntry, String> TO_NAME1 = new Function<ChecksumEntry, String>() {
        @Override
        public String valueOf(ChecksumEntry object) {
            return object.getName1();
        }
    };

    /**
     * The primary name of the object.
     */
    public String getName1() {
        return name1;
    }

    /**
     * A secondary name of the object, i.e. if we want to take a checksum on a specific feature of an object, like a
     * column or primary key
     */
    public String getName2() {
        return name2;
    }

    public String getChecksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return "ChecksumEntry{" +
                "physicalSchema=" + physicalSchema +
                ", objectType='" + objectType + '\'' +
                ", name1='" + name1 + '\'' +
                ", name2='" + name2 + '\'' +
                ", checksum='" + checksum + '\'' +
                '}';
    }
}
