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
package com.gs.obevo.dbmetadata.deepcompare;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;

public class ObjectCompareBreak extends CompareBreak {
    public enum ObjectCompareBreakSide {
        LEFT,
        RIGHT,
    }

    private final Object object;
    private final ObjectCompareBreakSide objectCompareBreakSide;

    public ObjectCompareBreak(Class clazz, Object object, ObjectCompareBreakSide objectCompareBreakSide) {
        super(clazz);
        this.object = object;
        this.objectCompareBreakSide = objectCompareBreakSide;
    }

    @Override
    public String getCompareSubject() {
        return this.object != null ? this.object.toString() : "";
    }

    public Object getObject() {
        return this.object;
    }

    public static Function<Object, ObjectCompareBreak> createObjectCompareBreak(final Class clazz,
            final ObjectCompareBreakSide objectCompareBreakSide) {
        return new Function<Object, ObjectCompareBreak>() {
            @Override
            public ObjectCompareBreak valueOf(Object input) {
                return new ObjectCompareBreak(clazz, input, objectCompareBreakSide);
            }
        };
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("objectCompareBreakSide", objectCompareBreakSide)
                .append("compareSubject", getCompareSubject())
                .toString();
    }
}
