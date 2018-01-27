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
import org.eclipse.collections.api.collection.ImmutableCollection;

/**
 * DAO for retrieving and storing the {@link ChecksumEntry} list for a given schema.
 */
public interface DbChecksumDao {
    // keeping the ARTIFACT name in the prefix to be consistent w/ the pre-existing ARTIFACTDEPLOYMENT table
    String SCHEMA_CHECKSUM_TABLE_NAME = "ARTIFACTDBCHECKSUM";

    ImmutableCollection<ChecksumEntry> getPersistedEntries(PhysicalSchema physicalSchema);

    void persistEntry(ChecksumEntry entry);

    void deleteEntry(ChecksumEntry entry);

    boolean isInitialized();

    void initialize();

    String getChecksumContainerName();
}
