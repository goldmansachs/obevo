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
import com.gs.obevo.api.appdata.doc.TextMarkupDocumentSection
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.DeployMetrics
import com.gs.obevo.api.platform.FileSourceContext
import com.gs.obevo.api.platform.FileSourceParams
import com.gs.obevo.impl.DeployMetricsCollector
import com.gs.obevo.impl.DeployMetricsCollectorImpl
import com.gs.obevo.impl.OnboardingStrategy
import com.gs.obevo.util.VisibleForTesting
import com.gs.obevo.util.hash.OldWhitespaceAgnosticDbChangeHashStrategy
import com.gs.obevo.util.vfs.*
import com.gs.obevo.util.vfs.FileFilterUtils.*
import org.apache.commons.lang3.Validate
import org.apache.commons.vfs2.FileFilter
import org.eclipse.collections.api.block.function.Function
import org.eclipse.collections.api.block.function.Function0
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Sets
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap
import org.slf4j.LoggerFactory

class DbDirectoryChangesetReader : FileSourceContext {

    private val convertDbObjectName: Function<String, String>
    private val packageMetadataReader: PackageMetadataReader
    private val packageMetadataCache = ConcurrentHashMap<FileObject, PackageMetadata>()
    private val tableChangeParser: DbChangeFileParser
    private val baselineTableChangeParser: DbChangeFileParser?
    private val rerunnableChangeParser: DbChangeFileParser
    private val deployMetricsCollector: DeployMetricsCollector

    constructor(convertDbObjectName: Function<String, String>, deployMetricsCollector: DeployMetricsCollector, backwardsCompatibleMode: Boolean, textMarkupDocumentReader: TextMarkupDocumentReader, baselineTableChangeParser: DbChangeFileParser?, getChangeType: GetChangeType) {
        this.packageMetadataReader = PackageMetadataReader(textMarkupDocumentReader)
        this.convertDbObjectName = convertDbObjectName
        this.tableChangeParser = TableChangeParser(OldWhitespaceAgnosticDbChangeHashStrategy(), backwardsCompatibleMode, deployMetricsCollector, textMarkupDocumentReader, getChangeType)
        this.baselineTableChangeParser = baselineTableChangeParser
        this.rerunnableChangeParser = RerunnableChangeParser(backwardsCompatibleMode, deployMetricsCollector, textMarkupDocumentReader)
        this.deployMetricsCollector = deployMetricsCollector
    }

    @VisibleForTesting
    constructor(convertDbObjectName: Function<String, String>, tableChangeParser: DbChangeFileParser,
                         baselineTableChangeParser: DbChangeFileParser?, rerunnableChangeParser: DbChangeFileParser) {
        this.packageMetadataReader = PackageMetadataReader(TextMarkupDocumentReader(false))
        this.convertDbObjectName = convertDbObjectName
        this.tableChangeParser = tableChangeParser
        this.baselineTableChangeParser = baselineTableChangeParser
        this.rerunnableChangeParser = rerunnableChangeParser
        this.deployMetricsCollector = DeployMetricsCollectorImpl()
    }

    /*
     * (non-Javadoc)
     *
     * @see com.gs.obevo.db.newdb.DbChangeReader#readChanges(java.io.File)
     */
    override fun readChanges(fileSourceParams: FileSourceParams): ImmutableList<ChangeInput> {
        val allChanges = mutableListOf<ChangeInput>()

        val envSchemas = fileSourceParams.schemaNames.collect(this.convertDbObjectName)

        Validate.notEmpty(envSchemas.castToSet(), "Environment must have schemas populated")

        for (sourceDir in fileSourceParams.files) {
            for (schemaDir in sourceDir.findFiles(BasicFileSelector(and(vcsAware(), directory()), false))) {
                val schema = schemaDir.name.baseName

                if (envSchemas.contains(this.convertDbObjectName.valueOf(schema))) {
                    val schemaChanges = fileSourceParams.changeTypes.flatMap { changeType ->
                        val changeTypeDir = this.findDirectoryForChangeType(schemaDir, changeType, fileSourceParams.isLegacyDirectoryStructureEnabled)

                        if (changeTypeDir != null) {
                            if (changeType.isRerunnable) {
                                findChanges(changeType, changeTypeDir, this.rerunnableChangeParser, TrueFileFilter.INSTANCE, schema, fileSourceParams.acceptedExtensions, fileSourceParams.defaultSourceEncoding)
                            } else {
                                findTableChanges(changeType, changeTypeDir, schema, fileSourceParams.isBaseline, fileSourceParams.acceptedExtensions, fileSourceParams.defaultSourceEncoding)
                            }
                        } else {
                            emptyList<ChangeInput>()
                        }
                    }

                    val tableChangeMap = schemaChanges
                            .filter { Sets.immutable.of(ChangeType.TABLE_STR, ChangeType.FOREIGN_KEY_STR).contains(it.changeTypeName) }
                            .groupBy(ChangeInput::getDbObjectKey)

                    val staticDataChanges = schemaChanges.filter { it.changeTypeName.equals(ChangeType.STATICDATA_STR)}

                    // now enrich the staticData objects w/ the information from the tables to facilitate the
                    // deployment order calculation.
                    staticDataChanges.forEach { staticDataChange ->
                        val relatedTableChanges = tableChangeMap.get(staticDataChange.getDbObjectKey()) ?: emptyList()
                        val foreignKeys = relatedTableChanges.filter { it.changeTypeName == ChangeType.FOREIGN_KEY_STR }

                        val fkContent = foreignKeys.map { it.content }.joinToString("\n\n")

                        staticDataChange.setContentForDependencyCalculation(fkContent)
                    }

                    allChanges.addAll(schemaChanges)
                } else {
                    LOG.info("Skipping schema directory [{}] as it was not defined among the schemas in your system-config.xml file: {}", schema, envSchemas)
                    continue
                }
            }
        }

        return Lists.immutable.ofAll(allChanges);
    }

