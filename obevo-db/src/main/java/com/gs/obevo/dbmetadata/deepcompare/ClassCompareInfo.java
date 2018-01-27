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
package com.gs.obevo.dbmetadata.deepcompare;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;

public class ClassCompareInfo<T> {
    private final Class<T> clazz;
    private final Function<T, ?> keyFunction;
    private final MutableCollection<Pair<String, Function<T, ?>>> compareFunctions;
    private final MutableCollection<CollectionFieldCompareInfo> collectionComparisonInfos;

    public static final Function<ClassCompareInfo, Class> TO_CLAZZ = new Function<ClassCompareInfo, Class>() {
        @Override
        public Class valueOf(ClassCompareInfo object) {
            return object.getClazz();
        }
    };

    public static <T> ClassCompareInfoBuilder<T> newBuilder() {
        return new ClassCompareInfoBuilder<T>();
    }

    private ClassCompareInfo(Class<T> clazz, Function<T, ?> keyFunction,
            MutableCollection<Pair<String, Function<T, ?>>> compareFunctions,
            MutableCollection<CollectionFieldCompareInfo> collectionComparisonInfos) {
        this.clazz = clazz;
        this.keyFunction = keyFunction;
        this.compareFunctions = compareFunctions;
        this.collectionComparisonInfos = collectionComparisonInfos;
    }

    private Class<T> getClazz() {
        return this.clazz;
    }

    public Function<T, ?> getKeyFunction() {
        return this.keyFunction;
    }

    public MutableCollection<Pair<String, Function<T, ?>>> getCompareFunctions() {
        return this.compareFunctions;
    }

    public MutableCollection<CollectionFieldCompareInfo> getCollectionComparisonInfos() {
        return this.collectionComparisonInfos;
    }

    public static class ClassCompareInfoBuilder<T> {
        private Class<T> clazz;
        private Function<T, ?> keyFunction;
        private final MutableCollection compareFunctions = Lists.mutable.empty();
        private final MutableCollection<CollectionFieldCompareInfo> collectionComparisonInfos = Lists.mutable.empty();

        public ClassCompareInfoBuilder setClazz(Class clazz) {
            this.clazz = clazz;
            return this;
        }

        public ClassCompareInfoBuilder setKeyFunction(Function<T, ?> keyFunction) {
            this.keyFunction = keyFunction;
            return this;
        }

        public ClassCompareInfoBuilder addCompareFunction(String fieldName, Function<T, ?> compareFunction) {
            this.compareFunctions.add(Tuples.pair(fieldName, compareFunction));
            return this;
        }

        public ClassCompareInfoBuilder addCollectionComparisonInfo(CollectionFieldCompareInfo collectionComparisonInfo) {
            this.collectionComparisonInfos.add(collectionComparisonInfo);
            return this;
        }

        public ClassCompareInfo build() {
            return new ClassCompareInfo(this.clazz, this.keyFunction, this.compareFunctions, this.collectionComparisonInfos);
        }
    }
}
