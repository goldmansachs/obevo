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

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.platform.ChangePair
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.block.procedure.Procedure2

/**
 * Calculates the changes that would need to be applied to the environment given the source vs. deployed changes.
 * The returned Changeset should have all the change information, including providing the execution commands in the
 * right order.
 */
interface ChangesetCreator {
    /**
     * See [ChangesetCreator] javadoc.
     */
    fun determineChangeset(changePairs: Iterable<ChangePair>, fromSourceList: RichIterable<Change>, initAllowedOnHashExceptions: Boolean): Changeset

    companion object {
        fun getChangePairs(deploys: RichIterable<Change>, sources: RichIterable<Change>): Collection<ChangePair> {
            val changes = mutableMapOf<ChangeKey, ChangePair>()
            val addChangeToMap = Procedure2<Change, Boolean> { change, fromSource ->
                val changePair = changes.getOrPut(change.changeKey, { ChangePair(change.changeKey) })

                if (fromSource) {
                    changePair.sourceChange = change
                } else {
                    changePair.deployedChange = change
                }
            }
            sources.forEachWith(addChangeToMap, true)
            deploys.forEachWith(addChangeToMap, false)

            return changes.values
        }
    }
}
