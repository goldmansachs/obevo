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
import com.gs.obevo.dbmetadata.api.DaColumnDataType;
import com.gs.obevo.dbmetadata.api.DaTable;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import schemacrawler.schema.Column;

public class DaColumnImpl implements DaColumn {
    private final Column column;
    private final SchemaStrategy schemaStrategy;

    public DaColumnImpl(Column column, SchemaStrategy schemaStrategy) {
        this.column = Validate.notNull(column);
        this.schemaStrategy = Validate.notNull(schemaStrategy);
    }

    @Override
    public String getName() {
        return column.getName();
    }

    @Override
    public DaColumnDataType getColumnDataType() {
        return new DaColumnDataTypeImpl(column.getColumnDataType());
    }

    @Override
    public String getDefaultValue() {
        return column.getDefaultValue();
    }

    @Override
    public boolean isNullable() {
        return column.isNullable();
    }

    @Override
    public DaTable getParent() {
        return new DaTableImpl(column.getParent(), schemaStrategy);
    }

    @Override
    public String getWidth() {
        return column.getWidth();
    }

    @Override
    public int getDecimalDigits() {
        return column.getDecimalDigits();
    }

    @Override
    public int getSize() {
        return column.getSize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaColumnImpl)) {
            return false;
        }

        DaColumnImpl daColumn6 = (DaColumnImpl) o;

        return column.equals(daColumn6.column);
    }

    @Override
    public int hashCode() {
        return column.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("column", column)
                .toString();
    }
}

