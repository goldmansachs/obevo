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
package com.gs.obevo.dbmetadata.impl;

import com.gs.obevo.dbmetadata.api.DaColumnReference;
import com.gs.obevo.dbmetadata.api.DaForeignKey;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;

public class DaForeignKeyImpl implements DaForeignKey {
    private final ForeignKey fk;
    private final SchemaStrategy schemaStrategy;

    public DaForeignKeyImpl(ForeignKey fk, SchemaStrategy schemaStrategy) {
        this.fk = Validate.notNull(fk);
        this.schemaStrategy = Validate.notNull(schemaStrategy);
    }

    @Override
    public String getName() {
        return fk.getName();
    }

    @Override
    public ImmutableList<DaColumnReference> getColumnReferences() {
        return ListAdapter.adapt(fk.getColumnReferences())
                .collect(new Function<ForeignKeyColumnReference, DaColumnReference>() {
                    @Override
                    public DaColumnReference valueOf(ForeignKeyColumnReference fk) {
                        return new DaColumnReferenceImpl(fk, schemaStrategy);
                    }
                })
                .toImmutable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaForeignKeyImpl)) {
            return false;
        }

        DaForeignKeyImpl that = (DaForeignKeyImpl) o;

        return fk.equals(that.fk);
    }

    @Override
    public int hashCode() {
        return fk.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("fk", fk)
                .toString();
    }
}
