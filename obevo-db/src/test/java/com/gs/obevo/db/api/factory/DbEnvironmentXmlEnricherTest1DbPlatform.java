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
package com.gs.obevo.db.api.factory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Timestamp;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Here just for the unit test example
 */
public class DbEnvironmentXmlEnricherTest1DbPlatform implements DbPlatform {
    @Override
    public String getName() {
        return "test1";
    }

    @Override
    public ImmutableList<ChangeType> getChangeTypes() {
        return Lists.immutable.with(
                newChangeType(ChangeType.TABLE_STR),
                newChangeType(ChangeType.VIEW_STR),
                newChangeType(ChangeType.SP_STR)
        );
    }

    private ChangeType newChangeType(String name) {
        ChangeType changeType = mock(ChangeType.class);
        when(changeType.getName()).thenReturn(name);
        return changeType;
    }

    @Override
    public Class<? extends Driver> getDriverClass(DbEnvironment env) {
        return null;
    }

    @Override
    public boolean isDuplicateCheckRequiredForReverseEngineering() {
        return false;
    }

    @Override
    public Class<? extends DeployerAppContext> getAppContextBuilderClass() {
        return null;
    }

    @Override
    public String getSchemaSeparator() {
        return null;
    }

    @Override
    public String getSchemaPrefix(PhysicalSchema schema) {
        return null;
    }

    @Override
    public String getNullMarkerForCreateTable() {
        return null;
    }

    @Override
    public String getTimestampType() {
        return null;
    }

    @Override
    public String getTextType() {
        return null;
    }

    @Override
    public String getBigIntType() {
        return null;
    }

    @Override
    public String getGrantTargetTypeStr(GrantTargetType grantTargetType, String grantTarget) {
        return null;
    }

    @Override
    public DbMetadataManager getDbMetadataManager() {
        return null;
    }

    @Override
    public void doTryBlockForArtifact(Connection conn, SqlExecutor sqlExecutor, Change artifact) {

    }

    @Override
    public void doFinallyBlockForArtifact(Connection conn, SqlExecutor sqlExecutor, Change artifact) {

    }

    @Override
    public void postProcessChangeForRevEng(ChangeEntry change, String sql) {

    }

    @Override
    public DbTranslationDialect getDbTranslationDialect(DbPlatform targetDialect) {
        return null;
    }

    @Override
    public String getArtifactDeploymentDDLString(String changeNameColumn, String changeTypeColumn, String deployUserIdColumn, String timeUpdatedColumn, String rollbackContentColumn, String tablespaceSql, DbEnvironment env) {
        return null;
    }

    @Override
    public ChangeType getChangeType(String name) {
        return null;
    }

    @Override
    public boolean isDropOrderRequired() {
        return false;
    }

    @Override
    public boolean hasChangeType(String name) {
        return false;
    }

    @Override
    public Function<String, String> convertDbObjectName() {
        return null;
    }

    @Override
    public ImmutableSet<String> getAcceptedExtensions() {
        return Sets.immutable.with("ext1", "ext2");
    }

    @Override
    public ImmutableSet<String> getRequiredValidationObjectTypes() {
        return Sets.immutable.with("TABLE");
    }

    @Override
    public ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder() {
        return new ObjectTypeAndNamePredicateBuilder(ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE);
    }

    @Override
    public AbstractDdlReveng getDdlReveng() {
        return null;
    }

    @Override
    public String getTableSuffixSql(DbEnvironment env) {
        return null;
    }

    @Override
    public Long getLongValue(Object obj) {
        return null;
    }

    @Override
    public Timestamp getTimestampValue(Object obj) {
        return null;
    }

    @Override
    public Integer getIntegerValue(Object obj) {
        return null;
    }
}
