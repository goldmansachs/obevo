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
package com.gs.obevo.dbmetadata.impl;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.eclipse.collections.api.block.function.Function;

public class ExtraIndexInfo {
    public static final Function<ExtraIndexInfo, String> TO_TABLE_NAME = new Function<ExtraIndexInfo, String>() {
        @Override
        public String valueOf(ExtraIndexInfo extraIndexInfo) {
            return extraIndexInfo.getTableName();
        }
    };
    public static final Function<ExtraIndexInfo, String> TO_INDEX_NAME = new Function<ExtraIndexInfo, String>() {
        @Override
        public String valueOf(ExtraIndexInfo extraIndexInfo) {
            return extraIndexInfo.getIndexName();
        }
    };
    private final String tableName;
    private final String indexName;
    private final boolean constraint;
    private final boolean clustered;

    public ExtraIndexInfo(String tableName, String indexName, boolean constraint, boolean clustered) {
        this.tableName = tableName;
        this.indexName = indexName;
        this.constraint = constraint;
        this.clustered = clustered;
    }

    public String getTableName() {
        return this.tableName;
    }

    String getIndexName() {
        return this.indexName;
    }

    public boolean isConstraint() {
        return this.constraint;
    }

    public boolean isClustered() {
        return this.clustered;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("tableName", tableName)
                .append("indexName", indexName)
                .append("constraint", constraint)
                .append("clustered", clustered)
                .toString();
    }
}
