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
package com.gs.obevocomparer.compare.simple;

import java.util.Comparator;
import java.util.List;

import com.gs.obevocomparer.compare.CatoDataComparator;
import com.gs.obevocomparer.data.CatoDataObject;

public class SimpleDataObjectComparator implements Comparator<CatoDataObject> {

    private final CatoDataComparator dataComparator;
    private final List<String> compareFields;

    public SimpleDataObjectComparator(CatoDataComparator dataComparator, List<String> compareFields) {
        this.dataComparator = dataComparator;
        this.compareFields = compareFields;
    }

    @Override
    public int compare(CatoDataObject obj1, CatoDataObject obj2) {
        if (obj1 == null && obj2 == null) {
            return 0;
        }
        if (obj1 == null) {
            return 1;
        }
        if (obj2 == null) {
            return -1;
        }

        int compareValue;

        for (String field : this.compareFields) {
            compareValue = this.dataComparator.compareKeyValues(obj1.getValue(field), obj2.getValue(field));

            if (compareValue != 0) {
                return compareValue;
            }
        }

        return 0;
    }
}
