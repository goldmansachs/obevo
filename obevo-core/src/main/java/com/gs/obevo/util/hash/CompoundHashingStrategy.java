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
package com.gs.obevo.util.hash;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;

class CompoundHashingStrategy<T> implements HashingStrategy<T> {
    private final ImmutableList<Function<T, ?>> keyAttrs;

    CompoundHashingStrategy(ImmutableList<? extends Function<T, ?>> keyAttrs) {
        this.keyAttrs = (ImmutableList<Function<T, ?>>) keyAttrs;
    }

    public int computeHashCode(final T object) {
        final HashCodeBuilder hash = new HashCodeBuilder(17, 37);
        this.keyAttrs.forEach(new Procedure<Function<T, ?>>() {
            @Override
            public void value(Function<T, ?> attr) {
                hash.append(attr.valueOf(object));
            }
        });
        return hash.toHashCode();
    }

    public boolean equals(final T object1, final T object2) {
        final EqualsBuilder equals = new EqualsBuilder();
        this.keyAttrs.forEach(new Procedure<Function<T, ?>>() {
            @Override
            public void value(Function<T, ?> attr) {
                equals.append(attr.valueOf(object1), attr.valueOf(object2));
            }
        });
        return equals.isEquals();
    }
}
