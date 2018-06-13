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
package com.gs.obevo.impl

import com.gs.obevo.api.appdata.*
import com.gs.obevo.api.platform.MainDeployerArgs
import com.gs.obevo.util.CollectionUtil
import org.eclipse.collections.api.block.predicate.Predicate
import org.eclipse.collections.api.collection.ImmutableCollection
import org.eclipse.collections.api.list.ImmutableList
import org.eclipse.collections.impl.block.factory.Predicates.and
import org.slf4j.LoggerFactory

class MainInputReader<E : Environment<*>>(
        private val env: E,
        private val dbChangeFilter: Predicate<in ChangeKey>,
        private val artifactTranslators: ImmutableList<PrepareDbChange<in E>>,
        protected val deployMetricsCollector: DeployMetricsCollector
) {

    fun readInternal(dbChangeReader: SourceReaderStrategy, deployerArgs: MainDeployerArgs): ImmutableList<ChangeInput> {
        LOG.info("Now fetching the changed artifacts")

        val sourceChanges = readSourceChanges(
                dbChangeReader,
                deployerArgs.isUseBaseline,
                and(
                        Predicate<ChangeInput> { this.dbChangeFilter.accept(it.changeKey) },
                        Predicate<ChangeInput> { deployerArgs.changeInclusionPredicate.accept(it.changeKey) }
                )
        )

        logChangeInputs("source", sourceChanges)

        logChangeMetrics("source", sourceChanges)

        return sourceChanges
    }

    private fun logChangeInputs(logType: String, sourceChanges: ImmutableCollection<ChangeInput>) {
        if (LOG.isInfoEnabled) {
            LOG.info("Read {} changes from {}", sourceChanges.size(), logType)
        }
        if (LOG.isDebugEnabled) {
            for (sourceChange in sourceChanges) {
                val locationLog = if (sourceChange.fileLocation != null) ", File: " + sourceChange.fileLocation else ""

                LOG.debug("-> {} Change: {} at {}", logType, sourceChange.displayString, locationLog)
            }
        }
    }

    fun logChanges(logType: String, sourceChanges: ImmutableCollection<Change>) {
        if (LOG.isInfoEnabled) {
            LOG.info("Read {} changes from {}", sourceChanges.size(), logType)
        }
        if (LOG.isDebugEnabled) {
            for (sourceChange in sourceChanges) {
                LOG.debug("-> {} Change: {} at {}", logType, sourceChange.displayString)
            }
        }
    }

    private fun logChangeMetrics(changeSide: String, changes: ImmutableList<ChangeInput>) {
        val changeTypeCounts = changes.collect { change -> change.changeKey.changeType.name }.toBag()
        changeTypeCounts.toMapOfItemToCount().forEachKeyValue { changeType, count -> deployMetricsCollector.addMetric("changes.$changeSide.$changeType", count) }
    }

    private fun readSourceChanges(dbChangeReader: SourceReaderStrategy, useBaseline: Boolean, dbChangeFilter: Predicate<ChangeInput>): ImmutableList<ChangeInput> {
        val sourceChanges = dbChangeReader
                .getChanges(useBaseline)
                .select(dbChangeFilter)
                .selectWith(ArtifactRestrictions.apply(), env)

        CollectionUtil.verifyNoDuplicates(sourceChanges, { change1 -> change1.changeKey }, "Duplicate changes found - please check your input source files (e.g. no //// CHANGE entries with the same name in a file or files w/ same object names within an environment)")

        // We tokenize at this point (prior to the changeset calculation) as we'd want to have both the untokenized
        // and tokenized file contents to be hashed so that the change comparison can consider both. The use case
        // here was if the original hash was taken from the untokenized value, but later we change the SQL text to
        // tokenize it -> we don't want that to count as a hash different as the end result of tokenization is still
        // the same
        for (change in sourceChanges) {
            this.tokenizeChange(change, env)
        }

        return sourceChanges
    }

    private fun tokenizeChange(change: ChangeInput, env: E) {
        var content = change.content
        var rollbackContent: String? = change.rollbackContent
        for (translator in this.artifactTranslators) {
            content = translator.prepare(content, change, env)
        }
        if (rollbackContent != null) {
            for (translator in this.artifactTranslators) {
                rollbackContent = translator.prepare(rollbackContent, change, env)
            }
        }

        change.setConvertedContent(content)
        change.setConvertedRollbackContent(rollbackContent)

        if (LOG.isTraceEnabled) {
            LOG.trace("Content for {} was converted to {}", change.displayString, content)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MainInputReader::class.java)
    }
}
