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
package com.gs.obevo.dbmetadata.impl;

import com.gs.obevo.dbmetadata.api.DaSchema;
import com.gs.obevo.dbmetadata.api.DaSequence;
import schemacrawler.schema.Sequence;

public class DaSequence6Impl implements DaSequence {
    private final Sequence delegate;
    private final SchemaStrategy schemaStrategy;

    public DaSequence6Impl(Sequence delegate, SchemaStrategy schemaStrategy) {
        this.delegate = delegate;
        this.schemaStrategy = schemaStrategy;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public DaSchema getSchema() {
        return new DaSchemaImpl(delegate.getSchema(), schemaStrategy);
    }
}
