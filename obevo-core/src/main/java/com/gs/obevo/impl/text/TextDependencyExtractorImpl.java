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
package com.gs.obevo.impl.text;

import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.CodeDependency;
import com.gs.obevo.api.appdata.CodeDependencyType;
import com.gs.obevo.api.appdata.ObjectKey;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Sets;

/**
 * Standard implementationt of {@link TextDependencyExtractor} going forward. Looks across all object types.
 */
public class TextDependencyExtractorImpl implements TextDependencyExtractor {
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\W+", Pattern.DOTALL);

    private final Function<String, String> convertDbObjectName;

    public TextDependencyExtractorImpl(Function<String, String> convertDbObjectName) {
        this.convertDbObjectName = convertDbObjectName;
    }

    @Override
    public <T extends TextDependencyExtractable> void calculateDependencies(RichIterable<T> changes) {
        MutableSet<String> objectNames = changes.collect(Functions.chain(TextDependencyExtractable.TO_OBJECT_KEY, ObjectKey.TO_OBJECT_NAME)).collect(convertDbObjectName).toSet();

        for (T change : changes) {
            // note - only check for nulls here; we may set dependencies to blank explicitly in the overrides
            if (change.getCodeDependencies() == null && change.getObjectKey().getChangeType().isEnrichableForDependenciesInText()) {
                // we use getContentForDependencyCalculation() instead of just getContent() due to the staticData
                // objects needing to have its dependency calculated differently.

                // TODO go via objectNames and physicalSchema+objectName combo
                MutableSet<CodeDependency> codeDependencies = calculateDependencies(change.getObjectKey().toString(), change.getContentForDependencyCalculation(), objectNames)
                        .reject(Predicates.equal(convertDbObjectName.valueOf(change.getObjectKey().getObjectName())))
                        .reject(Predicates.in(change.getExcludeDependencies()))
                        .collectWith(CodeDependency.CREATE_WITH_TYPE, CodeDependencyType.DISCOVERED);

                codeDependencies.withAll(change.getIncludeDependencies().collectWith(CodeDependency.CREATE_WITH_TYPE, CodeDependencyType.EXPLICIT));

                change.setCodeDependencies(codeDependencies.toImmutable());
            }
        }
    }

    MutableSet<String> calculateDependencies(String logMessage, String content, MutableSet<String> objectNames) {
        String[] tokens = SPACE_PATTERN.split(CommentRemover.removeComments(content, logMessage));
        MutableSet<String> dependencies = Sets.mutable.with();
        for (String token : tokens) {
            if (objectNames.contains(convertDbObjectName.valueOf(token))) {
                dependencies.add(convertDbObjectName.valueOf(token));
            }
        }
        return dependencies;
    }
}
