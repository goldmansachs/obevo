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
package com.gs.obevo.dbmetadata.impl.dialects;

import java.sql.Connection;

import schemacrawler.schemacrawler.ExcludeAll;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;

public class H2MetadataDialect extends AbstractMetadataDialect {
    @Override
    public void customEdits(SchemaCrawlerOptions options, Connection conn, String schemaName) {
        // Do not retrieve H2 functions, as versions starting with 1.4.x will complain
        // Notably, versions before that would throw a MethodNotImplementedError, which SchemaCrawler is smart enough to catch and ignore
        // However, 1.4.x throws a RuntimeException, not a SQLFeatureNotSupportedException, and so it bombs the process
        // We add this to let clients deploy correctly w/ any version
        // Note that we do not yet support routines in H2
        options.setRoutineInclusionRule(new ExcludeAll());
    }

    @Override
    public String getSchemaExpression(String schemaName) {
        return "(?i).+\\." + schemaName;
    }
}
