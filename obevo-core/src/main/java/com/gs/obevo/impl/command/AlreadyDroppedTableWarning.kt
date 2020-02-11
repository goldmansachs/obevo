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

class AlreadyDroppedTableWarning(private val source: Change) : ChangeCommandWarning {
    override val commandDescription: String
        get() = "Change/Object should be deleted from your source code: object file " + source.objectKey + " specified a DROP command at the end, but the object was already not deployed to this schema - " + source.displayString

    override val isFatal: Boolean
        get() = false

    override val changes: List<Change>
        get() = listOf(this.source)
}
