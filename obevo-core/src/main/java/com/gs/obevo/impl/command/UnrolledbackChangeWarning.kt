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
package com.gs.obevo.impl.command

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.ChangeIncremental
import com.gs.obevo.impl.ChangeCommandWarning

class UnrolledbackChangeWarning(private val incrementalDeployed: ChangeIncremental) : ChangeCommandWarning {
    override val commandDescription: String
        get() = "No longer in the source, but will be left in the DB as we are running in rollback mode: " + this.incrementalDeployed.displayString

    override val isFatal: Boolean
        get() = false

    override val changes: List<Change>
        get() = listOf(this.incrementalDeployed)
}
