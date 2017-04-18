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

import com.gs.obevo.dbmetadata.api.DaColumnDataType;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import schemacrawler.schema.ColumnDataType;

public class DaColumnDataTypeImpl implements DaColumnDataType {
    private final ColumnDataType columnDataType;

    public DaColumnDataTypeImpl(ColumnDataType columnDataType) {
        this.columnDataType = Validate.notNull(columnDataType);
    }

    @Override
    public String getTypeClassName() {
        return columnDataType.getTypeMappedClass().getName();
    }

    @Override
    public String getName() {
        return columnDataType.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaColumnDataTypeImpl)) {
            return false;
        }

        DaColumnDataTypeImpl that = (DaColumnDataTypeImpl) o;

        return columnDataType.equals(that.columnDataType);
    }

    @Override
    public int hashCode() {
        return columnDataType.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("columnDataType", columnDataType)
                .toString();
    }
}
