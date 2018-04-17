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
package com.gs.obevo.util.lookuppredicate;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.PartitionIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringPredicates;
import org.eclipse.collections.impl.factory.Lists;

/**
 * Utility class to build efficient predicates based off a comma-separated list of values that can have parts which
 * are either direct name lookups or wildcard searches (e.g. abc%def). We'd like to enforce this common format across
 * all platform implementations.
 *
 * Some refactoring still needed in the rest of the code to achieve this goal...
 */
public class LookupPredicateBuilder {
    public static <T> Predicates<? super T> convert(Function<? super T, String> typeFunction, ImmutableCollection<String> patterns) {
        if (patterns.isEmpty()) {
            return Predicates.alwaysTrue();
        } else {
            return Predicates.attributePredicate(typeFunction, convert(patterns));
        }
    }

    public static Predicates<? super String> convert(ImmutableCollection<String> patterns) {
        if (patterns == null) {
            return Predicates.alwaysTrue();
        }
        PartitionIterable<String> wildcardPartition = patterns.partition(Predicates.or(StringPredicates.contains("*"), StringPredicates.contains("%")));
        RichIterable<String> wildcardPatterns = wildcardPartition.getSelected();
        RichIterable<WildcardPatternIndex> wildcardPatternIndexes = wildcardPatterns.collect(WildcardPatternIndex::new);

        RichIterable<String> lookupPatterns = wildcardPartition.getRejected();
        LookupIndex lookupIndex = lookupPatterns.notEmpty() ? new LookupIndex(lookupPatterns.toSet().toImmutable()) : null;

        MutableList<Index> indexes = Lists.mutable.empty();
        if (lookupIndex != null) {
            indexes.add(lookupIndex);
        }
        indexes.withAll(wildcardPatternIndexes);

        return Predicates.or(indexes);
    }
}
