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
package com.gs.obevo.impl.reader

import com.gs.obevo.api.appdata.doc.TextMarkupDocument
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection
import com.gs.obevo.api.platform.DeployMetrics
import com.gs.obevo.impl.DeployMetricsCollector
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples

/**
 * Common baseline for the DbChangeFileParsers with common implementation methods.
 */
abstract class AbstractDbChangeFileParser(
        private val backwardsCompatibleMode: Boolean,
        private val deployMetricsCollector: DeployMetricsCollector,
        private val allowedAttrs: ImmutableSet<String>,
        private val allowedToggles: ImmutableSet<String>,
        private val disallowedSectionNames: ImmutableSet<String>,
        private val textMarkupDocumentReader: TextMarkupDocumentReader
) : DbChangeFileParser {

    fun getPermissionSchemeValue(doc: TextMarkupDocument): String? {
        return doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA)?.getAttr(TextMarkupDocumentReader.TAG_PERM_SCHEME)
    }

    fun readDocument(fileContent: String, packageMetadata: TextMarkupDocumentSection?): ObjectBooleanPair<TextMarkupDocument> {
        try {
            val doc = textMarkupDocumentReader.parseString(fileContent, packageMetadata)
            validateStructureNew(doc)
            validateAttributes(doc)
            return PrimitiveTuples.pair(doc, false)
        } catch (newExc: RuntimeException) {
            return if (backwardsCompatibleMode) {
                try {
                    val doc = TextMarkupDocumentReaderOld().parseString(fileContent, packageMetadata)
                    validateStructureOld(doc)
                    deployMetricsCollector.addListMetric(DeployMetrics.BAD_FILE_FORMAT_WARNINGS, newExc)
                    PrimitiveTuples.pair(doc, true)
                } catch (oldExc: RuntimeException) {
                    throw RuntimeException("Failed to parse the file in both the old and new formats:\nOld: " + oldExc.message + "\nNew: " + newExc.message + "\nStack Trace: ", newExc)
                }
            } else {
                throw newExc
            }
        }
    }

    protected abstract fun validateStructureOld(doc: TextMarkupDocument)

    protected abstract fun validateStructureNew(doc: TextMarkupDocument)

    private fun validateAttributes(doc: TextMarkupDocument) {
        val disallowedSections = doc.sections.map { it.name }.filter(disallowedSectionNames::contains)

        if (disallowedSections.isNotEmpty()) {
            throw IllegalArgumentException("Found these disallowed sections: $disallowedSections")
        }

        doc.sections.each { section ->
            val disallowedAttrs = section.attrs.keysView().toSet().subtract(allowedAttrs)
            val disallowedToggles = section.toggles.subtract(allowedToggles)

            val errorMessages = mutableListOf<String>()
            if (disallowedAttrs.isNotEmpty()) {
                errorMessages.add("Following attributes are not allowed in the " + section.name + " section: " + disallowedAttrs)
            }
            if (disallowedToggles.isNotEmpty()) {
                errorMessages.add("Following toggles are not allowed in the " + section.name + " section: " + disallowedToggles)
            }
            if (errorMessages.isNotEmpty()) {
                throw IllegalArgumentException("Found " + errorMessages.size + " errors in the input: " + errorMessages.joinToString(";;; "))
            }
        }
    }

    fun getOrCreateMetadataNode(doc: TextMarkupDocument): TextMarkupDocumentSection {
        return doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_METADATA)
                ?: TextMarkupDocumentSection(TextMarkupDocumentReader.TAG_METADATA, null)
    }
}
