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
package com.gs.obevo.db.impl.core.checksum;

import org.eclipse.collections.api.block.predicate.Predicate;

public class ChecksumBreak {
    private final String key;
    private final ChecksumBreakType checksumBreakType;
    private final ChecksumEntry existingChecksum;
    private final ChecksumEntry newChecksum;
    private final boolean expectedBreak;

    public ChecksumBreak(String key, ChecksumEntry existingChecksum, ChecksumEntry newChecksum, ChecksumBreakType checksumBreakType, boolean expectedBreak) {
        this.key = key;
        this.existingChecksum = existingChecksum;
        this.newChecksum = newChecksum;
        this.checksumBreakType = checksumBreakType;
        this.expectedBreak = expectedBreak;
    }

    public String getKey() {
        return key;
    }

    public ChecksumEntry getExistingChecksum() {
        return existingChecksum;
    }

    public ChecksumEntry getNewChecksum() {
        return newChecksum;
    }

    public ChecksumBreakType getChecksumBreakType() {
        return checksumBreakType;
    }

    public static final Predicate<ChecksumBreak> IS_EXPECTED_BREAK = new Predicate<ChecksumBreak>() {
        @Override
        public boolean accept(ChecksumBreak each) {
            return each.isExpectedBreak();
        }
    };

    public boolean isExpectedBreak() {
        return expectedBreak;
    }

    public String toDisplayString() {
        return "Object " + key + " " + checksumBreakType.getDisplayStringSuffix();
    }

    @Override
    public String toString() {
        return "ChecksumBreak{" +
                "key='" + key + '\'' +
                ", checksumBreakType=" + checksumBreakType +
                ", existingChecksum=" + existingChecksum +
                ", newChecksum=" + newChecksum +
                ", expectedBreak=" + expectedBreak +
                '}';
    }
}
