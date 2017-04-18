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
package com.gs.catodeployany.output.simple;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.output.CatoComparisonMetadata;
import com.gs.catodeployany.output.CatoContentFormatter;
import com.gs.catodeployany.output.CatoContentMetadata;
import com.gs.catodeployany.output.CatoContentRow;
import com.gs.catodeployany.output.CatoContentWriter;

public class SimpleDataSetFormatter implements CatoContentFormatter {

    private final CatoDataSide dataSide;

    public SimpleDataSetFormatter(CatoDataSide dataSide) {
        this.dataSide = dataSide;
    }

    public void writeData(CatoComparisonMetadata comparisonMetadata, CatoContentWriter contentWriter) throws IOException {

        Collection<CatoDataObject> data = this.dataSide == CatoDataSide.LEFT ?
                comparisonMetadata.getComparison().getLeftData() :
                comparisonMetadata.getComparison().getRightData();

        String dataName = this.dataSide == CatoDataSide.LEFT ?
                comparisonMetadata.getComparison().getLeftDataSource().getName() :
                comparisonMetadata.getComparison().getRightDataSource().getName();

        Map<String, Integer> fieldMap = this.calculateFieldMap(data);

        CatoContentMetadata contentMetadata = new SimpleContentMetadata(dataName, 1, 0);
        contentWriter.openContent(contentMetadata);

        contentWriter.writeRow(this.createHeaderRow(fieldMap));

        for (CatoDataObject object : data) {
            contentWriter.writeRow(this.createRow(object, fieldMap));
        }

        contentWriter.closeContent();
    }

    private CatoContentRow createHeaderRow(Map<String, Integer> fieldMap) {
        SimpleContentRow row = new SimpleContentRow(fieldMap.size());
        for (Entry<String, Integer> field : fieldMap.entrySet()) {
            row.setValue(field.getValue(), field.getKey());
        }

        return row;
    }

    private CatoContentRow createRow(CatoDataObject object, Map<String, Integer> fieldMap) {
        SimpleContentRow row = new SimpleContentRow(fieldMap.size());
        for (String field : object.getFields()) {
            row.setValue(fieldMap.get(field), object.getValue(field));
        }

        return row;
    }

    private Map<String, Integer> calculateFieldMap(Collection<CatoDataObject> data) {
        Map<String, Integer> fieldMap = new LinkedHashMap<String, Integer>();

        for (CatoDataObject object : data) {
            for (String field : object.getFields()) {
                if (!fieldMap.containsKey(field)) {
                    fieldMap.put(field, fieldMap.size());
                }
            }
        }

        return fieldMap;
    }
}
