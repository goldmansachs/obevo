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

import com.gs.obevo.impl.ExecuteChangeCommand
import org.eclipse.collections.api.list.ImmutableList

/**
 * Strategy interface defining how to sort the changes to deploy.
 */
interface ChangeCommandSorter {
    /**
     * Sorts the given commands into an order appropriate for deployment. May differ based on the impact of rollback.
     */
    fun sort(changeCommands: Iterable<ExecuteChangeCommand>, rollback: Boolean): ImmutableList<ExecuteChangeCommand>
}
