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
package com.gs.obevo.db.impl.platforms.postgresql;

import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbChangeTypeImpl;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.block.factory.StringFunctions;

/**
 * PostgreSQL DBMS platform implementation.
 */
public class PostgreSqlDbPlatform extends AbstractDbPlatform {
    public PostgreSqlDbPlatform() {
        super("POSTGRESQL");
    }

    /**
     * Protected constructor to allow for the Amazon-based platforms that leverage PostgreSQL as their SQL interface.
     */
    protected PostgreSqlDbPlatform(String name) {
        super(name);
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return PostgreSqlAppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    protected ImmutableList<ChangeType> initializeChangeTypes() {
        MutableList<ChangeType> changeTypes = super.initializeChangeTypes().toList();

        DbChangeType sequenceType = getChangeType(changeTypes, ChangeType.SEQUENCE_STR);
        sequenceType = DbChangeTypeImpl.newDbChangeType(sequenceType).setGrantObjectQualifier("SEQUENCE").build();
        replaceChangeType(changeTypes, sequenceType);

        DbChangeType functionType = getChangeType(changeTypes, ChangeType.FUNCTION_STR);
        functionType = DbChangeTypeImpl.newDbChangeType(functionType).setGrantObjectQualifier("FUNCTION").build();
        replaceChangeType(changeTypes, functionType);

        DbChangeType typeType = DbChangeTypeImpl.newDbChangeType(ChangeType.USERTYPE_STR, true, 1, "TYPE").build();
        changeTypes.add(typeType);

        return changeTypes.toImmutable();
    }

    @Override
    public boolean isDropOrderRequired() {
        return true;
    }

    @Override
    protected String getGrantTargetTypeStrDbSpecific(GrantTargetType grantTargetType) {
        // only user must have the blank defined; GROUP can still be specified
        switch (grantTargetType) {
        case USER:
            return "";
        default:
            return grantTargetType.name();
        }
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toLowerCase();
    }

    @Override
    public String getNullMarkerForCreateTable() {
        return "";
    }

    @Override
    public String getTimestampType() {
        return "TIMESTAMP";
    }

    @Override
    public ImmutableSet<String> getRequiredValidationObjectTypes() {
        // there is currently a quirk w/ PostgreSQL where view SQL definitions will not have the objects qualified w/
        // the schema name in the connection that creates the SQL; but in subsequent connections, it will be qualified.
        // Until we work out this issue, we will exclude views from PostgreSQL processing.
        return super.getRequiredValidationObjectTypes()
                .reject(Predicates.equal(ChangeType.VIEW_STR));
    }

    @Override
    public AbstractDdlReveng getDdlReveng() {
        return new PostgreSqlPgDumpReveng();
    }

    @Override
    public DbTranslationDialect getDbTranslationDialect(DbPlatform targetDialect) {
        if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.hsql.HsqlDbPlatform")) {
            return new PostgreSqlToHsqlTranslationDialect();
        } else {
            return super.getDbTranslationDialect(targetDialect);
        }
    }
}
