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
package com.gs.obevo.db.api.platform;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Timestamp;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.eclipse.collections.api.set.ImmutableSet;

public interface DbPlatform extends Platform {
    Class<? extends Driver> getDriverClass(DbEnvironment env);

    /**
     * This is for cases like in Sybase where teams had backed up db objects, and so it was created w/ a different name
     * in the object, but the underlying content points to the original name.
     * We only fire off this check in case the scan of the content does pick up the object name, as to be conservative
     */
    boolean isDuplicateCheckRequiredForReverseEngineering();

    /**
     * Returns the token that separates the schema from the object name.
     *
     * @since 6.0.0
     * @deprecated No longer used; use {@link #getSchemaPrefix(PhysicalSchema)} instead.
     */
    @Deprecated
    String getSchemaSeparator();

    /**
     * Determines whether this platform uses catalog + schema, or just a schema.
     *
     * @since 6.4.0
     */
    boolean isSubschemaSupported();

    /**
     * Returns the schema and subschema (if applicable) for the given schema with the appropriate separators appended.
     *
     * @since 6.0.0
     */
    String getSchemaPrefix(PhysicalSchema schema);

    /**
     * Returns the subschema section of the given PhysicalSchema with the separator appended, or a blank string in case
     * the subschema doesn't apply. Used for cases where we don't want to assume the schema is prefixed (for backwards-compatibility
     * reasons), while still supporting the subschema use case.
     *
     * @since 6.4.0
     */
    String getSubschemaPrefix(PhysicalSchema schema);

    /**
     * The token to use to indicate a null field when creating columns in a table. Some platforms use the NULL keyword,
     * and others imply a lack of keyword (e.g. "") as null.
     *
     * @since 6.0.0
     */
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

    String getArtifactDeploymentDDLString(String changeNameColumn,
            String changeTypeColumn,
            String deployUserIdColumn,
            String timeUpdatedColumn,
            String rollbackContentColumn,
            String tablespaceSql,
            DbEnvironment env);

    ImmutableSet<String> getRequiredValidationObjectTypes();

    /**
     * Still in beta...
     */
    AbstractDdlReveng getDdlReveng();

    String getTableSuffixSql(DbEnvironment env);

    /**
     * Given an object from the result set returned by the MapListHandler in JdbcHelper, this will return a long value.
     * This is simple for most DBMSs, but a few need special handling.
     */
    Long getLongValue(Object obj);

    /**
     * Given an object from the result set returned by the MapListHandler in JdbcHelper, this will return a timestamp value.
     * This is simple for most DBMSs, but a few need special handling.
     */
    Timestamp getTimestampValue(Object obj);

    /**
     * Given an object from the result set returned by the MapListHandler in JdbcHelper, this will return an integer value.
     * This is simple for most DBMSs, but a few need special handling.
     */
    Integer getIntegerValue(Object obj);
}
