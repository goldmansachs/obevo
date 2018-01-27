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
package com.gs.obevo.db.impl.platforms.sybaseiq;

import java.sql.Driver;

import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.block.factory.Functions;

/**
 * Sybase IQ DBMS platform implementation.
 */
public class IqDbPlatform extends AbstractDbPlatform {
    private static final String IANYWHERE_DRIVER_CLASS_NAME = "ianywhere.ml.jdbcodbc.IDriver";
    private static final String SQLANYWHERE_DRIVER_CLASS_NAME = "sybase.jdbc.sqlanywhere.IDriver";

    public IqDbPlatform() {
        super("SYBASE_IQ");
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return SybaseIqAppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "com.sybase.jdbc3.jdbc.SybDriver";
    }

    @Override
    public Class<? extends Driver> getDriverClass(DbEnvironment env) {
        if (env.getDbServer() != null) {
            try {
                if (isSqlAnywhereDriverAvailable()) {
                    return (Class<? extends Driver>) Class.forName(SQLANYWHERE_DRIVER_CLASS_NAME);
                } else if (isIanywhereDriverAvailable()) {
                    return (Class<? extends Driver>) Class.forName(IANYWHERE_DRIVER_CLASS_NAME);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return super.getDriverClass(env);
    }

    @Override
    protected String getGrantTargetTypeStrDbSpecific(GrantTargetType grantTargetType) {
        // These DB types do not need "GROUP" or "USER" explicitly specified
        return "";
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return Functions.getPassThru();
    }

    @Override
    public DbTranslationDialect getDbTranslationDialect(DbPlatform targetDialect) {
        if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.h2.H2DbPlatform")) {
            return new IqToH2TranslationDialect();
        } else if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.hsql.HsqlDbPlatform")) {
            return new IqToHsqlTranslationDialect();
        } else {
            return super.getDbTranslationDialect(targetDialect);
        }
    }

    private static boolean isIanywhereDriverAvailable() {
        try {
            Class.forName(IANYWHERE_DRIVER_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isSqlAnywhereDriverAvailable() {
        try {
            Class.forName(SQLANYWHERE_DRIVER_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
