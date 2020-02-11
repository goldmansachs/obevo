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
package com.gs.obevo.api.platform

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.appdata.Environment

/**
 * Represents a unit of work that will be invoked against an [Environment].
 *
 * This will encapsulate a [Change]; the variations here are around whether we do a regular
 * deploy, an undeploy, an audit-only change, or to emit warnings/exceptions.
 */
interface ChangeCommand {
    /**
     * Returns the Changes that are involved in this command.
     */
    val changes: List<Change>

    /**
     * Friendly-text of the command to display for end-users.
     */
    val commandDescription: String
}
