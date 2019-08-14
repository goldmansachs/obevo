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
package com.gs.obevo.apps.reveng;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RevengPatternOutput {
    private final RevengPattern revengPattern;
    private final String primaryName;
    private final String secondaryName;
    private final String schema;
    private final String subSchema;
    private final String revisedLine;

    RevengPatternOutput(RevengPattern revengPattern, String primaryName, String secondaryName, String schema, String subSchema, String revisedLine) {
        this.revengPattern = revengPattern;
        this.primaryName = primaryName;
        this.secondaryName = secondaryName;
        this.schema = schema;
        this.subSchema = subSchema;
        this.revisedLine = revisedLine;
    }

    RevengPattern getRevengPattern() {
        return revengPattern;
    }

    public String getPrimaryName() {
        return primaryName;
    }

    String getSecondaryName() {
        return secondaryName;
    }

    String getSchema() {
        return schema;
    }

    String getSubSchema() {
        return subSchema;
    }

    public String getRevisedLine() {
        return revisedLine;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("schema", schema)
                .append("subSchema", subSchema)
                .append("primaryName", primaryName)
                .append("secondaryName", secondaryName)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RevengPatternOutput that = (RevengPatternOutput) o;
        return Objects.equals(primaryName, that.primaryName) &&
                Objects.equals(secondaryName, that.secondaryName) &&
                Objects.equals(schema, that.schema) &&
                Objects.equals(subSchema, that.subSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryName, secondaryName, schema, subSchema);
    }
}
