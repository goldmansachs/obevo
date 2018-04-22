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
package com.gs.obevo.db.impl.platforms;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Timestamp;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ObjectTypeAndNamePredicateBuilder;
import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.factory.EnvironmentEnricher;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.ChangeTypeImpl;
import com.gs.obevo.api.platform.DeployerAppContext;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.factory.DbEnvironmentXmlEnricher;
import com.gs.obevo.db.api.factory.DbPlatformConfiguration;
import com.gs.obevo.db.api.platform.DbChangeType;
import com.gs.obevo.db.api.platform.DbChangeTypeImpl;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.db.api.platform.DbTranslationDialect;
import com.gs.obevo.db.api.platform.SqlExecutor;
import com.gs.obevo.db.apps.reveng.AbstractDdlReveng;
import com.gs.obevo.db.apps.reveng.ChangeEntry;
import com.gs.obevo.dbmetadata.api.DbMetadataManager;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to define the common features for most {@link DbPlatform} implementations.
 */
public abstract class AbstractDbPlatform implements DbPlatform {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDbPlatform.class);

    private final String name;
    private final Class<? extends DeployerAppContext> appContextBuilderClass;
    private final String defaultDriverClassName;

    private final ImmutableList<ChangeType> changeTypes;
    private final ImmutableMap<String, ChangeType> changeTypeMap;

    protected AbstractDbPlatform(String name) {
        this.name = name;
        this.appContextBuilderClass = initializeAppContextBuilderClass();
        this.defaultDriverClassName = initializeDefaultDriverClassName();
        this.changeTypes = initializeChangeTypes();
        this.changeTypeMap = changeTypes.groupByUniqueKey(ChangeType::getName).toImmutable();
    }

    @Override
    public Class<? extends DeployerAppContext> getAppContextBuilderClass() {
        return this.appContextBuilderClass;
    }

    /**
     * Returns the default DeployerAppContext class. This is overridable to let various layers of subclasses override
     * the default builder as needed.
     */
    protected abstract Class<? extends DeployerAppContext> initializeAppContextBuilderClass();

    /**
     * Returns the default driver fully-qualified class name. This is overridable to let various layers of subclasses override
     * the default builder as needed.
     */
    protected abstract String initializeDefaultDriverClassName();

    protected ImmutableList<ChangeType> initializeChangeTypes() {
        return Lists.immutable.with(
                DbChangeTypeImpl.newDbChangeType(ChangeType.TABLE_STR, false, 10, "TABLE").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.MIGRATION_STR, false, 41, null).setEnrichableForDependenciesInText(false).build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.INDEX_STR, false, 49, null).build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.FOREIGN_KEY_STR, false, 50, null).build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.TRIGGER_INCREMENTAL_OLD_STR, false, 51, null).setDirectoryName("invalidTriggerDoNotUse").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.VIEW_STR, true, 20, "VIEW").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.SP_STR, true, 30, "PROCEDURE").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.SEQUENCE_STR, true, 1, "SEQUENCE").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.FUNCTION_STR, true, 15, "FUNCTION").setDirectoryNameOld("func").build(),
                DbChangeTypeImpl.newDbChangeType(ChangeType.TRIGGER_STR, true, 52, "TRIGGER").setDirectoryName("trigger").build(),
                ChangeTypeImpl.newChangeType(ChangeType.STATICDATA_STR, true, 40).setDirectoryNameOld("data").build()
        );
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public Class<? extends Driver> getDriverClass(DbEnvironment env) {
        try {
            return (Class<? extends Driver>) Class.forName(defaultDriverClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isDuplicateCheckRequiredForReverseEngineering() {
        return false;
    }

    @Override
    public boolean isDropOrderRequired() {
        return false;
    }

    @Override
    public final ImmutableList<ChangeType> getChangeTypes() {
        return changeTypes;
    }

    @Override
    public final ChangeType getChangeType(String name) {
        return Validate.notNull(changeTypeMap.get(name), "No change type " + name + " registered in this platform");
    }

    @Override
    public ImmutableSet<String> getAcceptedExtensions() {
        return Sets.immutable.with("sql", "ddl", "dml", "tbl", "vw", "view", "spc", "sp", "txt", "csv", "proc", "dat");
    }

    @Override
    public EnvironmentEnricher getEnvironmentEnricher() {
        return new DbEnvironmentXmlEnricher();
    }

    @Override
    public ImmutableSet<String> getRequiredValidationObjectTypes() {
        return Sets.immutable.with(ChangeType.TABLE_STR, ChangeType.VIEW_STR, ChangeType.SP_STR, ChangeType.FUNCTION_STR);
    }

    @Override
    public AbstractDdlReveng getDdlReveng() {
        throw new UnsupportedOperationException("No ddl reveng implementation defined for this platform: " + this.getClass());
    }

    @Override
    public String getTableSuffixSql(DbEnvironment env) {
        return "";
    }

    @Override
    public ObjectTypeAndNamePredicateBuilder getObjectExclusionPredicateBuilder() {
        return new ObjectTypeAndNamePredicateBuilder(ObjectTypeAndNamePredicateBuilder.FilterType.EXCLUDE);
    }

    @Override
    public final boolean hasChangeType(String name) {
        return changeTypeMap.containsKey(name);
    }

    /**
     * Internal method meant to help subclasses modify the changeType list.
     */
    protected DbChangeType getChangeType(MutableList<ChangeType> inputs, String changeTypeName) {
        return (DbChangeType) inputs.detect(_this -> _this.getName().equals(changeTypeName));
    }

    /**
     * Internal method meant to help subclasses modify the changeType list.
     */
    protected void replaceChangeType(MutableCollection<ChangeType> inputs, ChangeType changeType) {
        inputs.removeIf((Predicate<ChangeType>) _this -> _this.getName().equals(changeType.getName()));
        inputs.with(changeType);
    }

    @Override
    public final String getGrantTargetTypeStr(GrantTargetType grantTargetType, String grantTarget) {
        if ("public".equalsIgnoreCase(grantTarget)) {
            return "";
        } else {
            return this.getGrantTargetTypeStrDbSpecific(grantTargetType);
        }
    }

    protected String getGrantTargetTypeStrDbSpecific(GrantTargetType grantTargetType) {
        return grantTargetType.name();
    }

    @Override
    @Deprecated
    public String getSchemaSeparator() {
        return ".";
    }

    @Override
    public String getSchemaPrefix(PhysicalSchema schema) {
        return schema.getPhysicalName() + ".";
    }

    @Override
    public String getSubschemaPrefix(PhysicalSchema schema) {
        return "";
    }

    @Override
    public boolean isSubschemaSupported() {
        return false;
    }

    @Override
    public String getNullMarkerForCreateTable() {
        return "NULL";
    }

    @Override
    public String getTimestampType() {
        return "DATETIME";
    }

    @Override
    public String getTextType() {
        return "VARCHAR(2048)";
    }

    @Override
    public String getBigIntType() {
        return "BIGINT";
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
        if (this.getClass().equals(targetDialect.getClass())) {
            return new DefaultDbTranslationDialect();
        } else {
            throw new IllegalArgumentException("No translation defined from " + this.getClass() + " to " +
                    targetDialect.getClass());
        }
    }

    @Override
    public String getArtifactDeploymentDDLString(final String changeNameColumn,
            final String changeTypeColumn,
            final String deployUserIdColumn,
            final String timeUpdatedColumn,
            final String rollbackContentColumn,
            final String tablespaceSql,
            final DbEnvironment env) {
        return String.format("CREATE TABLE ARTIFACTDEPLOYMENT ( \n" +
                "    ARTFTYPE    \tVARCHAR(31) NOT NULL,\n" +
                "    " + changeNameColumn + "\tVARCHAR(255) NOT NULL,\n" +
                "    OBJECTNAME  \tVARCHAR(255) NOT NULL,\n" +
                "    ACTIVE      \tINTEGER %1$s,\n" +
                "    " + changeTypeColumn + "  \tVARCHAR(255) %1$s,\n" +
                "    CONTENTHASH \tVARCHAR(255) %1$s,\n" +
                "    DBSCHEMA    \tVARCHAR(255) %1$s,\n" +
                "    " + deployUserIdColumn + "    \tVARCHAR(32) %1$s,\n" +
                "    " + timeUpdatedColumn + "    \t" + env.getPlatform().getTimestampType() + " %1$s,\n" +
                "    " + rollbackContentColumn + "\t" + env.getPlatform().getTextType() + " %1$s,\n" +
                "    CONSTRAINT ARTDEFPK PRIMARY KEY(" + changeNameColumn + ",OBJECTNAME)\n" +
                ") %2$s\n", env.getPlatform().getNullMarkerForCreateTable(), tablespaceSql);
    }

    /**
     * Most cases, we can just cast to timestamp and be done. Override this if the DBMS needs special handling
     */
    @Override
    public Timestamp getTimestampValue(Object obj) {
        return (Timestamp) obj;
    }

    /**
     * Because Sybase needs to store longs as a bigint, we add the handling for it here.
     */
    @Override
    public Long getLongValue(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else {
            throw new IllegalArgumentException("Not a Number: " + obj);
        }
    }

    /**
     * Because Oracle needs to store longs as a bigint, we add the handling for it here.
     */
    @Override
    public Integer getIntegerValue(Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else {
            throw new IllegalArgumentException("Not a Number: " + obj);
        }
    }

    @Override
    public DbMetadataManager getDbMetadataManager() {
        DbPlatformConfiguration dbPlatformConfiguration = DbPlatformConfiguration.getInstance();
        ImmutableHierarchicalConfiguration platformConfig = dbPlatformConfiguration.getPlatformConfig(getName());
        String dbMetadataManagerClass = platformConfig.getString("dbMetadataManager.class");
        try {
            return (DbMetadataManager) Class.forName(dbMetadataManagerClass).newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
