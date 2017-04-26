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
package com.gs.obevo.db.impl.core.changetypes;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaTable;
import org.eclipse.collections.api.list.ImmutableList;

public class StaticDataChangeRows {
    private final PhysicalSchema schema;
    private final DaTable table;
    private final ImmutableList<StaticDataInsertRow> insertRows;
    private final ImmutableList<StaticDataUpdateRow> updateRows;
    private final ImmutableList<StaticDataDeleteRow> deleteRows;

    public StaticDataChangeRows(PhysicalSchema schema, DaTable table, ImmutableList<StaticDataInsertRow> insertRows,
            ImmutableList<StaticDataUpdateRow> updateRows, ImmutableList<StaticDataDeleteRow> deleteRows) {
        this.schema = schema;
        this.table = table;
        this.insertRows = insertRows;
        this.updateRows = updateRows;
        this.deleteRows = deleteRows;
    }

    public PhysicalSchema getSchema() {
        return this.schema;
    }

    public DaTable getTable() {
        return this.table;
    }

    public ImmutableList<StaticDataInsertRow> getInsertRows() {
        return this.insertRows;
    }

    public ImmutableList<StaticDataUpdateRow> getUpdateRows() {
        return this.updateRows;
    }

    public ImmutableList<StaticDataDeleteRow> getDeleteRows() {
        return this.deleteRows;
    }
}
