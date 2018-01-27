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

import com.gs.obevo.api.appdata.Change;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Detects the dependencies of a Change from the code content that it has by searching for the object names
 * contained within it, and sets the {@link Change#setCodeDependencies(ImmutableSet)} )} field back on the object
 * accordingly.
 */
public interface TextDependencyExtractor {
    <T extends TextDependencyExtractable> void calculateDependencies(RichIterable<T> changes);
}
