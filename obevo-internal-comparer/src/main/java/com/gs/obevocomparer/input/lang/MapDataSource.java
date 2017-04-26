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
package com.gs.obevocomparer.input.lang;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.AbstractCatoDataSource;
import org.eclipse.collections.impl.factory.Lists;

public class MapDataSource extends AbstractCatoDataSource {

    private Collection<? extends Map<String, ? extends Object>> dataMaps;
    private Iterator<CatoDataObject> catoObjs;

    public MapDataSource(String name, Collection<? extends Map<String, ? extends Object>> dataMaps) {
        super(name, null);
        this.dataMaps = dataMaps;
    }

    protected CatoDataObject nextDataObject() throws Exception {
        if (!this.catoObjs.hasNext()) {
            return null;
        }

        return this.catoObjs.next();
    }

    protected void openSource() throws Exception {
        List<CatoDataObject> dataObjs = Lists.mutable.empty();
        for (Map<String, ? extends Object> dataMap : this.dataMaps) {
            CatoDataObject dataObj = this.createDataObject();
            for (Entry<String, ? extends Object> entry : dataMap.entrySet()) {
                dataObj.setValue(entry.getKey(), entry.getValue());
            }
            dataObjs.add(dataObj);
        }
        this.catoObjs = dataObjs.iterator();
        this.dataMaps = null;  // set to null to release the reference to the map
    }

    protected void closeSource() throws Exception {
    }
}
