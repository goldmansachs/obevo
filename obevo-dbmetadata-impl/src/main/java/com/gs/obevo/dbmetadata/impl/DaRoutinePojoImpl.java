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

import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaSchema;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class DaRoutinePojoImpl implements DaRoutine {
    private String name;
    private DaSchema schema;
    private DaRoutineType routineType;
    private String specificName;
    private String definition;

    public DaRoutinePojoImpl(String name, DaSchema schema, DaRoutineType routineType, String specificName, String definition) {
        this.name = name;
        this.schema = schema;
        this.routineType = routineType;
        this.specificName = specificName;
        this.definition = definition;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public DaSchema getSchema() {
        return schema;
    }

    public void setSchema(DaSchema schema) {
        this.schema = schema;
    }

    @Override
    public DaRoutineType getRoutineType() {
        return routineType;
    }

    public void setRoutineType(DaRoutineType routineType) {
        this.routineType = routineType;
    }

    @Override
    public String getSpecificName() {
        return specificName;
    }

    public void setSpecificName(String specificName) {
        this.specificName = specificName;
    }

    @Override
    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("name", name)
                .append("schema", schema)
                .append("routineType", routineType)
                .append("specificName", specificName)
                .append("definition", definition)
                .toString();
    }
}
