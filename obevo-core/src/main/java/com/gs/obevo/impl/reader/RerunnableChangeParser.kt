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

import com.gs.obevo.api.appdata.ChangeInput
import com.gs.obevo.api.appdata.ChangeKey
import com.gs.obevo.api.appdata.CodeDependency
import com.gs.obevo.api.appdata.CodeDependencyType
import com.gs.obevo.api.appdata.doc.TextMarkupDocument
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.impl.DeployMetricsCollector
import com.gs.obevo.impl.DeployMetricsCollectorImpl
import com.gs.obevo.util.VisibleForTesting
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy
import com.gs.obevo.util.vfs.FileObject
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.block.factory.Predicates
import org.eclipse.collections.impl.block.factory.StringFunctions
import org.eclipse.collections.impl.block.factory.StringPredicates
import org.eclipse.collections.impl.block.factory.primitive.IntPredicates
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Sets
import org.slf4j.LoggerFactory

class RerunnableChangeParser(backwardsCompatibleMode: Boolean, deployMetricsCollector: DeployMetricsCollector, textMarkupDocumentReader: TextMarkupDocumentReader) : AbstractDbChangeFileParser(backwardsCompatibleMode, deployMetricsCollector, rerunnableAttrs(), rerunnableToggles(), rerunnableDisallowedSections(), textMarkupDocumentReader) {
    private val contentHashStrategy = OldWhitespaceAgnosticDbChangeHashStrategy()

    @VisibleForTesting
    constructor() : this(false, DeployMetricsCollectorImpl(), TextMarkupDocumentReader(false)) {
    }

    override fun value(changeType: ChangeType, file: FileObject?, fileContent: String, objectName: String, schema: String, packageMetadata: TextMarkupDocumentSection?): ImmutableList<ChangeInput> {
        val docStatusPair = readDocument(fileContent, packageMetadata)
        val doc = docStatusPair.one
        val backwardsCompatibleModeUsed = docStatusPair.two

        val mainSection = getMainSection(doc, backwardsCompatibleModeUsed)
        val contentToUse = mainSection.content


        val dropCommandSection = doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_DROP_COMMAND)
        val metadata = getOrCreateMetadataNode(doc)
        val permissionScheme = getPermissionSchemeValue(doc)
        val change = createRerunnableChange(changeType, file, objectName, schema, "n/a", contentToUse, metadata, permissionScheme)
        change.dropContent = dropCommandSection?.content

