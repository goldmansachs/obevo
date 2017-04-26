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
package com.gs.obevo.dbmetadata.api;

import com.gs.obevo.dbmetadata.deepcompare.ClassCompareInfo;
import com.gs.obevo.dbmetadata.deepcompare.CollectionFieldCompareInfo;
import com.gs.obevo.dbmetadata.deepcompare.CompareBreak;
import com.gs.obevo.dbmetadata.deepcompare.DeepCompareUtil;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.impl.factory.Lists;

public class DbMetadataComparisonUtil {
    private final DeepCompareUtil deepCompareUtil;

    public DbMetadataComparisonUtil() {
        ClassCompareInfo tableCompareInfo = ClassCompareInfo.newBuilder()
                .setClazz(DaTable.class)
                .setKeyFunction(DaNamedObject.TO_NAME)
                .addCompareFunction("tableType", new Function<DaTable, String>() {
                    @Override
                    public String valueOf(DaTable object) {
                        return object.isView() ? "view" : "table";
                    }
                })
                .addCompareFunction("primaryKey", DaTable.TO_PRIMARY_KEY)
                .addCollectionComparisonInfo(new CollectionFieldCompareInfo(DaColumn.class, DaTable.TO_COLUMNS))
                .addCollectionComparisonInfo(new CollectionFieldCompareInfo(DaIndex.class, DaTable.TO_INDICES))
                        // the fields below are not yet activated for comparison
//				.addCollectionComparisonInfo(new CollectionFieldCompareInfo(ForeignKey.class, TABLE_TO_FOREIGN_KEYS))
//				.addCollectionComparisonInfo(new CollectionFieldCompareInfo(Index.class, TABLE_TO_INDICES))
//				.addCollectionComparisonInfo(new CollectionFieldCompareInfo(Privilege.class, TABLE_TO_PRIVILEGES))
//				.addCollectionComparisonInfo(new CollectionFieldCompareInfo(Trigger.class, TABLE_TO_TRIGGERS))
                .build();
        ClassCompareInfo columnCompareInfo = ClassCompareInfo.newBuilder()
                .setClazz(DaColumn.class)
                .setKeyFunction(DaNamedObject.TO_NAME)
                .addCompareFunction("columnDataType", DaColumn.TO_COLUMN_DATA_TYPE)
                .addCompareFunction("defaultValue", DaColumn.TO_DEFAULT_VALUE)
                .addCompareFunction("nullable", DaColumn.TO_NULLABLE)
                .addCompareFunction("width", DaColumn.TO_WIDTH)  // handles decimal-digits and size
                .build();
        ClassCompareInfo indexCompareInfo = ClassCompareInfo.newBuilder()
                .setClazz(DaIndex.class)
                .setKeyFunction(DaPrimaryKey.TO_COLUMN_STRING)
//                .addCompareFunction("indexName", TO_NAME)
                .addCompareFunction("indexType", DaIndex.TO_INDEX_TYPE)
                .addCompareFunction("unique", DaIndex.TO_UNIQUE)
                .addCompareFunction("pk", DaIndex.TO_PK)
                .addCollectionComparisonInfo(new CollectionFieldCompareInfo(DaColumn.class, DaIndex.TO_COLUMNS))
                .build();
        ClassCompareInfo pkCompareInfo = ClassCompareInfo.newBuilder()
                .setClazz(DaPrimaryKey.class)
                .setKeyFunction(DaPrimaryKey.TO_COLUMN_STRING)
//                .addCompareFunction("indexName", TO_NAME)
                .addCompareFunction("indexType", DaIndex.TO_INDEX_TYPE)
                .addCompareFunction("unique", DaIndex.TO_UNIQUE)
                .addCollectionComparisonInfo(new CollectionFieldCompareInfo(DaColumn.class, DaIndex.TO_COLUMNS))
                .build();

        this.deepCompareUtil = new DeepCompareUtil(Lists.mutable.with(tableCompareInfo, columnCompareInfo, pkCompareInfo, indexCompareInfo));
    }

    public MutableCollection<CompareBreak> compareTables(RichIterable<DaTable> tableLefts, RichIterable<DaTable> tableRights) {
        return this.deepCompareUtil.compareCollections(DaTable.class, tableLefts.toList(), tableRights.toList());
    }

    public MutableCollection<CompareBreak> compareTables(DaTable tableLeft, DaTable tableRight) {
        return this.deepCompareUtil.compareObjects(tableLeft, tableRight);
    }
}
