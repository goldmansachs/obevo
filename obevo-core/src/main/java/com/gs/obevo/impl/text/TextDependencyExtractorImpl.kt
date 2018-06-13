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
import com.gs.obevo.api.appdata.CodeDependencyType
import java.util.regex.Pattern

/**
 * Standard implementation of [TextDependencyExtractor] going forward. Looks across all object types.
 */
class TextDependencyExtractorImpl(
        private val convertDbObjectName: (String) -> String
) : TextDependencyExtractor {
    constructor(myConvert: org.eclipse.collections.api.block.function.Function<String, String>) : this(myConvert::valueOf)

    override fun <T : TextDependencyExtractable> calculateDependencies(changes: Iterable<T>): Map<T, Set<CodeDependency>> {
        val objectNames = changes.map { it.objectName }.map(convertDbObjectName).toSet()

        return changes.associateBy({ it }, { change ->
            // note - only check for nulls here; we may set dependencies to blank explicitly in the overrides
            if (change.codeDependencies == null) {
                // we use getContentForDependencyCalculation() instead of just getContent() due to the staticData
                // objects needing to have its dependency calculated differently.

                // TODO go via objectNames and physicalSchema+objectName combo
                val discoveredDependencies = calculateDependencies(change.objectName, change.contentForDependencyCalculation, objectNames)
                val filteredDependencies = discoveredDependencies
                        .filterNot { change.objectName.equals(it, true) }  // TODO  GITHUB#153 normally would check with the convertDbObjectName, but needs backwards-compatibility for now
                        .filterNot { change.excludeDependencies.contains(it) }
                        .map { CodeDependency(it, CodeDependencyType.DISCOVERED) }

                filteredDependencies.plus(change.includeDependencies.map { CodeDependency(it, CodeDependencyType.EXPLICIT) }).toSet()
            } else {
                change.codeDependencies.toSet()
            }
        })
    }

    internal fun calculateDependencies(logMessage: String, content: String, objectNames: Set<String>): Set<String> {
        val tokens = CommentRemover.removeComments(content, logMessage).split(SPACE_PATTERN)
        return tokens.filter { objectNames.contains(convertDbObjectName(it)) }.toSet()
    }

    companion object {
        private val SPACE_PATTERN = Pattern.compile("\\W+", Pattern.DOTALL)
    }
}
