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
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaSchemaInfoLevel;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.lang.Validate;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;

public class DbChecksumManagerImpl implements DbChecksumManager {
    private final DbMetadataManager dbMetadataManager;
    private final DbChecksumDao dbChecksumDao;
    private final ImmutableSet<PhysicalSchema> physicalSchemas;
    private final DbChecksumCalculator checksumCalculator = new DbChecksumCalculator();

    public DbChecksumManagerImpl(DbMetadataManager dbMetadataManager, DbChecksumDao dbChecksumDao, ImmutableSet<PhysicalSchema> physicalSchemas) {
        this.dbMetadataManager = dbMetadataManager;
        this.dbChecksumDao = dbChecksumDao;
        this.physicalSchemas = physicalSchemas;
    }

    @Override
    public ImmutableCollection<ChecksumBreak> determineChecksumDifferences(Predicate<? super ChecksumEntry> checksumEntryInclusionPredicate) {
        MutableList<ChecksumBreak> checksumBreaks = Lists.mutable.empty();

        for (PhysicalSchema physicalSchema : physicalSchemas) {
            DaCatalog catalog = dbMetadataManager.getDatabase(physicalSchema, new DaSchemaInfoLevel().setMaximum(), true, true);

            ImmutableCollection<ChecksumEntry> newChecksums = checksumCalculator.getChecksums(catalog)
                    .select(checksumEntryInclusionPredicate);
            MapIterable<String, ChecksumEntry> newChecksumMap = newChecksums.groupByUniqueKey(ChecksumEntry::getKey);

            ImmutableCollection<ChecksumEntry> existingChecksums = dbChecksumDao.getPersistedEntries(physicalSchema);
            MapIterable<String, ChecksumEntry> existingChecksumMap = existingChecksums.groupByUniqueKey(ChecksumEntry::getKey);

            SetIterable<String> allChecksumKeys = newChecksumMap.keysView().toSet().withAll(existingChecksumMap.keysView());

            for (String checksumKey : allChecksumKeys) {
                ChecksumEntry newChecksum = newChecksumMap.get(checksumKey);
                ChecksumEntry existingChecksum = existingChecksumMap.get(checksumKey);

                if (newChecksum == null && existingChecksum != null) {
                    checksumBreaks.add(new ChecksumBreak(existingChecksum.getKey(), existingChecksum, newChecksum, ChecksumBreakType.IN_AUDIT_BUT_NOT_DB, checksumEntryInclusionPredicate.accept(existingChecksum)));
                } else if (newChecksum != null && existingChecksum == null) {
                    checksumBreaks.add(new ChecksumBreak(newChecksum.getKey(), existingChecksum, newChecksum, ChecksumBreakType.IN_DB_BUT_NOT_AUDIT, false));
                } else if (!newChecksum.getChecksum().equalsIgnoreCase(existingChecksum.getChecksum())) {
                    checksumBreaks.add(new ChecksumBreak(newChecksum.getKey(), existingChecksum, newChecksum, ChecksumBreakType.DIFFERENCE, false));
                }
            }
        }

        return checksumBreaks.toImmutable();
    }

    @Override
    public void applyChecksumDiffs(Predicate<? super ChecksumEntry> checksumEntryInclusionPredicate) {
        ImmutableCollection<ChecksumBreak> checksumBreaks = determineChecksumDifferences(checksumEntryInclusionPredicate);
        applyChecksumDiffs(checksumBreaks);
    }

    private void applyChecksumDiffs(ImmutableCollection<ChecksumBreak> checksumBreaks) {
        for (ChecksumBreak checksumBreak : checksumBreaks) {
            if (checksumBreak.getExistingChecksum() == null) {
                Validate.notNull(checksumBreak.getNewChecksum());
                dbChecksumDao.persistEntry(checksumBreak.getNewChecksum());
            } else if (checksumBreak.getNewChecksum() == null) {
                Validate.notNull(checksumBreak.getExistingChecksum());
                dbChecksumDao.deleteEntry(checksumBreak.getExistingChecksum());
            } else {
                dbChecksumDao.persistEntry(checksumBreak.getNewChecksum());
            }
        }
    }

    @Override
    public boolean isInitialized() {
        return dbChecksumDao.isInitialized();
    }

    @Override
    public void initialize() {
        dbChecksumDao.initialize();
    }

    @Override
    public String getChecksumContainerName() {
        return dbChecksumDao.getChecksumContainerName();
    }
}
