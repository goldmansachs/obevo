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
package com.gs.obevo.db.impl.core.compare.data;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;

public class DbDataComparisonConfig {
    private MutableCollection<String> inputTables;
    private MutableSet<String> excludedTables;
    private MutableList<DbDataSource> dbDataSources;
    private MutableList<Pair<String, String>> comparisonCommandNamePairs;
    private MutableList<ComparisonCommand> comparisonCommands;

    public MutableCollection<String> getInputTables() {
        return this.inputTables;
    }

    public void setInputTables(MutableCollection<String> inputTables) {
        this.inputTables = inputTables;
    }

    public MutableSet<String> getExcludedTables() {
        return this.excludedTables;
    }

    public void setExcludedTables(MutableSet<String> excludedTables) {
        this.excludedTables = excludedTables;
    }

    public MutableList<DbDataSource> getDbDataSources() {
        return this.dbDataSources;
    }

    public void setDbDataSources(MutableList<DbDataSource> dbDataSources) {
        this.dbDataSources = dbDataSources;
    }

    public void setComparisonCommandNamePairs(MutableList<Pair<String, String>> comparisonCommandNamePairs) {
        this.comparisonCommandNamePairs = comparisonCommandNamePairs;
    }

    public MutableList<ComparisonCommand> getComparisonCommands() {
        return this.comparisonCommands;
    }

    public void init() {
        this.comparisonCommands = Lists.mutable.empty();
        MutableMap<String, DbDataSource> sourceMap = this.dbDataSources.groupByUniqueKey(new Function<DbDataSource, String>() {
            @Override
            public String valueOf(DbDataSource dbDataSource) {
                return dbDataSource.getName();
            }
        });
        for (Pair<String, String> comparisonCommandNamePair : this.comparisonCommandNamePairs) {
            this.comparisonCommands.add(new ComparisonCommand(
                    sourceMap.get(comparisonCommandNamePair.getOne())
                    , sourceMap.get(comparisonCommandNamePair.getTwo())
            ));
        }
    }
}
