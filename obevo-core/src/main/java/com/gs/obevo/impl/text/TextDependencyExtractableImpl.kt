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
package com.gs.obevo.impl.text

import com.gs.obevo.api.appdata.CodeDependency
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.factory.Sets

/**
 * Stand-alone pojo for representing data to have dependencies extracted.
 */
class TextDependencyExtractableImpl<T> (
        override val objectName: String,
        override val contentForDependencyCalculation: String,
        val payload: T
) : TextDependencyExtractable {
    override val codeDependencies: ImmutableSet<CodeDependency>
        get() = Sets.immutable.empty()

    override val excludeDependencies: ImmutableSet<String>
        get() = Sets.immutable.empty()

    override val includeDependencies: ImmutableSet<String>
        get() = Sets.immutable.empty()
}
