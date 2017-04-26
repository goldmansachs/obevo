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
package com.gs.obevocomparer.input.filter;

import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.input.AbstractCatoWrapperDataSource;
import com.gs.obevocomparer.input.CatoDataSource;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.impl.lazy.iterator.SelectIterator;

public class FilterDataSource extends AbstractCatoWrapperDataSource {

    private final SelectIterator<CatoDataObject> iterator;

    public FilterDataSource(CatoDataSource baseDataSource, Predicate<CatoDataObject> discriminator) {
        super(baseDataSource);
        this.iterator = new SelectIterator<CatoDataObject>(baseDataSource, discriminator);
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public CatoDataObject next() {
        return this.iterator.next();
    }
}
