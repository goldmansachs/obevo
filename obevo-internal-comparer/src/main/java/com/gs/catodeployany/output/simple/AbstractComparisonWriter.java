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

import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.compare.CatoDataSide;
import com.gs.catodeployany.output.CatoComparisonMetadata;
import com.gs.catodeployany.output.CatoComparisonWriter;
import com.gs.catodeployany.output.CatoContentFormatter;
import com.gs.catodeployany.output.CatoContentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComparisonWriter implements CatoComparisonWriter {

    private final boolean writeSummary;
    private final boolean writeLegend;
    private final boolean writeBreaks;
    private final boolean writeDataSets;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractComparisonWriter.class);

    protected AbstractComparisonWriter(boolean writeSummary, boolean writeLegend, boolean writeBreaks, boolean writeDataSets) {
        this.writeSummary = writeSummary;
        this.writeLegend = writeLegend;
        this.writeBreaks = writeBreaks;
        this.writeDataSets = writeDataSets;
    }

    protected abstract CatoContentWriter getSummaryContentWriter();

    protected abstract CatoContentWriter getBreakContentWriter();

    protected abstract CatoContentWriter getExcludedBreakContentWriter();

    protected abstract CatoContentWriter getLeftDataSetContentWriter();

    protected abstract CatoContentWriter getRightDataSetContentWriter();

    public void writeComparison(CatoComparison comparison) throws IOException {
        CatoComparisonMetadata comparisonMetadata = new SimpleComparisonMetadata(comparison);

        if (this.writeSummary) {
            this.getSummaryFormatter().writeData(comparisonMetadata, this.getSummaryContentWriter());
        }

        if (this.writeBreaks && comparisonMetadata.getIncludedBreakSize() > 0) {
            this.getBreakFormatter().writeData(comparisonMetadata, this.getBreakContentWriter());
        }

        if (this.writeBreaks && comparisonMetadata.getExcludedBreakSize() > 0) {
            this.getExcludedBreakFormatter().writeData(comparisonMetadata, this.getExcludedBreakContentWriter());
        }

        if (this.writeDataSets) {
            this.getLeftDataSetFormatter().writeData(comparisonMetadata, this.getLeftDataSetContentWriter());
            this.getRightDataSetFormatter().writeData(comparisonMetadata, this.getRightDataSetContentWriter());
        }
    }

    protected CatoContentFormatter getSummaryFormatter() {
        return new SimpleSummaryFormatter(this.writeLegend);
    }

    protected CatoContentFormatter getBreakFormatter() {
        return new SimpleBreakFormatter(false);
    }

    protected CatoContentFormatter getExcludedBreakFormatter() {
        return new SimpleBreakFormatter(true);
    }

    protected CatoContentFormatter getLeftDataSetFormatter() {
        return new SimpleDataSetFormatter(CatoDataSide.LEFT);
    }

    protected CatoContentFormatter getRightDataSetFormatter() {
        return new SimpleDataSetFormatter(CatoDataSide.RIGHT);
    }
}
