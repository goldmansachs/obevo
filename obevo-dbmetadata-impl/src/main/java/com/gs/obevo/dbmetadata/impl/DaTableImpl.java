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
package com.gs.obevo.dbmetadata.impl;

import java.util.Map;

import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaForeignKey;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaNamedObject;
import com.gs.obevo.dbmetadata.api.DaPrimaryKey;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaTable;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schema.Column;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.Index;
import schemacrawler.schema.Table;

public class DaTableImpl implements DaTable {
    private final Table table;
    private final Map<String, ExtraIndexInfo> extraIndexInfoMap;
    private final SchemaStrategy schemaStrategy;

    public DaTableImpl(Table table, SchemaStrategy schemaStrategy) {
        this(table, schemaStrategy, Multimaps.immutable.list.<String, ExtraIndexInfo>empty());
    }

    public DaTableImpl(Table table, SchemaStrategy schemaStrategy, Multimap<String, ExtraIndexInfo> extraIndexes) {
        this.table = Validate.notNull(table);
        this.schemaStrategy = schemaStrategy;
        this.extraIndexInfoMap = extraIndexes.get(table.getName())
                .toMap(ExtraIndexInfo.TO_INDEX_NAME, Functions.<ExtraIndexInfo>getPassThru());
    }

    @Override
    public String getName() {
        return table.getName();
    }

    @Override
    public DaSchema getSchema() {
        return new DaSchemaImpl(table.getSchema(), schemaStrategy);
    }

    @Override
    public ImmutableList<DaColumn> getColumns() {
        return ListAdapter.adapt(table.getColumns()).collect(new Function<Column, DaColumn>() {
            @Override
            public DaColumn valueOf(Column object) {
                return new DaColumnImpl(object, schemaStrategy);
            }
        }).toImmutable();
    }

    @Override
    public boolean isView() {
        return table.getTableType().isView();
    }

    @Override
    public DaColumn getColumn(String columnName) {
        return getColumns().detect(Predicates.attributeEqual(DaNamedObject.TO_NAME, columnName));
    }

    @Override
    public DaPrimaryKey getPrimaryKey() {
        return table.getPrimaryKey() != null ? new DaPrimaryKeyImpl(table.getPrimaryKey(), schemaStrategy) : null;
    }

    @Override
    public ImmutableCollection<DaIndex> getIndices() {
        return CollectionAdapter.adapt(table.getIndexes())
                .collect(new Function<Index, DaIndex>() {
                    @Override
                    public DaIndex valueOf(Index object) {
                        return new DaIndexImpl(object, schemaStrategy, extraIndexInfoMap.get(object.getName()));
                    }
                })
                .reject(new Predicate<DaIndex>() {
                    @Override
                    public boolean accept(DaIndex index) {
                        ExtraIndexInfo extraIndexInfo = extraIndexInfoMap.get(index.getName());
                        return extraIndexInfo != null && extraIndexInfo.isConstraint();
                    }
                })
                .toImmutable();
    }

    @Override
    public ImmutableCollection<DaForeignKey> getImportedForeignKeys() {
        return CollectionAdapter.adapt(table.getImportedForeignKeys())
                .collect(new Function<ForeignKey, DaForeignKey>() {
                    @Override
                    public DaForeignKey valueOf(ForeignKey object) {
                        return new DaForeignKeyImpl(object, schemaStrategy);
                    }
                }).toImmutable();
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) table.getAttribute(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaTableImpl)) {
            return false;
        }

        DaTableImpl daTable6 = (DaTableImpl) o;

        return table.equals(daTable6.table);
    }

    @Override
    public int hashCode() {
        return table.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("table", table)
                .toString();
    }
}
