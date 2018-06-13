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
package com.gs.obevo.api.appdata

import com.gs.obevo.api.platform.ChangeType

/**
 * The fields that consist the identity of an individual change within a client codebase.
 * (Objects can have multiple changes).
 */
data class ChangeKey(val objectKey: ObjectKey, val changeName: String) {
    constructor(schema: String, changeType: ChangeType, objectName: String, changeName: String) : this(ObjectKey(schema, objectName, changeType), changeName)

    val changeType: ChangeType
        get() = objectKey.changeType
}
