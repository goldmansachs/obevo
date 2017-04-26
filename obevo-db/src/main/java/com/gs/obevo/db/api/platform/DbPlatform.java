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
package com.gs.obevo.db.api.platform;

import java.sql.Connection;
import java.sql.Driver;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.eclipse.collections.api.set.ImmutableSet;

public interface DbPlatform extends Platform {
    Class<? extends DeployerAppContext> getAppContextBuilderClass();

    Class<? extends Driver> getDriverClass(DbEnvironment env);

    /**
     * This is for cases like in Sybase where teams had backed up db objects, and so it was created w/ a different name
     * in the object, but the underlying content points to the original name.
     * We only fire off this check in case the scan of the content does pick up the object name, as to be conservative
     * @return
     */
    boolean isDuplicateCheckRequiredForReverseEngineering();

    String getSchemaSeparator();

    String getSchemaPrefix(PhysicalSchema schema);

    String getNullMarkerForCreateTable();

    String getTimestampType();

    String getTextType();

    String getBigIntType();

    /**
     * Returns the grantTargetType intended for the input grant target. Also see See the grant pattern
     * in {@link DbChangeType#getGrantObjectQualifier()}
     */
    String getGrantTargetTypeStr(GrantTargetType grantTargetType, String grantTarget);

    DbMetadataManager getDbMetadataManager();

    void doTryBlockForArtifact(Connection conn, SqlExecutor sqlExecutor, Change artifact);

    void doFinallyBlockForArtifact(Connection conn, SqlExecutor sqlExecutor, Change artifact);

    /**
     * Do stuff like adding annotations to the reveng file if needed (e.g. adding annotations to handling quoted
     * identifiers for sybase)
     */
    void postProcessChangeForRevEng(ChangeEntry change, String sql);

    DbTranslationDialect getDbTranslationDialect(DbPlatform targetDialect);

    String getArtifactDeploymentDDLString(final String changeNameColumn,
            final String changeTypeColumn,
            final String deployUserIdColumn,
            final String timeUpdatedColumn,
            final String rollbackContentColumn,
            final String tablespaceSql,
            final DbEnvironment env);

    ImmutableSet<String> getAcceptedExtensions();

    ImmutableSet<String> getRequiredValidationObjectTypes();

    /**
     * Still in beta...
     */
    AbstractDdlReveng getDdlReveng();

    String getTableSuffixSql(DbEnvironment env);
}