    /**
     * We have this here to look for directories in the various places we had specified it in the code before (i.e.
     * for backwards-compatibility).
     */
    private fun findDirectoryForChangeType(schemaDir: FileObject, changeType: ChangeType, legacyDirectoryStructureEnabled: Boolean): FileObject? {
        var dir = schemaDir.getChild(changeType.directoryName)
        if (dir != null && dir.exists()) {
            return dir
        }
        dir = schemaDir.getChild(changeType.directoryName.toUpperCase())
        if (dir != null && dir.exists()) {
            return dir
        }
        if (legacyDirectoryStructureEnabled && changeType.directoryNameOld != null) {
            // for backwards-compatibility
            dir = schemaDir.getChild(changeType.directoryNameOld)
            if (dir != null && dir.exists()) {
                deployMetricsCollector.addMetric("oldDirectoryNameUsed", true)
                return dir
            }
        }

        return null
    }

    private fun findTableChanges(changeType: ChangeType, tableDir: FileObject, schema: String, useBaseline: Boolean, acceptedExtensions: ImmutableSet<String>, sourceEncoding: String): List<ChangeInput> {
        val baselineFilter = WildcardFileFilter("*.baseline.*")

        val nonBaselineFiles = findFiles(tableDir,
                if (this.isUsingChangesConvention(tableDir)) CHANGES_WILDCARD_FILTER else NotFileFilter(baselineFilter), acceptedExtensions)

        var nonBaselineChanges = parseChanges(changeType, nonBaselineFiles, this.tableChangeParser, schema, sourceEncoding)
        val nonBaselineChangeMap = nonBaselineChanges.groupBy { it.dbObjectKey }

        if (useBaseline) {
            LOG.info("Using the 'useBaseline' mode to read in the db changes")
            val baselineFiles = findFiles(tableDir,
                    if (this.isUsingChangesConvention(tableDir)) CHANGES_WILDCARD_FILTER else baselineFilter, acceptedExtensions)

            if (baselineTableChangeParser == null) {
                throw IllegalArgumentException("Cannot invoke readChanges with useBaseline == true if baselineTableChangeParser hasn't been set; baseline reading may not be enabled in your Platform type")
            }

            val baselineChanges = parseChanges(changeType, baselineFiles, this.baselineTableChangeParser, schema, sourceEncoding)

            for (baselineChange in baselineChanges) {
                val regularChanges = nonBaselineChangeMap.get(baselineChange.dbObjectKey)!!
                if (regularChanges.isEmpty()) {
                    throw IllegalArgumentException("Invalid state - expecting a change here for this object key: " + baselineChange.dbObjectKey)
                }
                val regularChange = regularChanges.get(0)

                this.copyDbObjectMetadataOverToBaseline(regularChange, baselineChange)
            }

            val baselineDbObjectKeys = baselineChanges.map { it.dbObjectKey }.toSet()
            LOG.info("Found the following baseline changes: will try to deploy via baseline for these db objects: " + baselineDbObjectKeys.joinToString(","))

            nonBaselineChanges = nonBaselineChanges
                    .filterNot { baselineDbObjectKeys.contains(it.dbObjectKey) }
                    .plus(baselineChanges)
        }

        return nonBaselineChanges
    }

