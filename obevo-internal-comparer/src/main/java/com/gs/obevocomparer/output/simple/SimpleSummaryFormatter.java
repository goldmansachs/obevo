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
package com.gs.obevocomparer.output.simple;

import java.io.IOException;

import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.output.CatoComparisonMetadata;
import com.gs.obevocomparer.output.CatoComparisonMetadata.BreakTypeInfo;
import com.gs.obevocomparer.output.CatoContentFormatter;
import com.gs.obevocomparer.output.CatoContentMetadata;
import com.gs.obevocomparer.output.CatoContentRow.ValueType;
import com.gs.obevocomparer.output.CatoContentWriter;

public class SimpleSummaryFormatter implements CatoContentFormatter {

    private final boolean writeLegend;

    public SimpleSummaryFormatter(boolean writeLegend) {
        this.writeLegend = writeLegend;
    }

    public void writeData(CatoComparisonMetadata comparisonMetadata, CatoContentWriter contentWriter) throws IOException {

        CatoContentMetadata contentMetadata = new SimpleContentMetadata(comparisonMetadata.getComparison().getName() + " Summary", 0, 0);
        contentWriter.openContent(contentMetadata);

        CatoComparison comparison = comparisonMetadata.getComparison();
        SimpleContentRow row;

        row = new SimpleContentRow(1);
        row.set(0, comparison.getName() + " Info", ValueType.TITLE);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, comparison.getLeftDataSource().getName() + " Size");
        row.setValue(1, comparison.getLeftData().size());
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, comparison.getRightDataSource().getName() + " Size");
        row.setValue(1, comparison.getRightData().size());
        contentWriter.writeRow(row);
        contentWriter.writeRow(new SimpleContentRow(0));

        boolean hasExcluded = comparisonMetadata.getExcludedBreakSize() > 0 || !comparisonMetadata.getExcludedFieldBreakFields().isEmpty();
        row = new SimpleContentRow(hasExcluded ? 3 : 2);
        row.set(0, "Break Type", ValueType.TITLE);
        row.set(1, "Count", ValueType.TITLE);
        if (hasExcluded) {
            row.set(2, "Excluded", ValueType.TITLE);
        }
        contentWriter.writeRow(row);

        hasExcluded = comparisonMetadata.getExcludedBreakSize() != 0;
        row = new SimpleContentRow(hasExcluded ? 3 : 2);
        row.setValue(0, "Total breaks");
        row.setValue(1, comparisonMetadata.getIncludedBreakSize());
        if (hasExcluded) {
            row.setValue(2, comparisonMetadata.getExcludedBreakSize());
        }
        contentWriter.writeRow(row);

        for (BreakTypeInfo type : comparisonMetadata.getBreakTypeInfo()) {
            hasExcluded = type.getExcludeCount() != 0;
            row = new SimpleContentRow(hasExcluded ? 3 : 2);

            row.setValue(0, type.getType() + " breaks");
            row.setValue(1, type.getBreakCount());
            if (hasExcluded) {
                row.setValue(2, type.getExcludeCount());
            }
            contentWriter.writeRow(row);
        }

        if (this.writeLegend) {
            this.writeLegend(comparisonMetadata, contentWriter);
        }

        contentWriter.closeContent();
    }

    private void writeLegend(CatoComparisonMetadata comparisonMetadata, CatoContentWriter contentWriter) throws IOException {
        SimpleContentRow row;

        row = new SimpleContentRow(0);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(1);
        row.set(0, "Color Legend", ValueType.TITLE);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, comparisonMetadata.getComparison().getLeftDataSource().getShortName() + " value");
        row.setValueType(1, ValueType.FIELD_BREAK);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, comparisonMetadata.getComparison().getRightDataSource().getShortName() + " value");
        row.setValueType(1, ValueType.RIGHT_VALUE);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, "Only in " + comparisonMetadata.getComparison().getLeftDataSource().getShortName());
        row.setValueType(1, ValueType.LEFT_ONLY);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, "Only in " + comparisonMetadata.getComparison().getRightDataSource().getShortName());
        row.setValueType(1, ValueType.RIGHT_ONLY);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, "Key field");
        row.setValueType(1, ValueType.KEY);
        contentWriter.writeRow(row);

        row = new SimpleContentRow(2);
        row.setValue(0, "Excluded field");
        row.setValueType(1, ValueType.EXCLUDE);
        contentWriter.writeRow(row);
    }
}
