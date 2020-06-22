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

import java.util.List;

import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaIndexType;
import com.gs.obevo.dbmetadata.api.DaTable;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schema.Column;
import schemacrawler.schema.DependantObject;
import schemacrawler.schema.Index;
import schemacrawler.schema.PrimaryKey;
import schemacrawler.schema.Table;

import static schemacrawler.schema.IndexType.clustered;

public class DaIndexImpl implements DaIndex {
    private final DependantObject<Table> index;
    private final ImmutableList<DaColumn> columns;
    private final boolean unique;
    private final DaIndexType indexType;
    private final SchemaStrategy schemaStrategy;

    public DaIndexImpl(Index index, SchemaStrategy schemaStrategy, ExtraIndexInfo extraIndexInfo) {
        this(index, index.getColumns(), schemaStrategy, index.isUnique(), getIndexType(index, extraIndexInfo));
    }

    public DaIndexImpl(PrimaryKey index, SchemaStrategy schemaStrategy, ExtraIndexInfo extraIndexInfo) {
        this(index, index.getColumns(), schemaStrategy, true, DaIndexType.OTHER);
    }

    private DaIndexImpl(DependantObject<Table> index, List<? extends Column> columns, SchemaStrategy schemaStrategy, boolean unique, DaIndexType indexType) {
        this.index = Validate.notNull(index);
        Validate.notNull(schemaStrategy);
        this.columns = ListAdapter.adapt(columns)
                .collect((Function<Column, DaColumn>) object -> new DaColumnImpl(object, schemaStrategy))
                .toImmutable();
        this.schemaStrategy = Validate.notNull(schemaStrategy);
        this.unique = unique;
        this.indexType = indexType;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    @Override
    public ImmutableList<DaColumn> getColumns() {
        return columns;
    }

    @Override
    public DaIndexType getIndexType() {
        return indexType;
    }

    @Override
    public DaTable getParent() {
        return new DaTableImpl(index.getParent(), schemaStrategy);
    }

    @Override
    public String getName() {
        return index.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaIndexImpl)) {
            return false;
        }

        DaIndexImpl daIndex6 = (DaIndexImpl) o;

        return index.equals(daIndex6.index);
    }

    @Override
    public int hashCode() {
        return index.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("index", index)
                .toString();
    }

    private static DaIndexType getIndexType(Index index, ExtraIndexInfo extraIndexInfo) {
        if ((extraIndexInfo != null && extraIndexInfo.isClustered()) || index.getIndexType() == clustered) {
            return DaIndexType.CLUSTERED;
        } else {
            switch (index.getIndexType()) {
            case clustered:
                return DaIndexType.CLUSTERED;
            default:
                return DaIndexType.OTHER;
            }
        }
    }
}
