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
package com.gs.obevo.db.impl.core.changetypes

import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.dbmetadata.api.DaTable
import org.eclipse.collections.api.list.ImmutableList

data class StaticDataChangeRows(
        val schema: PhysicalSchema,
        val table: DaTable,
        val insertRows: ImmutableList<StaticDataInsertRow>,
        val updateRows: ImmutableList<StaticDataUpdateRow>,
        val deleteRows: ImmutableList<StaticDataDeleteRow>
)
