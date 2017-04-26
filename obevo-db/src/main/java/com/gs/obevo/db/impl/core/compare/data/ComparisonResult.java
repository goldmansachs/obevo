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
package com.gs.obevo.db.impl.core.compare.data;

import com.gs.obevocomparer.compare.CatoComparison;

class ComparisonResult {
    private final ComparisonCommand comparisonCommand;
    private final CatoComparison catoComparison;
    private final ComparisonResultType comparisonResultType;

    public ComparisonResult(ComparisonCommand comparisonCommand, CatoComparison catoComparison,
            ComparisonResultType comparisonResultType) {
        this.comparisonCommand = comparisonCommand;
        this.catoComparison = catoComparison;
        this.comparisonResultType = comparisonResultType;
    }

    public ComparisonCommand getComparisonCommand() {
        return this.comparisonCommand;
    }

    public CatoComparison getCatoComparison() {
        return this.catoComparison;
    }

    public ComparisonResultType getComparisonResultType() {
        return this.comparisonResultType;
    }
}
