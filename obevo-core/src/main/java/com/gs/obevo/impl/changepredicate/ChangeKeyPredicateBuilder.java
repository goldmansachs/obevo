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
package com.gs.obevo.impl.changepredicate;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.lookuppredicate.LookupPredicateBuilder;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

import static com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder.PART_SPLITTER;
import static com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder.PREDICATE_SPLITTER;
import static com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder.SINGLE_PREDICATE_SPLITTER;
import static org.eclipse.collections.impl.block.factory.Predicates.attributePredicate;

/**
 * Predicate to allow clients to only select specific Changes based on the identity fields, e.g. schema, change type,
 * object name, and change name.
 *
 * Any combination of those fields can be applied here. See the unit test for examples.
 *
 * TODO more docs to come here.
 */
public class ChangeKeyPredicateBuilder {
    public static ChangeKeyInclusionPredicateBuilder newBuilder() {
        return new ChangeKeyInclusionPredicateBuilder();
    }

    public static Predicate<? super Change> parseFullPredicate(String fullPredicateString) {
        ImmutableList<String> fullPredicateParts = ArrayAdapter.adapt(fullPredicateString.split(PREDICATE_SPLITTER)).toImmutable();
        ImmutableList<Predicate<? super Change>> singlePredicates = fullPredicateParts.collect(ChangeKeyPredicateBuilder::parseSinglePredicate);

        return Predicates.or(singlePredicates);
    }

    @VisibleForTesting
    static Predicate<? super Change> parseSinglePredicate(String singlePredicateString) {
        MutableList<String> changeParts = ArrayAdapter.adapt(singlePredicateString.split(SINGLE_PREDICATE_SPLITTER));
        if (changeParts.size() > 4) {
            throw new IllegalArgumentException("Cannot have more than 4 parts here (i.e. splits via the tilde ~)");
        }

        ImmutableList<String> schemas = changeParts.size() > 0 ? parseSinglePredicatePart(changeParts.get(0)) : null;
        ImmutableList<String> changeTypes = changeParts.size() > 1 ? parseSinglePredicatePart(changeParts.get(1)) : null;
        ImmutableList<String> objectNames = changeParts.size() > 2 ? parseSinglePredicatePart(changeParts.get(2)) : null;
        ImmutableList<String> changeNames = changeParts.size() > 3 ? parseSinglePredicatePart(changeParts.get(3)) : null;

        return newBuilder()
                .setSchemas(schemas)
                .setChangeTypes(changeTypes)
                .setObjectNames(objectNames)
                .setChangeNames(changeNames)
                .build();
    }

    private static ImmutableList<String> parseSinglePredicatePart(String predicateString) {
        return ArrayAdapter.adapt(predicateString.split(PART_SPLITTER)).toImmutable();
    }

    public static class ChangeKeyInclusionPredicateBuilder {
        private ImmutableCollection<String> schemas;
        private ImmutableCollection<String> changeTypes;
        private ImmutableCollection<String> objectNames;
        private ImmutableCollection<String> changeNames;

        private ChangeKeyInclusionPredicateBuilder() {
        }

        public ChangeKeyInclusionPredicateBuilder setSchemas(String... schemas) {
            return this.setSchemas(Lists.immutable.with(schemas));
        }

        ChangeKeyInclusionPredicateBuilder setSchemas(ImmutableCollection<String> schemas) {
            this.schemas = schemas;
            return this;
        }

        public ChangeKeyInclusionPredicateBuilder setChangeTypes(String... changeTypes) {
            return this.setChangeTypes(Lists.immutable.with(changeTypes));
        }

        public ChangeKeyInclusionPredicateBuilder setChangeTypes(ImmutableCollection<String> changeTypes) {
            this.changeTypes = changeTypes;
            return this;
        }

        public ChangeKeyInclusionPredicateBuilder setObjectNames(String... objectNames) {
            return this.setObjectNames(Lists.immutable.with(objectNames));
        }

        public ChangeKeyInclusionPredicateBuilder setObjectNames(ImmutableCollection<String> objectNames) {
            this.objectNames = objectNames;
            return this;
        }

        public ChangeKeyInclusionPredicateBuilder setChangeNames(String... changeNames) {
            return this.setChangeNames(Lists.immutable.with(changeNames));
        }

        ChangeKeyInclusionPredicateBuilder setChangeNames(ImmutableCollection<String> changeNames) {
            this.changeNames = changeNames;
            return this;
        }

        public Predicate<? super Change> build() {
            return attributePredicate(Change::getSchema, LookupPredicateBuilder.convert(schemas))
                    .and(attributePredicate(_this -> _this.getChangeType().getName(), LookupPredicateBuilder.convert(changeTypes)))
                    .and(attributePredicate(Change::getObjectName, LookupPredicateBuilder.convert(objectNames)))
                    .and(attributePredicate(Change::getChangeName, LookupPredicateBuilder.convert(changeNames)));
        }
    }
}
