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
import com.gs.obevo.impl.ChangeCommandWarning

class HashMismatchWarning(private val source: Change, private val deployed: Change) : ChangeCommandWarning {
    override val commandDescription: String
        get() = "Artifact Hash is mismatching for : " + this.source.displayString + ":\n" +
                "\tSourceVersion - " + this.source.contentHash + "\n" +
                "\tDeployed Version - " + this.deployed.contentHash + "\n" +
                "\tDeployed At - " + this.deployed?.deployExecution?.displayString + "\n" +
                "\t* Please remember not to edit already-deployed changes in place, " +
                "and instead add a new Change definition"

    override val isFatal: Boolean
        get() = true

    override val changes: List<Change>
        get() = listOf(this.source)
}
