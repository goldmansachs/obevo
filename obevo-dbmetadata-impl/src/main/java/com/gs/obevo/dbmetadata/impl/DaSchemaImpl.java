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

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.dbmetadata.api.DaSchema;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import schemacrawler.schema.Schema;

public class DaSchemaImpl implements DaSchema {
    private final Schema schema;
    private final SchemaStrategy schemaStrategy;

    public DaSchemaImpl(Schema schema, SchemaStrategy schemaStrategy) {
        this.schema = Validate.notNull(schema);
        this.schemaStrategy = schemaStrategy;
    }

    @Override
    public String getName() {
        return schemaStrategy.getSchemaName(schema);
    }

    @Override
    public String getSubschemaName() {
        return schemaStrategy.getSubschemaName(schema);
    }

    @Override
    public PhysicalSchema toPhysicalSchema() {
        return new PhysicalSchema(getName(), getSubschemaName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaSchemaImpl)) {
            return false;
        }

        DaSchemaImpl daSchema6 = (DaSchemaImpl) o;

        return schema.equals(daSchema6.schema);
    }

    @Override
    public int hashCode() {
        return schema.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("schema", schema)
                .toString();
    }
}
