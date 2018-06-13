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
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.ObjectKey
import org.apache.commons.lang3.builder.ToStringBuilder

class ChangePair(val changeKey: ChangeKey) {
    var sourceChange: Change? = null
        set(value) {
            field?.let {
                throw IllegalArgumentException(
                        String.format("sourceChange field could not be set again - something wrong w/ your keys:\n" +
                                "Source Artifact 1 [%s] at location [%s]\n" +
                                "Source Artifact 2 [%s] at location [%s]\n",
                                value?.displayString, value?.changeInput,
                                it.displayString, it.changeInput))
            }

            field = value
        }

    var deployedChange: Change? = null
        set(value) {
            field?.let {
                throw IllegalArgumentException(
                        "deployed field could not be set again - something wrong w/ your keys:\n   "
                                + value?.displayString + "\nvs: " + it.displayString)
            }

            field = value
        }

    val objectKey: ObjectKey?
        get() = artifact?.objectKey

    private val artifact: Change?
        get() = deployedChange ?: sourceChange

    /**
     * Used during tests.
     */
    constructor(changeKey: ChangeKey, sourceChange: Change?, deployedChange: Change?) : this(changeKey) {
        this.deployedChange = deployedChange
        this.sourceChange = sourceChange
    }

    /**
     * Used during tests.
     */
    constructor(sourceChange: Change?, deployedChange: Change?) : this((sourceChange ?: deployedChange)?.changeKey!!) {
        this.deployedChange = deployedChange
        this.sourceChange = sourceChange
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("sourceChange", sourceChange)
                .append("deployedChange", deployedChange)
                .toString()
    }
}