        val bodySection = doc.findSectionWithElementName(TextMarkupDocumentReader.TAG_BODY)
        if (bodySection != null) {
            val bodyChangeType = changeType.bodyChangeType
                    ?: throw IllegalArgumentException("Cannot specify a //// " + TextMarkupDocumentReader.TAG_BODY + " section for an object type without a body type configured: [" + changeType + "]")
            val bodyChange = createRerunnableChange(bodyChangeType, file, objectName, schema, "body", bodySection.content, bodySection, permissionScheme)

            return Lists.immutable.of(change, bodyChange)
        } else {
            return Lists.immutable.of(change)
        }
    }

    private fun createRerunnableChange(changeType: ChangeType, file: FileObject?, objectName: String, schema: String, changeName: String, contentToUse: String, metadata: TextMarkupDocumentSection, permissionScheme: String?): ChangeInput {
//        val change = ChangeRerunnable(changeType, schema, objectName, changeName, contentHashStrategy.hashContent(contentToUse), contentToUse)
        val change = ChangeInput(true)
        change.changeKey = ChangeKey(schema, changeType, objectName, changeName)
        change.content = contentToUse
        change.contentHash = contentHashStrategy.hashContent(contentToUse)

        val restrictions = DbChangeRestrictionsReader().valueOf(metadata)
        change.setRestrictions(restrictions)

        val dependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_DEPENDENCIES)
        val excludeDependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES)
        val includeDependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES)

        if (dependenciesStr != null && (excludeDependenciesStr != null || includeDependenciesStr != null)) {
            throw IllegalArgumentException(String.format("Cannot specify the %1%s attribute with either the %2\$s or %3\$s attributes; either go w/ just %1\$s or a combo of %2\$s and %3\$s or neither",
                    TextMarkupDocumentReader.ATTR_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES))
        }

        if (dependenciesStr != null) {
            change.setCodeDependencies(Sets.immutable.with(*dependenciesStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).reject(StringPredicates.empty()).collectWith({ target, codeDependencyType -> CodeDependency(target, codeDependencyType) }, CodeDependencyType.EXPLICIT))
        } else {
            if (excludeDependenciesStr != null) {
                change.setExcludeDependencies(Sets.immutable.with(*excludeDependenciesStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).reject(StringPredicates.empty()))
            }
            if (includeDependenciesStr != null) {
                change.setIncludeDependencies(Sets.immutable.with(*includeDependenciesStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).reject(StringPredicates.empty()))
            }
        }

        val orderStr = metadata.getAttr(ATTR_ORDER)
        if (orderStr != null) {
            change.order = Integer.valueOf(orderStr)
        }

        change.permissionScheme = permissionScheme
        change.metadataSection = metadata

        change.setFileLocation(file)
        return change
    }

    override fun validateStructureOld(doc: TextMarkupDocument) {
        val foundSections = doc.sections.collect { section -> section.name }.toSet().reject(Predicates.isNull())
        val expectedSections = Sets.mutable.with(TextMarkupDocumentReader.TAG_METADATA, TextMarkupDocumentReader.TAG_BODY, TextMarkupDocumentReader.TAG_DROP_COMMAND)
        val extraSections = foundSections.difference(expectedSections)
        if (extraSections.notEmpty()) {
            throw IllegalArgumentException("Unexpected sections found: $extraSections; only expecting: $expectedSections")
        }
    }

    override fun validateStructureNew(doc: TextMarkupDocument) {
        val allowedSectionString = Sets.immutable.with(TextMarkupDocumentReader.TAG_METADATA, TextMarkupDocumentReader.TAG_BODY, TextMarkupDocumentReader.TAG_DROP_COMMAND).collect(StringFunctions.prepend("//// ")).makeString(", ")

        val docSections = doc.sections
        if (docSections.isEmpty) {
            throw IllegalArgumentException("No content defined")
        }

        val disallowedSections = docSections.reject(Predicates.attributeIn({ section -> section.name }, Sets.immutable.with<String>(null, TextMarkupDocumentReader.TAG_METADATA, TextMarkupDocumentReader.TAG_DROP_COMMAND, TextMarkupDocumentReader.TAG_BODY)))
        if (disallowedSections.notEmpty()) {
            throw IllegalArgumentException("Only allowed 1 content section and at most 1 of these [$allowedSectionString]; instead, found these disallowed sections: $disallowedSections")
        }

        val sectionNames = docSections.collect { section -> section.name }
        val duplicateSections = sectionNames.toBag().selectByOccurrences(IntPredicates.greaterThan(1))
        if (duplicateSections.notEmpty()) {
            throw IllegalArgumentException("Only allowed 1 content section and at most 1 of these [" + allowedSectionString + "]; instead, found these extra sections instances: " + duplicateSections.toSet())
        }

        val metadataIndex = sectionNames.indexOf(TextMarkupDocumentReader.TAG_METADATA)
        val contentIndex = sectionNames.indexOf(null)
        val dropIndexIndex = sectionNames.indexOf(TextMarkupDocumentReader.TAG_DROP_COMMAND)

        if (metadataIndex != -1 && contentIndex != -1 && metadataIndex > contentIndex) {
            throw IllegalArgumentException("Improper section ordering: " + TextMarkupDocumentReader.TAG_METADATA + " section must come before the content section")
        } else if (contentIndex != -1 && dropIndexIndex != -1 && contentIndex > dropIndexIndex) {
            throw IllegalArgumentException("Improper section ordering: content section must come before the " + TextMarkupDocumentReader.TAG_DROP_COMMAND + " section")
        }
    }

    private fun getMainSection(doc: TextMarkupDocument, backwardsCompatibleModeUsed: Boolean): TextMarkupDocumentSection {
        return if (backwardsCompatibleModeUsed) getMainSectionOld(doc) else getMainSectionNew(doc)!!
    }

    private fun getMainSectionNew(doc: TextMarkupDocument): TextMarkupDocumentSection? {
        return doc.findSectionWithElementName(null)
    }

    private fun getMainSectionOld(doc: TextMarkupDocument): TextMarkupDocumentSection {
        return doc.sections.get(0)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RerunnableChangeParser::class.java)
        private val ATTR_ORDER = "order"

        private fun rerunnableDisallowedSections(): ImmutableSet<String> {
            return Sets.immutable.with(TextMarkupDocumentReader.TAG_CHANGE)
        }

        private fun rerunnableToggles(): ImmutableSet<String> {
            return Sets.immutable.with(
                    TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS
            )
        }

        private fun rerunnableAttrs(): ImmutableSet<String> {
            return Sets.immutable.with(
                    TextMarkupDocumentReader.ATTR_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES,
                    TextMarkupDocumentReader.TAG_PERM_SCHEME,
                    TextMarkupDocumentReader.EXCLUDE_ENVS,
                    TextMarkupDocumentReader.EXCLUDE_PLATFORMS,
                    TextMarkupDocumentReader.INCLUDE_ENVS,
                    TextMarkupDocumentReader.INCLUDE_PLATFORMS,
                    TextMarkupDocumentReader.ATTR_UPDATE_TIME_COLUMN,
                    TextMarkupDocumentReader.ATTR_PRIMARY_KEYS,
                    ATTR_ORDER
            )
        }
    }
}
