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
import schemacrawler.schema.Index;
import schemacrawler.schema.IndexColumn;
import schemacrawler.schema.IndexType;

public class DaIndexImpl implements DaIndex {
    private final Index index;
    private final ExtraIndexInfo extraIndexInfo;
    private final SchemaStrategy schemaStrategy;

    public DaIndexImpl(Index index, SchemaStrategy schemaStrategy, ExtraIndexInfo extraIndexInfo) {
        this.index = Validate.notNull(index);
        this.schemaStrategy = Validate.notNull(schemaStrategy);
        this.extraIndexInfo = extraIndexInfo;
    }

    @Override
    public boolean isUnique() {
        return index.isUnique();
    }

    @Override
    public ImmutableList<DaColumn> getColumns() {
        try {
            return ListAdapter.adapt(index.getColumns())
                    .collect(new Function<IndexColumn, DaColumn>() {
                        @Override
                        public DaColumn valueOf(IndexColumn object) {
                            return new DaColumnImpl(object, schemaStrategy);
                        }
                    })
                    .toImmutable();
        } catch (NullPointerException exc) {
            throw exc;
        }
    }

    @Override
    public DaIndexType getIndexType() {
        if ((this.extraIndexInfo != null && this.extraIndexInfo.isClustered()) || this.index.getIndexType() == IndexType
                .clustered) {
            return DaIndexType.CLUSTERED;
        } else {
            switch (this.index.getIndexType()) {
            case clustered:
                return DaIndexType.CLUSTERED;
            default:
                return DaIndexType.OTHER;
            }
        }
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
}
