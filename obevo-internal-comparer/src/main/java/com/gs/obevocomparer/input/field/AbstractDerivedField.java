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
package com.gs.obevocomparer.input.field;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.CatoDerivedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDerivedField<T> implements CatoDerivedField {

    private final String sourceField;
    private final String targetField;
    private final Class<T> clazz;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractDerivedField.class);

    protected AbstractDerivedField(String field, Class<T> clazz) {
        this(field, field, clazz);
    }

    protected AbstractDerivedField(String sourceField, String targetField, Class<T> clazz) {
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.clazz = clazz;
    }

    public final String getName() {
        return this.targetField;
    }

    public final Object getValue(CatoDataObject obj) {
        Object val = obj.getValue(this.sourceField);

        if (val == null) {
            return null;
        }

        if (this.clazz.isInstance(val)) {
            return this.getValue(this.clazz.cast(val));
        }

        return val;
    }

    protected abstract Object getValue(T sourceValue);
}
