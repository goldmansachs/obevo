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
package com.gs.obevo.impl

import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.factory.Lists

class Changeset(
        val inserts: ImmutableList<ExecuteChangeCommand>,
        val deferredChanges: ImmutableList<ExecuteChangeCommand>,
        val auditChanges: RichIterable<AuditChangeCommand>,
        val changeWarnings: RichIterable<ChangeCommandWarning>) {

    constructor(inserts: ImmutableList<ExecuteChangeCommand>,
                auditChanges: RichIterable<AuditChangeCommand>,
                changeWarnings: RichIterable<ChangeCommandWarning>
    ) : this(inserts, Lists.immutable.empty(), auditChanges, changeWarnings)

    val isDeploymentNeeded: Boolean
        get() = !this.inserts.isEmpty || !this.auditChanges.isEmpty

    fun applyDeferredPredicate(deferredChangePredicate: Predicate<in ExecuteChangeCommand>?): Changeset {
        val partition = inserts.partition(deferredChangePredicate ?: DEFAULT_DEFERRED_PREDICATE);
        return Changeset(partition.selected, deferredChanges.newWithAll(partition.rejected), auditChanges, changeWarnings)
    }

    fun validateForDeployment() {
        val fatalWarnings = this.changeWarnings.filter { it.isFatal }

        if (!fatalWarnings.isEmpty()) {
            // check for serious exceptions
            throw IllegalArgumentException("Found exceptions:\n" + fatalWarnings.map { it.commandDescription }.joinToString("\n"))
        }
    }

    companion object {
        /**
         * By default, we will always defer those predicates marked w/ the changeset attribute as the existence of that
         * indicates something that may not be good to run alongside a regular release.
         */
        private val DEFAULT_DEFERRED_PREDICATE: Predicate<in ExecuteChangeCommand> = Predicate { command -> command.changes.any { it.changeset == null } }
    }
}
