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
package com.gs.obevo.util.lookuppredicate;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate;

/**
 * Predicate used when we want to iterate over another predicate itself.
 */
public class ObjectPredicate<T> implements Predicate<Predicate<T>> {
    private final Function0<T> inputFunction;

    public static <T> ObjectPredicate<T> create(Function0<T> inputFunction) {
        return new ObjectPredicate<T>(inputFunction);
    }

    private ObjectPredicate(Function0<T> inputFunction) {
        this.inputFunction = inputFunction;
    }

    @Override
    public boolean accept(Predicate<T> index) {
        return index.accept(inputFunction.value());
    }
}
