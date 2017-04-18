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

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

class ComparisonSummaryRow {
    private final String table;
    private final MutableMap<String, ComparisonResult> results = UnifiedMap.newMap();

    public ComparisonSummaryRow(String table) {
        this.table = table;
    }

    public String getTable() {
        return this.table;
    }

    public void addResult(String comparisonResultKey, ComparisonResult result) {
        this.results.put(comparisonResultKey, result);
    }

    public ComparisonResult getResult(String comparisonResultKey) {
        return this.results.get(comparisonResultKey);
    }

    public String getResultForOutput(String comparisonResultKey) {

        switch (this.getResult(comparisonResultKey).getComparisonResultType()) {
        case ONLY_ON_LEFT_SIDE:
            return "only-in-" + this.getResult(comparisonResultKey).getComparisonCommand().getLeftDs().getName();
        case ONLY_ON_RIGHT_SIDE:
            return "only-in-" + this.getResult(comparisonResultKey).getComparisonCommand().getRightDs().getName();
        default:
            return this.getResult(comparisonResultKey).getComparisonResultType().name();
        }
    }

    public boolean isNeedsAnalysis() {
        return this.results.valuesView().anySatisfy(Predicates.notEqual(ComparisonResultType.NO_BREAKS));
    }
}
