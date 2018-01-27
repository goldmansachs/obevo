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
package com.gs.obevo.dbmetadata.deepcompare;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;

public class FieldCompareBreak extends CompareBreak {
    private final Object key;
    private final Object left;
    private final Object right;
    private final String fieldName;
    private final Object leftVal;
    private final Object rightVal;

    public static final Function<FieldCompareBreak, String> TO_FIELD_NAME = new Function<FieldCompareBreak, String>() {
        @Override
        public String valueOf(FieldCompareBreak object) {
            return object.getFieldName();
        }
    };

    public static final Function<FieldCompareBreak, Object> TO_KEY = new Function<FieldCompareBreak, Object>() {
        @Override
        public Object valueOf(FieldCompareBreak object) {
            return object.getKey();
        }
    };

    public FieldCompareBreak(Class clazz, Object key, Object left, Object right, String fieldName, Object leftVal,
            Object rightVal) {
        super(clazz);
        this.key = key;
        this.left = left;
        this.right = right;
        this.fieldName = fieldName;
        this.leftVal = leftVal;
        this.rightVal = rightVal;
    }

    @Override
    public String getCompareSubject() {
        return this.key != null ? this.key.toString() : "";
    }

    private Object getKey() {
        return this.key;
    }

    public Object getLeft() {
        return this.left;
    }

    public Object getRight() {
        return this.right;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public Object getLeftVal() {
        return this.leftVal;
    }

    public Object getRightVal() {
        return this.rightVal;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("compareSubject", getCompareSubject())
                .append("fieldName", this.fieldName)
                .append("leftVal", this.leftVal)
                .append("rightVal", this.rightVal)
                .append("leftObjectDetail", this.left)
                .append("rightObjectDetail", this.right)
                .toString();
    }
}
