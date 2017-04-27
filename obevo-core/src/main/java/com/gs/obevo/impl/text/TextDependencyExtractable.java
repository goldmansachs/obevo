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
package com.gs.obevo.impl.text;

import com.gs.obevo.api.appdata.ObjectKey;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Functions;

/**
 * Apply this interface to objects to make it possible to determine their dependencies from their text code using a
 * {@link TextDependencyExtractor}.
 */
public interface TextDependencyExtractable {
    Function<TextDependencyExtractable, ObjectKey> TO_OBJECT_KEY = new Function<TextDependencyExtractable, ObjectKey>() {
        @Override
        public ObjectKey valueOf(TextDependencyExtractable arg0) {
            return arg0.getObjectKey();
        }
    };

    Function<TextDependencyExtractable, String> TO_OBJECT_NAME = Functions.chain(TextDependencyExtractable.TO_OBJECT_KEY, ObjectKey.TO_OBJECT_NAME);

    Function<TextDependencyExtractable, String> TO_SCHEMA = Functions.chain(TextDependencyExtractable.TO_OBJECT_KEY, ObjectKey.TO_SCHEMA);

    /**
     * The object's identity. The {@link TextDependencyExtractor} would use the names from the identities as the
     * dependencies to try to extract from the text (see {@link #getContentForDependencyCalculation()}.
     */
    ObjectKey getObjectKey();

    /**
     * The object's dependencies. Note that this may be set beforehand; if so, then no additional dependency calculation
     * would be done.
     */
    ImmutableSet<String> getDependencies();

    /**
     * Sets the dependencies on the object. This would be called by the {@link TextDependencyExtractor} only.
     */
    void setDependencies(ImmutableSet<String> dependencies);

    /**
     * The dependencies to exclude from the text. This is to let the user specify the false-positive dependencies that
     * the {@link TextDependencyExtractor} may extract.
     */
    ImmutableSet<String> getExcludeDependencies();

    /**
     * The content that would be analyzed by the {@link TextDependencyExtractor} for dependencies.
     */
    String getContentForDependencyCalculation();

    ImmutableSet<String> getIncludeDependencies();
}
