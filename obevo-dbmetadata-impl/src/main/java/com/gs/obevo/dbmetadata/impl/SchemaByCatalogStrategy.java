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

import schemacrawler.schema.Schema;

/**
 * Alternative {@link SchemaStrategy} schema strategy that reads in the catalog name as the schema. Used in exceptional
 * cases like Sybase ASE.
 */
public class SchemaByCatalogStrategy implements SchemaStrategy {
    public static final SchemaByCatalogStrategy INSTANCE = new SchemaByCatalogStrategy();

    @Override
    public String getSchemaName(Schema schema) {
        return schema.getCatalogName();
    }

    @Override
    public String getSubschemaName(Schema schema) {
        return schema.getName();
    }
}
