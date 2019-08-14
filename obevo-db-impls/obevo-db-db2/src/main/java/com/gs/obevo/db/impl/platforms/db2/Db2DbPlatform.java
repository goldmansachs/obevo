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
package com.gs.obevo.db.impl.platforms.db2;

import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.apps.reveng.Reveng;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbChangeTypeImpl;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Multimaps;

/**
 * DB2 platform information.
 */
public class Db2DbPlatform extends AbstractDbPlatform {
    public Db2DbPlatform() {
        super("DB2");
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return Db2AppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "com.ibm.db2.jcc.DB2Driver";
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

        DbChangeType spType = getChangeType(changeTypes, ChangeType.SP_STR);
        spType = DbChangeTypeImpl.newDbChangeType(spType).setGrantObjectQualifier("PROCEDURE").build();
        replaceChangeType(changeTypes, spType);

        return changeTypes.toImmutable();
    }

    @Override
    public ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder() {
        return super.getObjectExclusionPredicateBuilder().add(Multimaps.immutable.set.with(ChangeType.TABLE_STR, "EXPLAIN_%"));
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return StringFunctions.toUpperCase();
    }

    @Override
    public String getNullMarkerForCreateTable() {
        return "";  // for DB2 create statement, a blank implies NULL. Adding NULL explicitly was not allowed prior to
        // v9.7, so we leave this here
    }

    @Override
    public String getTimestampType() {
        return "TIMESTAMP";
    }

    @Override
    public DbTranslationDialect getDbTranslationDialect(DbPlatform targetDialect) {
        if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.h2.H2DbPlatform")) {
            return new Db2ToH2TranslationDialect();
        } else if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.hsql.HsqlDbPlatform")) {
            return new Db2ToHsqlTranslationDialect();
        } else {
            return super.getDbTranslationDialect(targetDialect);
        }
    }

    @Override
    public Reveng getDdlReveng() {
        return new Db2lookReveng();
    }

    @Override
    public String getTableSuffixSql(DbEnvironment env) {
        return env.getDefaultTablespace() != null ? "IN " + env.getDefaultTablespace() : "";
    }
}
