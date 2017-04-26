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

import org.eclipse.collections.api.block.function.Function;

public class ComparisonCommand {
    private final DbDataSource leftDs;
    private final DbDataSource rightDs;

    public static final Function<ComparisonCommand, String> TO_KEY = new Function<ComparisonCommand, String>() {
        @Override
        public String valueOf(ComparisonCommand object) {
            return object.getKey();
        }
    };

    public ComparisonCommand(DbDataSource leftDs, DbDataSource rightDs) {
        this.leftDs = leftDs;
        this.rightDs = rightDs;
    }

    public DbDataSource getLeftDs() {
        return this.leftDs;
    }

    public DbDataSource getRightDs() {
        return this.rightDs;
    }

    public String getKey() {
        return this.leftDs.getName() + "-" + this.rightDs.getName();
    }
}
