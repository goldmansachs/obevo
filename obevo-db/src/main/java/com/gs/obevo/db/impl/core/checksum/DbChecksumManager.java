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

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * Coordinator of the checksum calculation logic for a particular environment.
 *
 * Exposes two primary functions:
 * 1) Calculate the checksum differences between the target db and the persisted data and return those breaks for
 * clients to do as they wish.
 * 2) Provide a standard way to apply those breaks back to the audit database.
 */
public interface DbChecksumManager {
    /**
     * Calculates the checksum differences between the actual database and the checksum audit table.
     *
     * @param checksumEntryInclusionPredicate Filter for only including certain objects from the actual database; this
     * is to handle cases of objects that are explicitly not managed in this
     * code base and thus should not add to the noise for checksums
     */
    ImmutableCollection<ChecksumBreak> determineChecksumDifferences(Predicate<? super ChecksumEntry> checksumEntryInclusionPredicate);

    /**
     * Calculates the checksum differences and then applies the changes to the target DB.
     *
     * @param checksumEntryInclusionPredicate See the description in {@link #determineChecksumDifferences(Predicate)}
     */
    void applyChecksumDiffs(Predicate<? super ChecksumEntry> checksumEntryInclusionPredicate);

    boolean isInitialized();

    void initialize();

    String getChecksumContainerName();
}
