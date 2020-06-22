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
package com.gs.obevo.dbmetadata.api;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;

public interface DaIndex extends DaNamedObject {
    Function<DaIndex, DaIndexType> TO_INDEX_TYPE = DaIndex::getIndexType;
    Function<DaIndex, String> TO_COLUMN_STRING = object -> object.getColumns().collect(TO_NAME).toSortedList().makeString(",");
    Function<DaIndex, Boolean> TO_UNIQUE = DaIndex::isUnique;
    Function<DaIndex, ImmutableList<DaColumn>> TO_COLUMNS = DaIndex::getColumns;
    Function<DaIndex, Boolean> TO_PK = object -> object instanceof DaPrimaryKey;

    boolean isUnique();

    ImmutableList<DaColumn> getColumns();

    DaIndexType getIndexType();

    DaTable getParent();
}
