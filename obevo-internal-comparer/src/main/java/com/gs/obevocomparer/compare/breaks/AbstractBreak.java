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
package com.gs.obevocomparer.compare.breaks;

import com.gs.obevocomparer.compare.CatoDataSide;
import com.gs.obevocomparer.data.CatoDataObject;

public abstract class AbstractBreak implements Break {

    final CatoDataObject dataObject;
    final CatoDataSide dataSide;
    boolean excluded;

    protected AbstractBreak(CatoDataObject dataObject, CatoDataSide dataSide) {
        this.dataObject = dataObject;
        this.dataSide = dataSide;
    }

    public CatoDataObject getDataObject() {
        return this.dataObject;
    }

    public CatoDataSide getDataSide() {
        return this.dataSide;
    }

    public boolean isExcluded() {
        return this.excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }
}
