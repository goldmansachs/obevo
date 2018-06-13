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
package com.gs.obevo.impl.changepredicate

import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder.*
import com.gs.obevo.util.VisibleForTesting
import com.gs.obevo.util.lookuppredicate.LookupPredicateBuilder
import org.eclipse.collections.api.block.function.Function
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.collection.ImmutableCollection
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.factory.Predicates
import org.eclipse.collections.impl.block.factory.Predicates.attributePredicate
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.list.fixed.ArrayAdapter

/**
 * Predicate to allow clients to only select specific Changes based on the identity fields, e.g. schema, change type,
 * object name, and change name.
 *
 * Any combination of those fields can be applied here. See the unit test for examples.
 *
 * TODO more docs to come here.
 */
object ChangeKeyPredicateBuilder {
    @JvmStatic
    fun newBuilder(): ChangeKeyInclusionPredicateBuilder {
        return ChangeKeyInclusionPredicateBuilder()
    }

    @JvmStatic
    fun parseFullPredicate(fullPredicateString: String): Predicate<ChangeKey> {
        val fullPredicateParts = fullPredicateString.split(PREDICATE_SPLITTER.toRegex())
        val singlePredicates = fullPredicateParts.map { parseSinglePredicate(it) }

        return Predicates.or(singlePredicates)
    }

    @VisibleForTesting
    @JvmStatic
    fun parseSinglePredicate(singlePredicateString: String): Predicate<ChangeKey> {
        val changeParts = ArrayAdapter.adapt(*singlePredicateString.split(SINGLE_PREDICATE_SPLITTER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        if (changeParts.size > 4) {
            throw IllegalArgumentException("Cannot have more than 4 parts here (i.e. splits via the tilde ~)")
        }

        val schemas = if (changeParts.size > 0) parseSinglePredicatePart(changeParts[0]) else null
        val changeTypes = if (changeParts.size > 1) parseSinglePredicatePart(changeParts[1]) else null
        val objectNames = if (changeParts.size > 2) parseSinglePredicatePart(changeParts[2]) else null
        val changeNames = if (changeParts.size > 3) parseSinglePredicatePart(changeParts[3]) else null

        return newBuilder()
                .setSchemas(schemas)
                .setChangeTypes(changeTypes)
                .setObjectNames(objectNames)
                .setChangeNames(changeNames)
                .build()
    }

    private fun parseSinglePredicatePart(predicateString: String): ImmutableList<String> {
        return ArrayAdapter.adapt(*predicateString.split(PART_SPLITTER.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).toImmutable()
    }

    class ChangeKeyInclusionPredicateBuilder internal constructor() {
        private var schemas: ImmutableCollection<String>? = null
        private var changeTypes: ImmutableCollection<String>? = null
        private var objectNames: ImmutableCollection<String>? = null
        private var changeNames: ImmutableCollection<String>? = null

        fun setSchemas(vararg schemas: String): ChangeKeyInclusionPredicateBuilder {
            return this.setSchemas(Lists.immutable.with(*schemas))
        }

        internal fun setSchemas(schemas: ImmutableCollection<String>?): ChangeKeyInclusionPredicateBuilder {
            this.schemas = schemas
            return this
        }

        fun setChangeTypes(vararg changeTypes: String): ChangeKeyInclusionPredicateBuilder {
            return this.setChangeTypes(Lists.immutable.with(*changeTypes))
        }

        fun setChangeTypes(changeTypes: ImmutableCollection<String>?): ChangeKeyInclusionPredicateBuilder {
            this.changeTypes = changeTypes
            return this
        }

        fun setObjectNames(vararg objectNames: String): ChangeKeyInclusionPredicateBuilder {
            return this.setObjectNames(Lists.immutable.with(*objectNames))
        }

        fun setObjectNames(objectNames: ImmutableCollection<String>?): ChangeKeyInclusionPredicateBuilder {
            this.objectNames = objectNames
            return this
        }

        fun setChangeNames(vararg changeNames: String): ChangeKeyInclusionPredicateBuilder {
            return this.setChangeNames(Lists.immutable.with(*changeNames))
        }

        internal fun setChangeNames(changeNames: ImmutableCollection<String>?): ChangeKeyInclusionPredicateBuilder {
            this.changeNames = changeNames
            return this
        }

        fun build(): Predicate<ChangeKey> {
            return attributePredicate(Function<ChangeKey, String> { it.objectKey.schema }, LookupPredicateBuilder.convert(schemas))
                    .and(attributePredicate(Function { it.changeType.name }, LookupPredicateBuilder.convert(changeTypes)))
                    .and(attributePredicate(Function { it.objectKey.objectName }, LookupPredicateBuilder.convert(objectNames)))
                    .and(attributePredicate(Function { it.changeName }, LookupPredicateBuilder.convert(changeNames)))
        }
    }
}
