/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collection;

import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.output.CatoMultiComparisonWriter;

public abstract class AbstractMultiComparisonWriter extends AbstractComparisonWriter implements CatoMultiComparisonWriter {

    protected AbstractMultiComparisonWriter(boolean writeSummary, boolean writeLegend, boolean writeBreaks, boolean writeDataSets) {
        super(writeSummary, writeLegend, writeBreaks, writeDataSets);
    }

    public void writeComparison(Collection<CatoComparison> comparisons) throws IOException {
        for (CatoComparison comparison : comparisons) {
            this.writeComparison(comparison);
        }
    }
}
