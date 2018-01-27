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
package com.gs.obevo.dbmetadata.api;

public class DaRuleImpl implements DaRule {
    private final String name;
    private final DaSchema schema;

    public DaRuleImpl(String name, DaSchema schema) {
        this.name = name;
        this.schema = schema;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DaSchema getSchema() {
        return schema;
    }
}