    /**
     * This is for stuff in the METADATA tag that would be in the changes file but not the baseline file
     */
    private fun copyDbObjectMetadataOverToBaseline(regularChange: ChangeInput, baselineChange: ChangeInput) {
        baselineChange.setRestrictions(regularChange.getRestrictions())
        baselineChange.permissionScheme = regularChange.permissionScheme
    }

    private fun findFiles(dir: FileObject, fileFilter: FileFilter, acceptedExtensions: ImmutableSet<String>): List<FileObject> {
        val positiveSelector = BasicFileSelector(
                and(
                        fileFilter,
                        not(directory()),
                        not(
                                or(
                                        WildcardFileFilter("package-info*"),
                                        WildcardFileFilter("*." + OnboardingStrategy.exceptionExtension)
                                )
                        )
                ),
                vcsAware()
        )

        val candidateFiles = listOf(*dir.findFiles(positiveSelector))
        val extensionPartition = candidateFiles.partition { acceptedExtensions.contains(it.name.extension.toLowerCase()) }

        if (extensionPartition.second.isNotEmpty()) {
            val message = "Found unexpected file extensions (these will not be deployed!) in directory " + dir + "; expects extensions [" + acceptedExtensions + "], found: " + extensionPartition.second
            LOG.warn(message)
            deployMetricsCollector.addListMetric(DeployMetrics.UNEXPECTED_FILE_EXTENSIONS, message)
        }

        return extensionPartition.first
    }

    private fun parseChanges(changeType: ChangeType, files: List<FileObject>,
                             changeParser: DbChangeFileParser, schema: String, sourceEncoding: String): List<ChangeInput> {
        return files.flatMap { file ->
            val packageMetadata = getPackageMetadata(file, sourceEncoding)
            var encoding: String? = null
            var metadataSection: TextMarkupDocumentSection? = null
            if (packageMetadata != null) {
                encoding = packageMetadata.fileToEncodingMap.get(file.name.baseName)
                metadataSection = packageMetadata.metadataSection
            }
            val charsetStrategy = CharsetStrategyFactory.getCharsetStrategy(encoding ?: sourceEncoding)
            val objectName = file.name.baseName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            try {
                LOG.debug("Attempting to read file {}", file)
                changeParser.value(changeType, file, file.getStringContent(charsetStrategy), objectName, schema, metadataSection).castToList()
            } catch (e: RuntimeException) {
                throw IllegalArgumentException("Error while parsing file " + file + " of change type " + changeType.name + "; please see the cause in the stack trace below: " + e.message, e)
            }
        }
    }

    private fun getPackageMetadata(file: FileObject, sourceEncoding: String): PackageMetadata? {
        return packageMetadataCache.getIfAbsentPut(file.parent, Function0<PackageMetadata> {
            val packageMetadataFile = file.parent!!.getChild("package-info.txt")

            // we check for containsKey, as we may end up persisting null as the value in the map
            if (packageMetadataFile == null || !packageMetadataFile.isReadable) {
                null
            } else {
                packageMetadataReader.getPackageMetadata(packageMetadataFile.getStringContent(CharsetStrategyFactory.getCharsetStrategy(sourceEncoding)))
            }
        })
    }

    private fun findChanges(changeType: ChangeType, dir: FileObject, changeParser: DbChangeFileParser,
                            fileFilter: FileFilter, schema: String, acceptedExtensions: ImmutableSet<String>, sourceEncoding: String): List<ChangeInput> {
        return parseChanges(changeType, findFiles(dir, fileFilter, acceptedExtensions), changeParser, schema, sourceEncoding)
    }

    /**
     * We have this check here for backwards-compatibility
     * Originally, we required the db files to end in .changes.*, but we now want to have the convention of the stard
     * file name being the
     * file that you edit, much like the other ones
     *
     * Originally, we wanted to regular name to be for the baseline, but given this is not yet implemented and won't
     * drive the changes, this was a wrong idea from the start. Eventually, we will have the baseline files in
     * .baseline.*
     */
    private fun isUsingChangesConvention(dir: FileObject): Boolean {
        val files = dir.findFiles(BasicFileSelector(CHANGES_WILDCARD_FILTER, false))
        val changesConventionUsed = files.size > 0
        if (changesConventionUsed) {
            deployMetricsCollector.addMetric("changesConventionUsed", true)
        }

        return changesConventionUsed
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DbDirectoryChangesetReader::class.java)

        @VisibleForTesting
        @JvmStatic
        val CHANGES_WILDCARD_FILTER: FileFilter = WildcardFileFilter("*.changes.*")
    }
}
