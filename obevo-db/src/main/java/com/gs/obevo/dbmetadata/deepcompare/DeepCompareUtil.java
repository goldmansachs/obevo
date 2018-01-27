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

import java.util.Collection;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;

public class DeepCompareUtil {
    private final MutableMultimap<Class, ClassCompareInfo> classCompareInfoMap;

    public DeepCompareUtil(MutableCollection<ClassCompareInfo> classCompareInfos) {
        this.classCompareInfoMap = classCompareInfos.groupBy(ClassCompareInfo.TO_CLAZZ);
    }

    public MutableCollection<CompareBreak> compareObjects(Object left, Object right) {
        MutableCollection<CompareBreak> breaks = Lists.mutable.empty();
        this.compareObjectsInternal(null, left, right, breaks);
        return breaks;
    }

    public MutableCollection<CompareBreak> compareCollections(Class clazz, Collection lefts, Collection rights) {
        MutableCollection<CompareBreak> breaks = Lists.mutable.empty();
        this.compareCollectionsInternal(clazz, lefts, rights, breaks);
        return breaks;
    }

    private void compareCollectionsInternal(Class clazz, Collection lefts, Collection rights,
            MutableCollection<CompareBreak> breaks) {
        MutableCollection<ClassCompareInfo> classCompareInfos = this.getClassCompareInfos(clazz);
        for (ClassCompareInfo classCompareInfo : classCompareInfos) {
            // UnifiedMapWithHashingStrategy.newMap()
            MutableSet groupByKeyLefts = UnifiedSetWithHashingStrategy.newSet(
                    HashingStrategies.fromFunction(classCompareInfo.getKeyFunction()), lefts);
            MutableSet groupByKeyRights = UnifiedSetWithHashingStrategy.newSet(
                    HashingStrategies.fromFunction(classCompareInfo.getKeyFunction()), rights);

            MutableMap groupByKeyLeftsMap = UnifiedMapWithHashingStrategy.newMap(HashingStrategies
                    .fromFunction(classCompareInfo.getKeyFunction()));
            MutableMap groupByKeyRightsMap = UnifiedMapWithHashingStrategy.newMap(HashingStrategies
                    .fromFunction(classCompareInfo.getKeyFunction()));

            for (Object obj : lefts) {
                groupByKeyLeftsMap.put(obj, obj);
            }
            for (Object obj : rights) {
                groupByKeyRightsMap.put(obj, obj);
            }

            MutableSet onlyLefts = groupByKeyLefts.difference(groupByKeyRights);
            breaks.addAll(onlyLefts.collect(ObjectCompareBreak.createObjectCompareBreak(clazz,
                    ObjectCompareBreak.ObjectCompareBreakSide.LEFT)));
            MutableSet onlyRights = groupByKeyRights.difference(groupByKeyLefts);
            breaks.addAll(onlyRights.collect(ObjectCompareBreak.createObjectCompareBreak(clazz,
                    ObjectCompareBreak.ObjectCompareBreakSide.RIGHT)));

            MutableSet boths = groupByKeyLefts.intersect(groupByKeyRights);

            for (Object both : boths) {
                this.compareObjectsInternal(both, groupByKeyLeftsMap.get(both), groupByKeyRightsMap.get(both), breaks);
            }
        }
    }

    private void compareObjectsInternal(Object key, Object left, Object right, MutableCollection<CompareBreak> breaks) {
        Class objectClass = left.getClass();

        MutableCollection<ClassCompareInfo> classCompareInfos = this.getClassCompareInfos(objectClass);

        if (classCompareInfos.isEmpty()) {
            if (!ObjectUtils.equals(left, right)) {
                breaks.add(new FieldCompareBreak(objectClass, key, left, right, "this", left, right));
            }
        } else {
            for (ClassCompareInfo<Object> classCompareInfo : classCompareInfos) {
                for (Pair<String, Function<Object, ?>> functionPair : classCompareInfo.getCompareFunctions()) {
                    Function<Object, ?> function = functionPair.getTwo();
                    Object leftFuncVal = function.valueOf(left);
                    Object rightFuncVal = function.valueOf(right);

                    if (leftFuncVal == null && rightFuncVal == null) {
                        continue;  // no break - continue
                    } else if (leftFuncVal == null ^ rightFuncVal == null) {  // XOR - if one of these is null, but not
                        // the other
                        breaks.add(new FieldCompareBreak(objectClass, key, left, right, functionPair.getOne(),
                                leftFuncVal, rightFuncVal));
                    } else {
                        MutableCollection<ClassCompareInfo> funcClassCompareInfos = this.getClassCompareInfos(leftFuncVal
                                .getClass());

                        if (funcClassCompareInfos.isEmpty()) {
                            if (!ObjectUtils.equals(leftFuncVal, rightFuncVal)) {
                                breaks.add(new FieldCompareBreak(objectClass, key, left, right, functionPair.getOne(),
                                        leftFuncVal, rightFuncVal));
                            }
                        } else {
                            this.compareObjectsInternal(key, leftFuncVal, rightFuncVal, breaks);
                        }
                    }
                }

                for (CollectionFieldCompareInfo collectionCompareInfo : classCompareInfo.getCollectionComparisonInfos()) {
                    this.compareCollectionsInternal(collectionCompareInfo.getElementClass()
                            , (Collection) collectionCompareInfo.getCollectionFieldFunction().valueOf(left)
                            , (Collection) collectionCompareInfo.getCollectionFieldFunction().valueOf(right)
                            , breaks);
                }
            }
        }
    }

    private MutableCollection<ClassCompareInfo> getClassCompareInfos(final Class clazz) {
        if (!this.classCompareInfoMap.containsKey(clazz)) {
            // We may have defined the comparison on a generalization (interface or superclass), so we check if there
            // are any compatible classes to check
            RichIterable<Class> realizedClasses = this.classCompareInfoMap.keysView().select(new Predicate<Class>() {
                @Override
                public boolean accept(Class each) {
                    return each.isAssignableFrom(clazz);
                }
            });

            RichIterable<ClassCompareInfo> realizedClassCompareInfos = realizedClasses
                    .flatCollect(new Function<Class, MutableCollection<ClassCompareInfo>>() {
                        @Override
                        public MutableCollection<ClassCompareInfo> valueOf(Class realizedClass) {
                            return DeepCompareUtil.this.classCompareInfoMap.get(realizedClass);
                        }
                    });

            this.classCompareInfoMap.putAll(clazz, realizedClassCompareInfos.toList());
        }
        return this.classCompareInfoMap.get(clazz);
    }
}
