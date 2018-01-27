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
 * The default implementation for the {@link SchemaStrategy}; usually the regular schema name suffices and catalog name
 * is not needed.
 */
public class SchemaByNameStrategy implements SchemaStrategy {
    public static final SchemaByNameStrategy INSTANCE = new SchemaByNameStrategy();

    @Override
    public String getSchemaName(Schema schema) {
        return schema.getName();
    }

    @Override
    public String getSubschemaName(Schema schema) {
        return null;
    }
}
