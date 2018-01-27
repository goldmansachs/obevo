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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;

public interface DaTable extends DaNamedObject {
    Predicate<DaTable> IS_VIEW = new Predicate<DaTable>() {
        @Override
        public boolean accept(DaTable each) {
            return each.isView();
        }
    };
    Function<DaTable, ImmutableCollection<DaIndex>> TO_INDICES = new Function<DaTable, ImmutableCollection<DaIndex>>() {
        @Override
        public ImmutableCollection<DaIndex> valueOf(DaTable object) {
            return object.getIndices();
        }
    };
    Function<DaTable, ImmutableList<DaColumn>> TO_COLUMNS = new Function<DaTable, ImmutableList<DaColumn>>() {
        @Override
        public ImmutableList<DaColumn> valueOf(DaTable object) {
            return object.getColumns();
        }
    };
    Function<DaTable, DaPrimaryKey> TO_PRIMARY_KEY = new Function<DaTable, DaPrimaryKey>() {
        @Override
        public DaPrimaryKey valueOf(DaTable object) {
            return object.getPrimaryKey();
        }
    };

    ImmutableList<DaColumn> getColumns();

    boolean isView();

    DaSchema getSchema();

    DaColumn getColumn(String columnName);

    DaPrimaryKey getPrimaryKey();

    ImmutableCollection<DaIndex> getIndices();

    ImmutableCollection<DaForeignKey> getImportedForeignKeys();

    <T> T getAttribute(String name);
}
