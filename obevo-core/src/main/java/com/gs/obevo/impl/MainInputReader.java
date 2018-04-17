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
package com.gs.obevo.impl;

import com.gs.obevo.api.appdata.ArtifactRestrictions;
import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.platform.MainDeployerArgs;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.util.CollectionUtil;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.collections.impl.block.factory.Predicates.and;

public class MainInputReader<P extends Platform, E extends Environment<P>> {
    private static final Logger LOG = LoggerFactory.getLogger(MainInputReader.class);
    private final Environment env;
    private final Predicate<? super Change> dbChangeFilter;
    private final ImmutableList<PrepareDbChange> artifactTranslators;
    private final DeployMetricsCollector deployMetricsCollector;

    public MainInputReader(Environment env, Predicate<? super Change> dbChangeFilter, ImmutableList<PrepareDbChange> artifactTranslators, DeployMetricsCollector deployMetricsCollector) {
        this.env = env;
        this.dbChangeFilter = dbChangeFilter;
        this.artifactTranslators = artifactTranslators;
        this.deployMetricsCollector = deployMetricsCollector;
    }

    protected DeployMetricsCollector getDeployMetricsCollector() {
        return deployMetricsCollector;
    }

    public ImmutableList<Change> readInternal(SourceReaderStrategy dbChangeReader, final MainDeployerArgs deployerArgs) {
        LOG.info("Now fetching the changed artifacts");

        ImmutableList<Change> sourceChanges = readSourceChanges(
                dbChangeReader,
                deployerArgs.isUseBaseline(),
                and(this.dbChangeFilter, deployerArgs.getChangeInclusionPredicate()));

        logChanges("source", sourceChanges);

        logChangeMetrics("source", sourceChanges);

        return sourceChanges;
    }

    public void logChanges(String logType, ImmutableCollection<Change> sourceChanges) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Read {} changes from {}", sourceChanges.size(), logType);
        }
        if (LOG.isDebugEnabled()) {
            for (Change sourceChange : sourceChanges) {
                String locationLog = sourceChange.getFileLocation() != null ? ", File: " + sourceChange.getFileLocation() : "";

                LOG.debug("-> {} Change: {}, Hash: {} {}", logType, sourceChange.getDisplayString(), sourceChange.getContentHash(), locationLog);
            }
        }
    }

    private void logChangeMetrics(final String changeSide, ImmutableList<Change> changes) {
        MutableBag<String> changeTypeCounts = changes.collect(Change::getChangeTypeName).toBag();
        changeTypeCounts.toMapOfItemToCount().forEachKeyValue((changeType, count) -> deployMetricsCollector.addMetric("changes." + changeSide + "." + changeType, count));
    }

    private ImmutableList<Change> readSourceChanges(SourceReaderStrategy dbChangeReader, boolean useBaseline, Predicate<Change> dbChangeFilter) {
        ImmutableList<Change> sourceChanges = dbChangeReader
                .getChanges(useBaseline)
                .select(dbChangeFilter)
                .selectWith(ArtifactRestrictions.apply(), env);

        CollectionUtil.verifyNoDuplicates(sourceChanges, Change::getChangeKey, "Duplicate changes found - please check your input source files (e.g. no //// CHANGE entries with the same name in a file or files w/ same object names within an environment)");

        // We tokenize at this point (prior to the changeset calculation) as we'd want to have both the untokenized
        // and tokenized file contents to be hashed so that the change comparison can consider both. The use case
        // here was if the original hash was taken from the untokenized value, but later we change the SQL text to
        // tokenize it -> we don't want that to count as a hash different as the end result of tokenization is still
        // the same
        for (Change change : sourceChanges) {
            this.tokenizeChange(change, env);
        }

        return sourceChanges;
    }

    private void tokenizeChange(Change change, Environment env) {
        String content = change.getContent();
        String rollbackContent = change.getRollbackContent();
        for (PrepareDbChange translator : this.artifactTranslators) {
            content = translator.prepare(content, change, env);
        }
        if (rollbackContent != null) {
            for (PrepareDbChange translator : this.artifactTranslators) {
                rollbackContent = translator.prepare(rollbackContent, change, env);
            }
        }

        change.setConvertedContent(content);
        change.setConvertedRollbackContent(rollbackContent);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Content for {} was converted to {}", change.getDisplayString(), content);
        }
    }
}
