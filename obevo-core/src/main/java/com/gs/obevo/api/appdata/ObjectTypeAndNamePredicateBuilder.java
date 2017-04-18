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
package com.gs.obevo.api.appdata;

import com.gs.obevo.util.lookuppredicate.LookupPredicateBuilder;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;

/**
 * Predicate builder that works off the object type and object name. Can work on any kind of object; just needs
 * functions passed in from the target class to filter on that maps to the object type and name values.
 */
public class ObjectTypeAndNamePredicateBuilder {
    public enum FilterType {
        INCLUDE(false),
        EXCLUDE(true),;

        private final boolean emptyInputResult;

        FilterType(boolean emptyInputResult) {
            this.emptyInputResult = emptyInputResult;
        }

        public boolean isEmptyInputResult() {
            return emptyInputResult;
        }
    }

    private final ImmutableMultimap<String, String> objectNamesByType;
    private final FilterType filterType;

    public ObjectTypeAndNamePredicateBuilder(FilterType filterType) {
        this(Multimaps.immutable.set.<String, String>empty(), filterType);
    }

    public ObjectTypeAndNamePredicateBuilder(ImmutableMultimap<String, String> objectNamesByType, FilterType filterType) {
        this.objectNamesByType = Validate.notNull(objectNamesByType);
        this.filterType = filterType;
    }

    public ObjectTypeAndNamePredicateBuilder add(ImmutableMultimap<String, String> otherObjectNamesByType) {
        if (otherObjectNamesByType == null) {
            return this;
        }

        MutableSetMultimap<String, String> empty = Multimaps.mutable.set.empty();
        empty.putAll(this.objectNamesByType);
        empty.putAll(otherObjectNamesByType);
        return new ObjectTypeAndNamePredicateBuilder(empty.toImmutable(), this.filterType);
    }

    /**
     * Returns a new builder instance that combines the filter parameters from the current instances and the parameters
     * from the other input builder. The existing instance remains unchanged.
     */
    public ObjectTypeAndNamePredicateBuilder add(ObjectTypeAndNamePredicateBuilder other) {
        if (other == null) {
            return this;
        }
        if (!this.filterType.equals(other.filterType)) {
            throw new IllegalArgumentException("Filter types must match if we want to combine the builders; this: " + filterType + "; other: " + other.filterType);
        }

        return add(other.objectNamesByType);
    }

    /**
     * Only exposed for testing and debugging.
     */
    public ImmutableMultimap<String, String> getObjectNamesByType() {
        return objectNamesByType;
    }

    /**
     * Builds the predicate on object type and name based on the input functions passed in.
     */
    public <T> Predicates<? super T> build(final Function<? super T, String> objectTypeFunction, final Function<? super T, String> objectNameFunction) {
        if (objectNamesByType.isEmpty()) {
            if (filterType.isEmptyInputResult()) {
                return Predicates.alwaysTrue();
            } else {
                return Predicates.alwaysFalse();
            }
        }

        RichIterable<Predicate<? super T>> typePredicates = objectNamesByType.keyMultiValuePairsView().collect(new Function<Pair<String, RichIterable<String>>, Predicate<? super T>>() {
            @Override
            public Predicate<? super T> valueOf(Pair<String, RichIterable<String>> pair) {
                String objectType = pair.getOne();
                RichIterable<String> objectPatterns = pair.getTwo();
                Predicate<T> objectTypeAndNamePredicate = getObjectTypeAndNamePredicate(
                        objectTypeFunction, Lists.immutable.with(objectType),
                        objectNameFunction, objectPatterns.toList().toImmutable()
                );

                if (filterType == FilterType.EXCLUDE) {
                    objectTypeAndNamePredicate = Predicates.not(objectTypeAndNamePredicate);
                }
                return objectTypeAndNamePredicate;
            }
        });

        if (filterType == FilterType.EXCLUDE) {
            return Predicates.and(typePredicates);
        } else {
            return Predicates.or(typePredicates);
        }
    }

    private static <T> Predicate<T> getObjectTypeAndNamePredicate(
            Function<? super T, String> typeFunction, ImmutableCollection<String> typePatterns,
            Function<? super T, String> nameFunction, ImmutableCollection<String> namePatterns) {
        Predicates<? super T> typePredicate = LookupPredicateBuilder.convert(typeFunction, typePatterns);
        Predicates<? super T> namePredicate = LookupPredicateBuilder.convert(nameFunction, namePatterns);
        return Predicates.and(typePredicate, namePredicate);
    }
}
