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
package com.gs.obevocomparer.input.aggregate.field;

import java.util.Collection;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.aggregate.AggregateField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAggregateField<T, AT> implements AggregateField {

    private final String name;
    private final String baseField;
    private final Class<T> clazz;
    private final boolean aggregateNulls;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAggregateField.class);

    AbstractAggregateField(String name, String baseField, boolean aggregateNulls, Class<T> clazz) {
        this.name = name;
        this.baseField = baseField;
        this.aggregateNulls = aggregateNulls;
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String getBaseField() {
        return this.baseField;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AT getValue(Collection<CatoDataObject> objs) {
        AT aggValue = this.getInitialValue();
        Object val;

        for (CatoDataObject obj : objs) {
            val = obj.getValue(this.baseField);

            if (val != null && !this.clazz.isInstance(val)) {
                LOG.warn("Value {} skipped - is not of required type {} for record {}",
                        val, this.clazz.getSimpleName(), obj);
                continue;
            }

            if (this.aggregateNulls || val != null) {
                aggValue = this.aggregate((T) val, aggValue);
            }
        }

        return aggValue;
    }

    protected abstract AT getInitialValue();

    protected abstract AT aggregate(T newValue, AT aggValue);
}
