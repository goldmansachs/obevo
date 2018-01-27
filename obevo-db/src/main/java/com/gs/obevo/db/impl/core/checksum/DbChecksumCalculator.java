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

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.dbmetadata.api.DaCatalog;
import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaTable;
import com.gs.obevo.dbmetadata.api.DaView;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Returns the checksum entries for the given {@link DaCatalog}. Many checksums can be returned from a single DaCatalog;
 * this class will recurse into the child objects (e.g. tables, views, ...) to pull the checksums out of those.
 */
class DbChecksumCalculator {
    public ImmutableCollection<ChecksumEntry> getChecksums(DaCatalog database) {
        MutableCollection<ChecksumEntry> checksums = Lists.mutable.empty();

        for (DaRoutine routine : database.getRoutines()) {
            checksums.withAll(getRoutineChecksums(routine));
        }
        for (DaTable tableOrView : database.getTables()) {
            checksums.withAll(getTableOrViewChecksums(tableOrView));
        }

        return checksums.toImmutable();
    }

    private ImmutableCollection<ChecksumEntry> getTableOrViewChecksums(DaTable tableOrView) {
        if (tableOrView instanceof DaView) {
            return getViewChecksums((DaView) tableOrView);
        } else {
            return getTableChecksums(tableOrView);
        }
    }

    private ImmutableCollection<ChecksumEntry> getTableChecksums(DaTable table) {
        MutableList<ChecksumEntry> checksums = Lists.mutable.empty();

        String pkContent = table.getPrimaryKey() != null ? table.getPrimaryKey().toString() : "";
        ChecksumEntry tableChecksum = ChecksumEntry.createFromText(table.getSchema().toPhysicalSchema(), ChangeType.TABLE_STR, table.getName(), "primaryKey", pkContent);
        checksums.add(tableChecksum);

        for (DaColumn column : table.getColumns()) {
            checksums.add(getColumnChecksum(table, column));
        }

        return checksums.toImmutable();
    }

    private ImmutableCollection<ChecksumEntry> getViewChecksums(DaView view) {
        MutableList<ChecksumEntry> checksums = Lists.mutable.empty();

        String tableContent = view.getDefinition();
        ChecksumEntry tableChecksum = ChecksumEntry.createFromText(view.getSchema().toPhysicalSchema(), ChangeType.VIEW_STR, view.getName(), null, tableContent);
        checksums.add(tableChecksum);
        return checksums.toImmutable();
    }

    private ChecksumEntry getColumnChecksum(DaTable table, DaColumn column) {
        String columnText = column.getColumnDataType().toString() + ":" + column.isNullable();
        return ChecksumEntry.createFromText(table.getSchema().toPhysicalSchema(), ChangeType.TABLE_STR, table.getName(), column.getName(), columnText);
    }

    private ImmutableCollection<ChecksumEntry> getRoutineChecksums(DaRoutine routine) {
        String contentToChecksum = routine.getDefinition();

        String objectType = routine.getRoutineType() == DaRoutineType.procedure ? ChangeType.SP_STR : ChangeType.FUNCTION_STR;

        return Lists.immutable.with(
                ChecksumEntry.createFromText(routine.getSchema().toPhysicalSchema(), objectType, routine.getName(), routine.getSpecificName(), contentToChecksum)
        );
    }
}
