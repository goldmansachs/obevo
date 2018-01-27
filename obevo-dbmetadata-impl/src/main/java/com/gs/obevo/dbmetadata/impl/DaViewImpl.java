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
import com.gs.obevo.dbmetadata.api.DaForeignKey;
import com.gs.obevo.dbmetadata.api.DaIndex;
import com.gs.obevo.dbmetadata.api.DaPrimaryKey;
import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaView;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import schemacrawler.schema.View;

public class DaViewImpl implements DaView {
    private final View view;
    private final ExtraRerunnableInfo extraRerunnableInfo;
    private final SchemaStrategy schemaStrategy;

    public DaViewImpl(View view, SchemaStrategy schemaStrategy, ExtraRerunnableInfo extraRerunnableInfo) {
        this.view = Validate.notNull(view);
        this.schemaStrategy = Validate.notNull(schemaStrategy);
        this.extraRerunnableInfo = extraRerunnableInfo;
    }

    @Override
    public String getName() {
        return view.getName();
    }

    @Override
    public String getDefinition() {
        if (extraRerunnableInfo != null && extraRerunnableInfo.getDefinition() != null) {
            return extraRerunnableInfo.getDefinition();
        }
        return view.getDefinition();
    }

    @Override
    public ImmutableList<DaColumn> getColumns() {
        return Lists.immutable.empty();
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public DaSchema getSchema() {
        return new DaSchemaImpl(view.getSchema(), schemaStrategy);
    }

    @Override
    public DaColumn getColumn(String columnName) {
        return null;
    }

    @Override
    public DaPrimaryKey getPrimaryKey() {
        return null;
    }

    @Override
    public ImmutableCollection<DaIndex> getIndices() {
        return Lists.immutable.empty();
    }

    @Override
    public ImmutableCollection<DaForeignKey> getImportedForeignKeys() {
        return Lists.immutable.empty();
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) view.getAttribute(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaViewImpl)) {
            return false;
        }

        DaViewImpl daView6 = (DaViewImpl) o;

        return view.equals(daView6.view);
    }

    @Override
    public int hashCode() {
        return view.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("view", view)
                .toString();
    }
}
