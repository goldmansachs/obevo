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

/**
 * Apply this interface to objects to make it possible to determine their dependencies from their text code using a
 * [TextDependencyExtractor].
 */
interface TextDependencyExtractable {
    /**
     * The object's identity. The [TextDependencyExtractor] would use the names from the identities as the
     * dependencies to try to extract from the text (see [.getContentForDependencyCalculation].
     *
     * @since 7.0.0
     */
    val objectName: String

    /**
     * The object's dependencies. Note that this may be set beforehand; if so, then no additional dependency calculation
     * would be done.
     *
     * @since 6.4.0
     */
    val codeDependencies: ImmutableSet<CodeDependency>

    /**
     * The dependencies to exclude from the text. This is to let the user specify the false-positive dependencies that
     * the [TextDependencyExtractor] may extract.
     *
     * @since 6.0.0
     */
    val excludeDependencies: ImmutableSet<String>

    /**
     * The dependencies to force-include. This is to let the user specify the false-negative dependencies that
     * the [TextDependencyExtractor] may omit.
     *
     * @since 6.0.0
     */
    val includeDependencies: ImmutableSet<String>

    /**
     * The content that would be analyzed by the [TextDependencyExtractor] for dependencies.
     *
     * @since 6.0.0
     */
    val contentForDependencyCalculation: String
}
