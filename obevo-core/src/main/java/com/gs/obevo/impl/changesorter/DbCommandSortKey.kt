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
package com.gs.obevo.impl.changesorter

import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.impl.ExecuteChangeCommand
import com.gs.obevo.impl.graph.SortableDependency
import com.gs.obevo.impl.graph.SortableDependencyGroup
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.factory.Sets

/**
 * Represents a node in the graph to be sorted.
 */
internal class DbCommandSortKey(val changeCommand: ExecuteChangeCommand) : SortableDependencyGroup {
    val changeType: ChangeType
    val objectName: String
    var order: Int = 0

    init {
        val candidateChange = changeCommand.changes.first()
        this.changeType = candidateChange.changeType
        this.objectName = candidateChange.objectName
    }

    override fun getComponents(): ImmutableSet<SortableDependency> {
        return Sets.immutable.ofAll(changeCommand.changes.toSet())
    }

    override fun toString(): String {
        return "DbCommandSortKey{" +
                "changeCommand=" + changeCommand +
                ", changeType=" + changeType +
                ", objectName='" + objectName + '\''.toString() +
                ", order=" + order +
                '}'.toString()
    }
}
