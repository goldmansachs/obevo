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

import com.gs.obevo.api.appdata.CodeDependency;
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
     * @since 6.0.0
     */
    ObjectKey getObjectKey();

    /**
     * The object's dependencies. Note that this may be set beforehand; if so, then no additional dependency calculation
     * would be done.
     * @since 6.4.0
     */
    ImmutableSet<CodeDependency> getCodeDependencies();

    /**
     * The object's dependencies. Note that this may be set beforehand; if so, then no additional dependency calculation
     * would be done.
     * @since 6.0.0
     * @deprecated Use {@link #getCodeDependencies()}. Remove in 7.0.0
     */
    @Deprecated
    ImmutableSet<String> getDependencies();

    /**
     * Sets the dependencies on the object. This would be called by the {@link TextDependencyExtractor} only.
     * @since 6.4.0
     */
    void setCodeDependencies(ImmutableSet<CodeDependency> codeDependencies);

    /**
     * Sets the dependencies on the object. This would be called by the {@link TextDependencyExtractor} only.
     * @deprecated Use {@link #setCodeDependencies(ImmutableSet)}. Remove in 7.0.0
     */
    @Deprecated
    void setDependencies(ImmutableSet<String> dependencies);

    /**
     * The dependencies to exclude from the text. This is to let the user specify the false-positive dependencies that
     * the {@link TextDependencyExtractor} may extract.
     * @since 6.0.0
     */
    ImmutableSet<String> getExcludeDependencies();

    /**
     * The dependencies to force-include. This is to let the user specify the false-negative dependencies that
     * the {@link TextDependencyExtractor} may omit.
     * @since 6.0.0
     */
    ImmutableSet<String> getIncludeDependencies();

    /**
     * The content that would be analyzed by the {@link TextDependencyExtractor} for dependencies.
     * @since 6.0.0
     */
    String getContentForDependencyCalculation();
}
