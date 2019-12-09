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
package com.gs.obevo.db.impl.platforms.mysql;

import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.apps.reveng.Reveng;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.block.factory.StringFunctions;

/**
 * PostgreSQL DBMS platform implementation.
 */
public class MySqlDbPlatform extends AbstractDbPlatform {
    public MySqlDbPlatform() {
        super("MYSQL");
    }

    /**
     * Protected constructor to allow for the Amazon-based platforms that leverage PostgreSQL as their SQL interface.
     */
    protected MySqlDbPlatform(String name) {
        super(name);
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return MySqlAppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toLowerCase();
    }

    @Override
    public Reveng getDdlReveng() {
        return new MySqlDumpReveng();
    }

    /**
     * MySQL doesn't have a public grant concept. Obevo currently doesn't have a good way to define a read-only
     * grant to another user/group, so we leave this for now.
     */
    @Override
    public boolean isPublicSchemaSupported() {
        return false;
    }
}
