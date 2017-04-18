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
package com.gs.catodeployany.compare.simple;

import java.util.Date;

import com.gs.catodeployany.compare.CatoDataComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDataComparator implements CatoDataComparator {

    private final double precisionBase10;
    private final double precisionBaseNeg10;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleDataComparator.class);

    public SimpleDataComparator() {
        this(CatoDataComparator.DEFAULT_DECIMAL_PRECISION);
    }

    public SimpleDataComparator(int decimalPrecision) {
        this.precisionBase10 = Math.pow(10, decimalPrecision);
        this.precisionBaseNeg10 = Math.pow(10, -decimalPrecision);
        LOG.debug("Initializing with decimal precision {}", decimalPrecision);
    }

    @Override
    public boolean compareValues(Object val1, Object val2) {
        return this.compare(val1, val2, false) == 0;
    }

    @Override
    public int compareKeyValues(Object val1, Object val2) {
        return this.compare(val1, val2, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compare(Object val1, Object val2, boolean compareKey) {
        if (val1 == null && val2 == null) {
            return 0;
        }
        if (val1 == null) {
            return 1;
        }
        if (val2 == null) {
            return -1;
        }

        if (val1 instanceof Number && val2 instanceof Number) {
            if (compareKey) {
                return this.roundCompare((Number) val1, (Number) val2);
            } else {
                return this.subtractCompare((Number) val1, (Number) val2);
            }
        }

        if (val1 instanceof String && val2 instanceof String) {
            return ((String) val1).trim().compareTo(((String) val2).trim());
        }

        if (val1 instanceof Date && val2 instanceof Date) {
            return Long.valueOf(((Date) val1).getTime()).compareTo(((Date) val2).getTime());
        }

        if (compareKey) {
            if (val1.getClass().equals(val2.getClass()) && val1 instanceof Comparable) {
                return ((Comparable) val1).compareTo((Comparable) val2);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Comparing toString() methods for value {} of type {} to value {} of different type {}",
                        val1, val1.getClass().getName(), val2, val2.getClass().getName());
            }

            return val1.toString().compareTo(val2.toString());
        } else {
            if (LOG.isDebugEnabled() && !val1.getClass().equals(val2.getClass())) {
                LOG.debug("Comparing value {} of type {} to value {} of different type {}",
                        val1, val1.getClass().getName(), val2, val2.getClass().getName());
            }

            return val1.equals(val2) ? 0 : -2;
        }
    }

    private int subtractCompare(Number num1, Number num2) {
        return Math.abs(num1.doubleValue() - num2.doubleValue()) < this.precisionBaseNeg10 ? 0 : -2;
    }

    private int roundCompare(Number num1, Number num2) {
        return this.round(num1).compareTo(this.round(num2));
    }

    private Long round(Number n) {
        return Math.round(n.doubleValue() * this.precisionBase10);
    }
}
