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
package com.gs.catodeployany.compare.breaks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.data.CatoDataObject;

public class FieldBreak extends AbstractBreak {

    private final Map<String, Object> fieldBreaks;
    private final Map<String, Boolean> fieldExcludes;

    public FieldBreak(CatoDataObject dataObject, Map<String, Object> fieldBreaks) {
        super(dataObject, CatoDataSide.LEFT);
        this.fieldBreaks = fieldBreaks;

        this.fieldExcludes = new HashMap<String, Boolean>();
        for (String field : fieldBreaks.keySet()) {
            this.fieldExcludes.put(field, false);
        }
    }

    public Map<String, Object> getFieldBreaks() {
        return this.fieldBreaks;
    }

    public Set<String> getFields() {
        return this.fieldBreaks.keySet();
    }

    public void setExcluded(String field, boolean excluded) {
        this.fieldExcludes.put(field, excluded);
    }

    public boolean isExcluded(String field) {
        return this.fieldExcludes.get(field);
    }

    public Object getExpectedValue(String field) {
        return this.fieldBreaks.get(field);
    }

    public Object getActualValue(String field) {
        return this.dataObject.getValue(field);
    }
}
