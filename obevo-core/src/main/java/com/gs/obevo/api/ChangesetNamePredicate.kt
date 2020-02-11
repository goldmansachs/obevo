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
package com.gs.obevo.api

import com.gs.obevo.api.platform.ChangeCommand
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.set.ImmutableSet

/**
 * Convenience predicate to select a ChangeCommand based on the parameter changeset names passed in.
 */
class ChangesetNamePredicate(private val changesetNames: ImmutableSet<String>) : Predicate<ChangeCommand> {
    override fun accept(command: ChangeCommand): Boolean {
        return command.changes.any { it.changeset != null && changesetNames.contains(it.changeset) }
    }
}
