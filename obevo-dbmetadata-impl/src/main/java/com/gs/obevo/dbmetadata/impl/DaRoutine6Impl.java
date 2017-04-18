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

import com.gs.obevo.dbmetadata.api.DaRoutine;
import com.gs.obevo.dbmetadata.api.DaRoutineType;
import com.gs.obevo.dbmetadata.api.DaSchema;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import schemacrawler.schema.Routine;

public class DaRoutine6Impl implements DaRoutine {
    private final Routine routine;
    private final DaRoutineType routineOverrideValue;
    private final SchemaStrategy schemaStrategy;

    public DaRoutine6Impl(Routine routine, SchemaStrategy schemaStrategy, DaRoutineType routineOverrideValue) {
        this.routine = Validate.notNull(routine);
        this.schemaStrategy = Validate.notNull(schemaStrategy);
        this.routineOverrideValue = routineOverrideValue;
    }

    @Override
    public String getSpecificName() {
        return routine.getSpecificName();
    }

    @Override
    public String getDefinition() {
        return routine.getDefinition();
    }

    @Override
    public DaRoutineType getRoutineType() {
        if (routineOverrideValue != null) {
            return routineOverrideValue;
        }

        switch (routine.getRoutineType()) {
        case function:
            return DaRoutineType.function;
        case procedure:
            return DaRoutineType.procedure;
        default:
            return DaRoutineType.unknown;
        }
    }

    @Override
    public DaSchema getSchema() {
        return new DaSchemaImpl(routine.getSchema(), schemaStrategy);
    }

    @Override
    public String getName() {
        return routine.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DaRoutine6Impl)) {
            return false;
        }

        DaRoutine6Impl that = (DaRoutine6Impl) o;

        return routine.equals(that.routine);
    }

    @Override
    public int hashCode() {
        return routine.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("routine", routine)
                .toString();
    }
}
