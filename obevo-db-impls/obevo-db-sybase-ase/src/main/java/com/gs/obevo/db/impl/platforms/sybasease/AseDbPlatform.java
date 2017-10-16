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
package com.gs.obevo.db.impl.platforms.sybasease;

import java.sql.Connection;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.platform.DbChangeTypeImpl;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.AseDdlgenReveng;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.db.impl.core.reader.TextMarkupDocumentReader;
import com.gs.obevo.db.impl.platforms.AbstractDbPlatform;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;

/**
 * {@link DbPlatform} implementation for Sybase ASE.
 */
public class AseDbPlatform extends AbstractDbPlatform {
    public AseDbPlatform() {
        super("SYBASE_ASE");
    }

    @Override
    public Class<? extends DeployerAppContext> initializeAppContextBuilderClass() {
        return SybaseAseAppContext.class;
    }

    @Override
    protected String initializeDefaultDriverClassName() {
        return "com.sybase.jdbc3.jdbc.SybDriver";
    }

    @Override
    protected ImmutableList<ChangeType> initializeChangeTypes() {
        return super.initializeChangeTypes().newWithAll(Lists.immutable.with(
                DbChangeTypeImpl.newDbChangeType(ChangeType.RULE_STR, true, 2, "RULE").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.USERTYPE_STR, true, 3, "DOMAIN").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.DEFAULT_STR, true, 5, "DEFAULT").build()
        ));
    }

    /**
     * Sybase allows the object names to differ from the generated file names in case of duplicates; hence, we enable.
     * @return
     */
    @Override
    public boolean isDuplicateCheckRequiredForReverseEngineering() {
        return true;
    }

    @Override
    public ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder() {
        return super.getObjectExclusionPredicateBuilder().add(Multimaps.immutable.set.with(
                ChangeType.TABLE_STR, "rs_%",
                ChangeType.SP_STR, "rs_%"
        ));
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
    public String getSchemaPrefix(PhysicalSchema schema) {
        return schema.getPhysicalName() + "." + ObjectUtils.defaultIfNull(schema.getSubschema(), "") + ".";
    }

    @Override
    public String getSubschemaPrefix(PhysicalSchema schema) {
        return schema.getSubschema() != null ? schema.getSubschema() + "." : "";
    }

    @Override
    @Deprecated
    public String getSchemaSeparator() {
        return "..";
    }

    @Override
    public boolean isSubschemaSupported() {
        return true;
    }

    @Override
    public void doTryBlockForArtifact(Connection conn, final SqlExecutor sqlExecutor, Change artifact) {
        // See here for why we do this:
        // http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc38151.1510/html/iqrefbb/CACIIHCI.htm
        // In short - JDBC defaults this to ON, but isql had this to OFF
        // We have this toggle to handle the legacy DB cases that started off deploying their scripts via isql, and
        // need to transition to this product, which deploys things via JDBC
        if (artifact.getMetadataSection() != null
                && artifact.getMetadataSection().isTogglePresent(TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS)) {
            sqlExecutor.getJdbcTemplate().update(conn, "SET quoted_identifier OFF");
        }
    }

    @Override
    public void doFinallyBlockForArtifact(Connection conn, final SqlExecutor sqlExecutor, Change artifact) {
        if (artifact.getMetadataSection() != null
                && artifact.getMetadataSection().isTogglePresent(TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS)) {
            sqlExecutor.getJdbcTemplate().update(conn, "SET quoted_identifier ON");
        }
    }

    @Override
    public void postProcessChangeForRevEng(ChangeEntry change, String sql) {
        if (sql.contains("\"")) {
            change.addMetadataAnnotation(TextMarkupDocumentReader.TOGGLE_DISABLE_QUOTED_IDENTIFIERS);
        }
    }

    @Override
    public DbTranslationDialect getDbTranslationDialect(DbPlatform targetDialect) {
        if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.h2.H2DbPlatform")) {
            return new AseToH2TranslationDialect();
        } else if (targetDialect.getClass().getName().equals("com.gs.obevo.db.impl.platforms.hsql.HsqlDbPlatform")) {
            return new AseToHsqlTranslationDialect();
        } else {
            return super.getDbTranslationDialect(targetDialect);
        }
    }

    @Override
    public String getTextType() {
        return "TEXT";
    }

    @Override
    public String getBigIntType() {
        return "NUMERIC(19,0)";  // bigint isn't supported until ASE 15.x
    }

    @Override
    public AbstractDdlReveng getDdlReveng() {
        return new AseDdlgenReveng();
    }

    @Override
    public String getTableSuffixSql(DbEnvironment env) {
        return " LOCK DATAROWS";
    }
}
