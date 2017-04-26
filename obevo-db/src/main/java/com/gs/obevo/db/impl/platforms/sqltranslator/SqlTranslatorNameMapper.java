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
package com.gs.obevo.db.impl.platforms.sqltranslator;

/**
 * Index and constraint names must be unique across the schema in H2 and HSQL
 * However, ASE allow the names to be unique just within the table
 * and DB2 allows uniqueness for constraints (but not indices)
 *
 * We do this remapping to let existing schemas work
 */
public interface SqlTranslatorNameMapper {
    String remapIndexName(String name, String tableName);

    String remapConstraintName(String name, String tableName);
}
