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

import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;

public class DaTableImpl implements DaTable {
    private final DaSchema schema;
    private final String name;
    private ImmutableList<DaColumn> columns;
    private DaPrimaryKey primaryKey;
    private MutableMap<String, Object> attributes = Maps.mutable.empty();
    private ImmutableCollection<DaIndex> indices = Lists.immutable.empty();
    private boolean view;

    public DaTableImpl(DaSchema schema, String name) {
        this.schema = schema;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ImmutableList<DaColumn> getColumns() {
        return columns;
    }

    public void setColumns(ImmutableList<DaColumn> columns) {
        this.columns = columns;
    }

    @Override
    public boolean isView() {
        return view;
    }

    public void setView(boolean view) {
        this.view = view;
    }

    @Override
    public DaSchema getSchema() {
        return schema;
    }

    @Override
    public DaColumn getColumn(String columnName) {
        return null;
    }

    @Override
    public DaPrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(DaPrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
    }

    @Override
    public ImmutableCollection<DaIndex> getIndices() {
        return indices;
    }

    public void setIndices(ImmutableCollection<DaIndex> indices) {
        this.indices = indices;
    }

    @Override
    public ImmutableCollection<DaForeignKey> getImportedForeignKeys() {
        return null;
    }

    @Override
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }
}
