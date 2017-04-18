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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.compare.breaks.Break;
import com.gs.catodeployany.compare.breaks.FieldBreak;
import com.gs.catodeployany.compare.breaks.GroupBreak;
import com.gs.catodeployany.output.CatoComparisonMetadata;
import com.gs.catodeployany.output.CatoContentFormatter;
import com.gs.catodeployany.output.CatoContentMetadata;
import com.gs.catodeployany.output.CatoContentRow;
import com.gs.catodeployany.output.CatoContentRow.ValueType;
import com.gs.catodeployany.output.CatoContentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBreakFormatter implements CatoContentFormatter {

    private static final String BREAK_TYPE = "Break Type";
    private static final String BREAK_GROUP = "Group";

    protected final boolean excludeWriter;

    protected static final Logger LOG = LoggerFactory.getLogger(SimpleBreakFormatter.class);

    public SimpleBreakFormatter(boolean excludeWriter) {
        this.excludeWriter = excludeWriter;
    }

    public void writeData(CatoComparisonMetadata comparisonMetadata, CatoContentWriter contentWriter) throws IOException {

        Map<String, Set<Integer>> fieldMap = new LinkedHashMap<String, Set<Integer>>();
        Map<String, String> fieldBreakMap = new LinkedHashMap<String, String>();

        int fieldCount = this.calculateFieldMaps(comparisonMetadata, fieldMap, fieldBreakMap);

        for (String keyField : comparisonMetadata.getComparison().getKeyFields()) {
            if (!fieldMap.containsKey(keyField)) {
                throw new IllegalArgumentException("Could not find key field '" + keyField + "' in comparison " + comparisonMetadata.getComparison().getName());
            }
        }

        CatoContentMetadata contentMetadata = new SimpleContentMetadata(
                comparisonMetadata.getComparison().getName() + (this.excludeWriter ? " Excluded" : ""),
                1, comparisonMetadata.hasGroupBreaks() ? 2 : 1);

        contentWriter.openContent(contentMetadata);

        contentWriter.writeRow(this.createHeaderRow(comparisonMetadata, fieldCount, fieldMap, fieldBreakMap));

        int count = 0;

        for (Break br : comparisonMetadata.getComparison().getBreaks()) {
            if (this.excludeWriter == br.isExcluded()) {
                contentWriter.writeRow(this.createBreakRow(br, comparisonMetadata, fieldCount, fieldMap, fieldBreakMap));
                if (++count % 10000 == 0) {
                    LOG.info("Wrote {} {}breaks", count, this.excludeWriter ? "excluded " : "");
                }
            }
        }
        LOG.info("Wrote {} total {}breaks", count, this.excludeWriter ? "excluded " : "");

        contentWriter.closeContent();
    }

    private CatoContentRow createBreakRow(Break br, CatoComparisonMetadata comparisonMetadata, int fieldCount, Map<String, Set<Integer>> fieldMap, Map<String, String> fieldBreakMap) {

        SimpleContentRow row = new SimpleContentRow(fieldCount);

        String breakTypeStr = "N/A";
        int groupId = -1;

        for (String field : br.getDataObject().getFields()) {
            this.setFieldValue(field, br.getDataObject().getValue(field), row, fieldMap);
        }

        if (br instanceof FieldBreak) {
            FieldBreak fieldBreak = (FieldBreak) br;

            for (String field : fieldBreak.getFields()) {
                this.setFieldValue(fieldBreakMap.get(field), fieldBreak.getExpectedValue(field), row, fieldMap);
                this.setFieldType(fieldBreakMap.get(field), ValueType.RIGHT_VALUE, row, fieldMap);

                if (!this.excludeWriter && fieldBreak.isExcluded(field)) {
                    this.setFieldType(field, ValueType.EXCLUDE, row, fieldMap);
                } else {
                    this.setFieldType(field, ValueType.FIELD_BREAK, row, fieldMap);
                }
            }
            breakTypeStr = "Different values";
        } else {
            if (br.getDataSide() == CatoDataSide.LEFT) {
                this.setRowType(ValueType.LEFT_ONLY, row, fieldCount);
                breakTypeStr = "Only in " + comparisonMetadata.getComparison().getLeftDataSource().getShortName();
            } else if (br.getDataSide() == CatoDataSide.RIGHT) {
                this.setRowType(ValueType.RIGHT_ONLY, row, fieldCount);
                breakTypeStr = "Only in " + comparisonMetadata.getComparison().getRightDataSource().getShortName();
            }
        }

        if (br instanceof GroupBreak) {
            GroupBreak groupBreak = (GroupBreak) br;
            breakTypeStr += " group";
            groupId = groupBreak.getGroupId();

            for (String field : groupBreak.getFields()) {
                this.setFieldType(field, ValueType.FIELD_BREAK, row, fieldMap);
            }
        }

        for (String field : comparisonMetadata.getComparison().getExcludeFields()) {
            this.setFieldType(field, ValueType.EXCLUDE, row, fieldMap);
        }

        this.setFieldValue(BREAK_TYPE, breakTypeStr, row, fieldMap);
        if (groupId != -1) {
            this.setFieldValue(BREAK_GROUP, groupId, row, fieldMap);
        }

        return row;
    }

    private CatoContentRow createHeaderRow(CatoComparisonMetadata comparisonMetadata, int fieldCount, Map<String, Set<Integer>> fieldMap, Map<String, String> fieldBreakMap) {

        SimpleContentRow row = new SimpleContentRow(fieldCount);

        for (String field : fieldMap.keySet()) {
            this.setFieldValue(field, field, row, fieldMap);
        }

        this.setFieldType(comparisonMetadata.getComparison().getKeyFields(), ValueType.KEY, row, fieldMap);
        this.setFieldType(comparisonMetadata.getComparison().getExcludeFields(), ValueType.EXCLUDE, row, fieldMap);
        this.setFieldType(comparisonMetadata.getExcludedFieldBreakFields(), ValueType.EXCLUDE, row, fieldMap);
        this.setFieldType(comparisonMetadata.getIncludedFieldBreakFields(), ValueType.FIELD_BREAK, row, fieldMap);
        this.setFieldType(comparisonMetadata.getGroupBreakFields(), ValueType.FIELD_BREAK, row, fieldMap);
        this.setFieldType(fieldBreakMap.values(), ValueType.RIGHT_VALUE, row, fieldMap);

        return row;
    }

    private void setFieldValue(String field, Object value, SimpleContentRow row, Map<String, Set<Integer>> fieldMap) {
        for (int index : fieldMap.get(field)) {
            row.setValue(index, value);
        }
    }

    private void setFieldType(String field, ValueType type, SimpleContentRow row, Map<String, Set<Integer>> fieldMap) {
        if (!fieldMap.containsKey(field)) {
            return;
        }

        for (int index : fieldMap.get(field)) {
            row.setValueType(index, type);
        }
    }

    private void setFieldType(Collection<String> fields, ValueType type, SimpleContentRow row, Map<String, Set<Integer>> fieldMap) {
        for (String field : fields) {
            this.setFieldType(field, type, row, fieldMap);
        }
    }

    private void setRowType(ValueType type, SimpleContentRow row, int fieldCount) {
        for (int index = 0; index < fieldCount; index++) {
            row.setValueType(index, type);
        }
    }

    private int calculateFieldMaps(CatoComparisonMetadata comparisonMetadata, Map<String, Set<Integer>> fieldMap, Map<String, String> fieldBreakMap) {

        Set<String> leftFields = new LinkedHashSet<String>(comparisonMetadata.getLeftFields());
        leftFields.addAll(comparisonMetadata.getFieldBreakFields());

        String fieldBreakField;
        int fieldCount = 0;

        this.addField(BREAK_TYPE, fieldCount++, fieldMap);
        if (comparisonMetadata.hasGroupBreaks()) {
            this.addField(BREAK_GROUP, fieldCount++, fieldMap);
        }

        // Add key fields first
        for (String field : comparisonMetadata.getComparison().getKeyFields()) {
            this.addField(field, fieldCount++, fieldMap);
        }

        for (String field : leftFields) {
            // key fields already added
            if (comparisonMetadata.getComparison().getKeyFields().contains(field)) {
                continue;
            }

            // add exclude fields last
            if (comparisonMetadata.getComparison().getExcludeFields().contains(field)) {
                continue;
            }

            this.addField(field, fieldCount++, fieldMap);

            if (comparisonMetadata.getFieldBreakFields().contains(field)) {
                fieldBreakField = comparisonMetadata.getComparison().getProperties().getMappedFields().get(field);

                // if there is no mapped field, use the same field name with a suffix
                if (fieldBreakField == null) {
                    fieldBreakField = this.getRefFieldSuffix(field, comparisonMetadata);
                }

                // if there is a mapped field, use that name instead
                else {
                    // if the mapped field name is also in the left dataset, then add the suffix
                    if (leftFields.contains(fieldBreakField)) {
                        fieldBreakField = this.getRefFieldSuffix(fieldBreakField, comparisonMetadata);
                    }
                }

                fieldBreakMap.put(field, fieldBreakField);
                this.addField(fieldBreakField, fieldCount++, fieldMap);
            }
        }

        for (String field : comparisonMetadata.getRightFields()) {
            // key fields already added
            if (comparisonMetadata.getComparison().getKeyFields().contains(field)) {
                continue;
            }

            // add exclude fields last
            if (comparisonMetadata.getComparison().getExcludeFields().contains(field)) {
                continue;
            }

            if (!fieldMap.keySet().contains(field)) {
                this.addField(field, fieldCount++, fieldMap);
            }
        }

        // add exclude fields
        for (String field : comparisonMetadata.getComparison().getExcludeFields()) {
            if (leftFields.contains(field) || comparisonMetadata.getRightFields().contains(field)) {
                this.addField(field, fieldCount++, fieldMap);
            }
        }

        return fieldCount;
    }

    private void addField(String field, int fieldCount, Map<String, Set<Integer>> fieldMap) {
        if (!fieldMap.containsKey(field)) {
            fieldMap.put(field, new HashSet<Integer>());
        }

        fieldMap.get(field).add(fieldCount);
    }

    private String getRefFieldSuffix(String field, CatoComparisonMetadata info) {
        return field.concat(" (" + info.getComparison().getRightDataSource().getShortName() + ")");
    }
}
