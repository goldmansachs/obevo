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

import java.util.Collection;
import java.util.HashSet;

import com.gs.obevocomparer.compare.CatoBreakExcluder;
import com.gs.obevocomparer.compare.CatoDataComparator;
import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.compare.breaks.BreakExclude;
import com.gs.obevocomparer.compare.breaks.FieldBreak;
import com.gs.obevocomparer.data.CatoDataObject;

public class SimpleBreakExcluder implements CatoBreakExcluder {

    private final CatoDataComparator comparator;

    public SimpleBreakExcluder(CatoDataComparator comparator) {
        this.comparator = comparator;
    }

    public void excludeBreaks(Iterable<Break> breaks, Collection<BreakExclude> breakExcludes) {

        Collection<String> fields = new HashSet<String>();
        FieldBreak fieldBreak;

        for (Break br : breaks) {
            fields.clear();

            for (BreakExclude exclude : breakExcludes) {
                if (!this.matchesKey(exclude, br.getDataObject())) {
                    continue;
                }

                if (this.matchesAnything(exclude)) {
                    br.setExcluded(true);
                    break;
                }

                if (br instanceof FieldBreak) {
                    fieldBreak = (FieldBreak) br;

                    for (String field : fieldBreak.getFields()) {
                        if (this.matchesData(exclude, field, fieldBreak.getActualValue(field), fieldBreak.getExpectedValue(field))) {
                            fieldBreak.setExcluded(field, true);
                            fields.add(field);
                        }
                    }
                }
            }

            if (br instanceof FieldBreak && fields.size() == ((FieldBreak) br).getFields().size()) {
                br.setExcluded(true);
            }
        }
    }

    private boolean matchesAnything(BreakExclude exclude) {
        return exclude.getField() == null &&
                exclude.getLeftValue() == null &&
                exclude.getRightValue() == null;
    }

    private boolean matchesKey(BreakExclude exclude, CatoDataObject key) {
        if (exclude.getKey() == null) {
            return true;
        }

        for (String field : exclude.getKey().getFields()) {
            if (!this.comparator.compareValues(exclude.getKey().getValue(field), key.getValue(field))) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesData(BreakExclude exclude, String field, Object leftValue, Object rightValue) {
        return (exclude.getField() == null || exclude.getField().equals(field)) &&
                (exclude.getLeftValue() == null || this.comparator.compareValues(exclude.getLeftValue(), leftValue)) &&
                (exclude.getRightValue() == null || this.comparator.compareValues(exclude.getRightValue(), rightValue));
    }
}
