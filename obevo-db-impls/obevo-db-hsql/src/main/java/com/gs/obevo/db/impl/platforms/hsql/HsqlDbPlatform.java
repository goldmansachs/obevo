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
package com.gs.obevo.db.impl.platforms.hsql;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.platform.DbChangeTypeImpl;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;

/**
 * HSQL DB implementation.
 */
public class HsqlDbPlatform extends AbstractDbPlatform {
    public HsqlDbPlatform() {
        super("HSQL");
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return HsqlAppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "org.hsqldb.jdbc.JDBCDriver";
    }

    @Override
    protected ImmutableList<ChangeType> initializeChangeTypes() {
        return super.initializeChangeTypes().newWithAll(Lists.immutable.with(
                DbChangeTypeImpl.newDbChangeType(ChangeType.USERTYPE_STR, true, 3, "TYPE").build()
        ));
    }

    @Override
    protected String getGrantTargetTypeStrDbSpecific(GrantTargetType grantTargetType) {
        // These DB types do not need "GROUP" or "USER" explicitly specified
        return "";
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toUpperCase();
    }

    @Override
    public AbstractDdlReveng getDdlReveng() {
        return new HsqlReveng();
    }
}
