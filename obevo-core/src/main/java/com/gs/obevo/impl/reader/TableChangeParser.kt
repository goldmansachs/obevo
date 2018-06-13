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

import com.gs.obevo.api.appdata.*
import com.gs.obevo.api.appdata.doc.TextMarkupDocument
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.impl.DeployMetricsCollector
import com.gs.obevo.impl.DeployMetricsCollectorImpl
import com.gs.obevo.util.Tokenizer
import com.gs.obevo.util.VisibleForTesting
import com.gs.obevo.util.hash.DbChangeHashStrategy
import com.gs.obevo.util.vfs.FileObject
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.api.map.ImmutableMap
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.block.factory.Functions
import org.eclipse.collections.impl.block.factory.StringPredicates
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Maps
import org.eclipse.collections.impl.factory.Sets
import org.eclipse.collections.impl.list.fixed.ArrayAdapter
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.tuple.Tuples
import org.eclipse.collections.impl.utility.StringIterate.trimmedTokensToList
import org.slf4j.LoggerFactory

class TableChangeParser(
        private val contentHashStrategy: DbChangeHashStrategy,
        backwardsCompatibleMode: Boolean,
        deployMetricsCollector: DeployMetricsCollector,
        textMarkupDocumentReader: TextMarkupDocumentReader,
        private val getChangeType: GetChangeType
) : AbstractDbChangeFileParser(backwardsCompatibleMode, deployMetricsCollector, tableAttributes(), tableToggles(), tableDisallowedSections(), textMarkupDocumentReader) {

    @VisibleForTesting
    constructor(contentHashStrategy: DbChangeHashStrategy, getChangeType: GetChangeType) : this(contentHashStrategy, false, DeployMetricsCollectorImpl(), TextMarkupDocumentReader(false), getChangeType) {
    }

    override fun value(tableChangeType: ChangeType, file: FileObject?, fileContent: String, nonTokenizedObjectName: String, schema: String, packageMetadata: TextMarkupDocumentSection?): ImmutableList<ChangeInput> {
        LOG.debug("Attempting to read file {}", file)

        val origDoc = readDocument(fileContent, packageMetadata).one

        val metadata = this.getOrCreateMetadataNode(origDoc)

        val templateParamAttr = metadata.getAttr(ATTR_TEMPLATE_PARAMS)

        // Handle a potential template object; this will return a dummy params list if this is not a template object
        val templateParamsList = convertToParamList(templateParamAttr)

        val fileToChangePairs = templateParamsList.map { templateParams ->
            val tokenizer = Tokenizer(templateParams, "\${", "}")

            val objectName = tokenizer.tokenizeString(nonTokenizedObjectName)
            val doc = if (templateParams.notEmpty())
                this@TableChangeParser.readDocument(tokenizer.tokenizeString(fileContent), packageMetadata).one
            else
                origDoc  /// if no template params, then save some effort and don't bother re-reading the doc from the string

            val fileLevelRestrictions = DbChangeRestrictionsReader().valueOf(metadata)
            val changes = doc.sections
                    .filter { TextMarkupDocumentReader.TAG_CHANGE == it.name }
                    .mapIndexed { i, section ->
                        val change = this@TableChangeParser.create(tableChangeType, section, schema, objectName, i)
                        val changeLevelRestrictions = DbChangeRestrictionsReader().valueOf(section)
                        change.setRestrictions(this@TableChangeParser.mergeRestrictions(fileLevelRestrictions, changeLevelRestrictions))
                        change.permissionScheme = this@TableChangeParser.getPermissionSchemeValue(doc)
                        change.setFileLocation(file)
                        change.metadataSection = metadata

                        val dependenciesStr = section.getAttr(TextMarkupDocumentReader.ATTR_DEPENDENCIES)
                        if (dependenciesStr != null) {
                            change.setCodeDependencies(Sets.immutable.with(*dependenciesStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).reject(StringPredicates.empty()).collectWith({ target, codeDependencyType -> CodeDependency(target, codeDependencyType) }, CodeDependencyType.EXPLICIT))
                        }

                        val excludeDependenciesStr = section.getAttr(TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES)
                        if (excludeDependenciesStr != null) {
                            change.setExcludeDependencies(Sets.immutable.with(*excludeDependenciesStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).reject(StringPredicates.empty()))
                        }

                        val includeDependenciesStr = metadata.getAttr(TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES)
                        if (includeDependenciesStr != null) {
                            change.setIncludeDependencies(Sets.immutable.with(*includeDependenciesStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).reject(StringPredicates.empty()))
                        }

                        change
                    }

            Tuples.pair(objectName, changes)
        }

        // Validate that if we had used templates, that it resulted in different file names
        val detemplatedObjectNames = fileToChangePairs.mapTo(Sets.mutable.empty(), { it.one })

        if (detemplatedObjectNames.size != templateParamsList.size()) {
            throw IllegalArgumentException("Expecting the usage of templates to result in a different file name per template set; expected " + templateParamsList.size() + " object names (from " + templateParamAttr + ") but found " + detemplatedObjectNames)
        }

        return Lists.immutable.ofAll(fileToChangePairs.flatMap { it.two })
    }

    private fun convertToParamList(templateParamAttr: String?): ImmutableList<ImmutableMap<String, String>> {
        if (templateParamAttr == null) {
            return Lists.immutable.of(Maps.immutable.empty())
        }

        val paramGroups = ArrayAdapter.adapt(*templateParamAttr.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()).toImmutable()
        return paramGroups.collect { paramGroup ->
            val paramStrs = paramGroup.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val params = Maps.mutable.empty<String, String>()
            for (paramStr in paramStrs) {
                val paramParts = paramStr.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                params[paramParts[0]] = paramParts[1]
            }

            params.toImmutable()
        }
    }

    override fun validateStructureNew(doc: TextMarkupDocument) {
        var docSections = doc.sections

        if (docSections.isEmpty || docSections.noneSatisfy { it -> TextMarkupDocumentReader.TAG_CHANGE == it.name }) {
            throw IllegalArgumentException("No //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections found; at least one is required")
        }

        if (!(TextMarkupDocumentReader.TAG_CHANGE == docSections.get(0).name || TextMarkupDocumentReader.TAG_METADATA == docSections.get(0).name)) {
            throw IllegalArgumentException("First content of the file must be the //// CHANGE line, " +
                    "or a //// " + TextMarkupDocumentReader.TAG_METADATA + " line, " +
                    "or just a blank line")
        }

        if (TextMarkupDocumentReader.TAG_METADATA == docSections.get(0).name) {
            docSections = docSections.subList(1, docSections.size())
        }

        val badSections = docSections.reject { it -> TextMarkupDocumentReader.TAG_CHANGE == it.name }
        if (badSections.notEmpty()) {
            throw IllegalArgumentException("File structure for incremental file must be optionally a //// " + TextMarkupDocumentReader.TAG_METADATA + " section followed only by //// " + TextMarkupDocumentReader.TAG_CHANGE + " sections. Instead, found this section in between: " + badSections)
        }
    }

    override fun validateStructureOld(doc: TextMarkupDocument) {
        val contentEmpty = StringUtils.isBlank(doc.sections.get(0).content)

        Validate.isTrue(doc.sections.get(0).name != null || contentEmpty,
                "First content of the file must be the //// CHANGE line, or a //// "
                        + TextMarkupDocumentReader.TAG_METADATA + " line, or just a blank")
    }

    /**
     * Merge file and change level restrictions by restriction class name
     */
    private fun mergeRestrictions(fileLevelRestrictions: ImmutableList<ArtifactRestrictions>, changeLevelRestrictions: ImmutableList<ArtifactRestrictions>): ImmutableList<ArtifactRestrictions> {
        return Lists.mutable.ofAll(fileLevelRestrictions)
                .withAll(changeLevelRestrictions)
                .groupBy(Functions.getToClass())
                .multiValuesView()
                .collect { artifactRestrictions -> artifactRestrictions.last }
                .toList().toImmutable()
    }

    private fun create(tableChangeType: ChangeType, section: TextMarkupDocumentSection, schema: String, objectName: String,
                       changeIndex: Int): ChangeInput {
        val changeType = getChangeType.getChangeType(section, tableChangeType)
        val drop = section.isTogglePresent(TOGGLE_DROP_TABLE)

        val changeName = section.getAttr(ATTR_NAME)
        Validate.notNull(changeName, "Must define name parameter")

        val baselinedChanges =
                if (section.getAttr(ATTR_BASELINED_CHANGES) == null)
                    FastList.newList()
                else
                    trimmedTokensToList(section.getAttr(ATTR_BASELINED_CHANGES), ",")

        val active = !section.isTogglePresent(TOGGLE_INACTIVE)

        val rollbackIfAlreadyDeployedSection = section.subsections
                .find { it.name == TextMarkupDocumentReader.TAG_ROLLBACK_IF_ALREADY_DEPLOYED }

        // in case the section exists but no content, mark it as an empty string so that changes can still get dropped from the audit log
        val rollbackIfAlreadyDeployedCommand = if (rollbackIfAlreadyDeployedSection == null) null else StringUtils.defaultIfEmpty(rollbackIfAlreadyDeployedSection.content, "")

        val rollbackSection = section.subsections
                .detect { it -> it.name == TextMarkupDocumentReader.TAG_ROLLBACK }
        val rollbackContent = rollbackSection?.content

        val content = if (section.content == null) "" else section.content
        if (StringUtils.isEmpty(content)) {
            LOG.warn("WARNING: Empty change defined in [Table={}, Change={}]; please avoid empty changes or correct/remove if possible", objectName, changeName)
        }
        if (rollbackSection != null && StringUtils.isBlank(rollbackContent)) {
            LOG.warn("WARNING: Empty rollback script defined in [Table={}, Change={}], which will be ignored. Please remove, or add content (e.g. a dummy SQL update if you want to force a rollback)", objectName, changeName)
        }

        val changeInput = ChangeInput(false)
        changeInput.changeKey = ChangeKey(schema, changeType, objectName, changeName)
        changeInput.orderWithinObject = changeIndex
        changeInput.contentHash = this.contentHashStrategy.hashContent(content)
        changeInput.content = content;
        changeInput.active = active
        changeInput.rollbackIfAlreadyDeployedContent = rollbackIfAlreadyDeployedCommand
        // part2
        changeInput.rollbackContent = rollbackContent
        changeInput.baselinedChanges = baselinedChanges
        changeInput.isDrop = drop
        changeInput.isKeepIncrementalOrder = drop

        val applyGrantsStr = section.getAttr(ATTR_APPLY_GRANTS)
        changeInput.applyGrants = if (applyGrantsStr == null) null else java.lang.Boolean.valueOf(applyGrantsStr)
        changeInput.changeset = section.getAttr(ATTR_CHANGESET)
        changeInput.parallelGroup = section.getAttr(ATTR_PARALLEL_GROUP)

        return changeInput
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TableChangeParser::class.java)
        private val ATTR_APPLY_GRANTS = "applyGrants"
        private val ATTR_CHANGESET = "changeset"
        private val ATTR_PARALLEL_GROUP = "parallelGroup"
        private val ATTR_NAME = "name"
        private val ATTR_BASELINED_CHANGES = "baselinedChanges"
        private val ATTR_COMMENT = "comment"
        private val ATTR_TEMPLATE_PARAMS = "templateParams"
        private val TOGGLE_FK = "FK"
        private val TOGGLE_TRIGGER = "TRIGGER"
        private val TOGGLE_DROP_TABLE = "DROP_TABLE"
        private val TOGGLE_INACTIVE = "INACTIVE"
        private val TOGGLE_INDEX = "INDEX"

        @JvmField
        val DEFAULT_IMPL: GetChangeType = object : GetChangeType {
            override fun getChangeType(section: TextMarkupDocumentSection, tableChangeType: ChangeType): ChangeType {
                return tableChangeType
            }
        }

        private fun tableDisallowedSections(): ImmutableSet<String> {
            return Sets.immutable.with(TextMarkupDocumentReader.TAG_DROP_COMMAND)
        }

        private fun tableToggles(): ImmutableSet<String> {
            return Sets.immutable.with(
                    TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS,
                    TOGGLE_INACTIVE,
                    TOGGLE_FK,
                    TOGGLE_TRIGGER,
                    TOGGLE_INDEX,
                    TOGGLE_DROP_TABLE
            )
        }

        private fun tableAttributes(): ImmutableSet<String> {
            return Sets.immutable.with(
                    ATTR_APPLY_GRANTS,
                    ATTR_BASELINED_CHANGES,
                    ATTR_CHANGESET,
                    ATTR_NAME,
                    ATTR_COMMENT,
                    ATTR_PARALLEL_GROUP,
                    ATTR_TEMPLATE_PARAMS,
                    TextMarkupDocumentReader.ATTR_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_EXCLUDE_DEPENDENCIES,
                    TextMarkupDocumentReader.ATTR_INCLUDE_DEPENDENCIES,
                    TextMarkupDocumentReader.TAG_PERM_SCHEME,
                    TextMarkupDocumentReader.EXCLUDE_ENVS,
                    TextMarkupDocumentReader.EXCLUDE_PLATFORMS,
                    TextMarkupDocumentReader.INCLUDE_ENVS,
                    TextMarkupDocumentReader.INCLUDE_PLATFORMS
            )
        }
    }
}
