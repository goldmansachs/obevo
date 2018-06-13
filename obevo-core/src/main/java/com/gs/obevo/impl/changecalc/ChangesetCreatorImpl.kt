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
package com.gs.obevo.impl.changecalc

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.platform.ChangePair
import com.gs.obevo.impl.*
import com.gs.obevo.impl.changesorter.ChangeCommandSorter
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter
import org.eclipse.collections.impl.factory.Lists

class ChangesetCreatorImpl(
        private val changeCommandSorter: ChangeCommandSorter,
        private val changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry
) : ChangesetCreator {
    override fun determineChangeset(changePairs: Iterable<ChangePair>, fromSourceList: RichIterable<Change>, initAllowedOnHashExceptions: Boolean): Changeset {
        val changePairsByChangeType = changePairs.groupBy { it.changeKey.changeType }

        val commands = changePairsByChangeType.flatMap { (changeType, changeTypePairs) ->
            changeTypeBehaviorRegistry.getChangeTypeSemantic(changeType.name).changeTypeCalculator.calculateCommands(changeType, CollectionAdapter.adapt(changeTypePairs), fromSourceList, initAllowedOnHashExceptions)
        }

        val executePartition = commands.partition { it is ExecuteChangeCommand }
        val auditPartition = executePartition.second.partition { it is AuditChangeCommand }
        val warningPartition = auditPartition.second.partition { it is ChangeCommandWarning }

        if (warningPartition.second.isNotEmpty()) {
            throw IllegalStateException("These changes are not of an expected class type: " + warningPartition.second)
        }

        val changeCommands = changeCommandSorter.sort(executePartition.first as Iterable<ExecuteChangeCommand>)

        return Changeset(Lists.immutable.ofAll(changeCommands),
                Lists.immutable.ofAll(auditPartition.first as Iterable<AuditChangeCommand>),
                Lists.immutable.ofAll(warningPartition.first as Iterable<ChangeCommandWarning>)
        )
    }
}
