/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.db.impl.core.reader;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.impl.SourceChangeReader;
import com.gs.obevo.impl.text.TextDependencyExtractor;
import com.gs.obevo.util.CollectionUtil;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceChangeReaderImpl implements SourceChangeReader<DbEnvironment> {
    private static final Logger LOG = LoggerFactory.getLogger(SourceChangeReaderImpl.class);

    private final DbEnvironment env;
    private final DbChangeReader dbChangeReader;
    private final TextDependencyExtractor textDependencyExtractor;
    private final ImmutableList<PrepareDbChange> artifactTranslators;

    public SourceChangeReaderImpl(DbEnvironment env, DbChangeReader dbChangeReader, TextDependencyExtractor textDependencyExtractor, ImmutableList<PrepareDbChange> artifactTranslators) {
        this.env = env;
        this.dbChangeReader = dbChangeReader;
        this.textDependencyExtractor = textDependencyExtractor;
        this.artifactTranslators = artifactTranslators;
    }

    @Override
    public ImmutableList<Change> readSourceChanges(boolean useBaseline, Predicate<Change> dbChangeFilter) {
        ImmutableList<Change> sourceChanges = this.dbChangeReader
                .readChanges(useBaseline)
                .select(dbChangeFilter);

        CollectionUtil.verifyNoDuplicates(sourceChanges, Change.TO_CHANGE_KEY, "Duplicate changes found - please check your input source files (e.g. no //// " + TextMarkupDocumentReader.TAG_CHANGE + " entries with the same name in a file or files w/ same object names within an environment)");

        // TODO ensure that we've handled the split between static data and others properly
        this.textDependencyExtractor.calculateDependencies(sourceChanges);

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

    private void tokenizeChange(Change change, DbEnvironment env) {
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
