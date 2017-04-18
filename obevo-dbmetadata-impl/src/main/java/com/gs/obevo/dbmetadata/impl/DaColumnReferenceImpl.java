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

import com.gs.obevo.dbmetadata.api.DaColumn;
import com.gs.obevo.dbmetadata.api.DaColumnReference;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import schemacrawler.schema.ForeignKeyColumnReference;

public class DaColumnReferenceImpl implements DaColumnReference {
    private final ForeignKeyColumnReference fkColumnReference;
    private final SchemaStrategy schemaStrategy;

    public DaColumnReferenceImpl(ForeignKeyColumnReference fkColumnReference, SchemaStrategy schemaStrategy) {
        this.fkColumnReference = Validate.notNull(fkColumnReference);
        this.schemaStrategy = Validate.notNull(schemaStrategy);
    }

    @Override
    public DaColumn getForeignKeyColumn() {
        return new DaColumnImpl(fkColumnReference.getForeignKeyColumn(), schemaStrategy);
    }

    @Override
    public DaColumn getPrimaryKeyColumn() {
        return new DaColumnImpl(fkColumnReference.getPrimaryKeyColumn(), schemaStrategy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaColumnReferenceImpl)) {
            return false;
        }

        DaColumnReferenceImpl that = (DaColumnReferenceImpl) o;

        return fkColumnReference.equals(that.fkColumnReference);
    }

    @Override
    public int hashCode() {
        return fkColumnReference.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("fkColumnReference", fkColumnReference)
                .toString();
    }
}
