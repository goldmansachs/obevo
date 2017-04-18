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
package com.gs.catodeployany.input.aggregate.field;

public class MinField extends AbstractAggregateField<Number, Double> {

    public MinField(String name) {
        this(name, name);
    }

    public MinField(String name, String baseField) {
        super(name, baseField, false, Number.class);
    }

    @Override
    protected Double getInitialValue() {
        return Double.MAX_VALUE;
    }

    @Override
    protected Double aggregate(Number newValue, Double aggValue) {
        return Math.min(aggValue, newValue.doubleValue());
    }
}
