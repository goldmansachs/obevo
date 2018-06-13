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
package com.gs.obevo.db.impl.core.reader;

import com.gs.obevo.api.appdata.ChangeInput;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.impl.PrepareDbChange;
import com.gs.obevo.util.Tokenizer;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

public class PrepareDbChangeForDb implements PrepareDbChange<DbEnvironment> {
    @Override
    public String prepare(String content, ChangeInput change, DbEnvironment env) {
        MutableMap<String, String> tokens = Maps.mutable.<String, String>empty()
                .withKeyValue("dbSchemaSuffix", env.getDbSchemaSuffix())
                .withKeyValue("dbSchemaPrefix", env.getDbSchemaPrefix());

        for (Schema schema : env.getSchemas()) {
            PhysicalSchema physicalSchema = env.getPhysicalSchema(schema.getName());
            tokens.put(schema.getName() + "_physicalName", physicalSchema.getPhysicalName());
            if (env.getPlatform() != null) {
                tokens.put(schema.getName() + "_schemaSuffixed", env.getPlatform().getSchemaPrefix(physicalSchema));
                tokens.put(schema.getName() + "_subschemaSuffixed", env.getPlatform().getSubschemaPrefix(physicalSchema));
            }
        }

        if (env.getDefaultTablespace() != null) {
            tokens.put("defaultTablespace", env.getDefaultTablespace());
        }

        tokens.putAll(env.getTokens().castToMap());  // allow clients to override these values if needed

        return new Tokenizer(tokens, env.getTokenPrefix(), env.getTokenSuffix()).tokenizeString(content);
    }
}
