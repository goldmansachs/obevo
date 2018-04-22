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
package com.gs.obevo.db.api.factory;

import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Grant;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.appdata.ServerDirectory;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.impl.AbstractEnvironmentEnricher;
import com.gs.obevo.util.Tokenizer;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;

public class DbEnvironmentXmlEnricher extends AbstractEnvironmentEnricher<DbEnvironment> {
    private final DbPlatformConfiguration dbPlatformConfiguration;

    public DbEnvironmentXmlEnricher() {
        this.dbPlatformConfiguration = DbPlatformConfiguration.getInstance();
    }

    @Override
    protected DbEnvironment createNewEnv() {
        return new DbEnvironment();
    }

    @Override
    protected void createEnv(DbEnvironment dbEnv, ImmutableHierarchicalConfiguration envCfg, Platform systemDbPlatformOrig) {
        DbPlatform systemDbPlatform = (DbPlatform) systemDbPlatformOrig;

        dbEnv.setDbHost(envCfg.getString("dbHost"));
        dbEnv.setDbPort(envCfg.getInt("dbPort", 0));
        dbEnv.setDbServer(envCfg.getString("dbServer"));
        dbEnv.setDbSchemaPrefix(envCfg.getString("dbSchemaPrefix"));
        dbEnv.setDbSchemaSuffix(envCfg.getString("dbSchemaSuffix"));
        dbEnv.setDbDataSourceName(envCfg.getString("dbDataSourceName"));
        dbEnv.setJdbcUrl(envCfg.getString("jdbcUrl"));

        // Allow the groups + users to be tokenized upfront for compatibility w/ the EnvironmentInfraSetup classes
        Tokenizer tokenizer = new Tokenizer(dbEnv.getTokens(), dbEnv.getTokenPrefix(), dbEnv.getTokenSuffix());
        dbEnv.setGroups(iterConfig(envCfg, "groups.group").collectWith(DbEnvironmentXmlEnricher::convertCfgToGroup, tokenizer));
        dbEnv.setUsers(iterConfig(envCfg, "users.user").collectWith(DbEnvironmentXmlEnricher::convertCfgToUser, tokenizer));
        dbEnv.setServerDirectories(iterConfig(envCfg, "serverDirectories.serverDirectory").collectWith(DbEnvironmentXmlEnricher::convertCfgToServerDirectory, tokenizer));

        if (envCfg.getString("driverClass") != null) {
            dbEnv.setDriverClassName(envCfg.getString("driverClass"));
        }

        dbEnv.setDefaultTablespace(envCfg.getString("defaultTablespace"));

        dbEnv.setPersistToFile(envCfg.getBoolean("persistToFile", false));
        dbEnv.setDisableAuditTracking(envCfg.getBoolean("disableAuditTracking", false));

        dbEnv.setAutoReorgEnabled(envCfg.getBoolean("autoReorgEnabled", true));
        dbEnv.setInvalidObjectCheckEnabled(envCfg.getBoolean("invalidObjectCheckEnabled", true));
        dbEnv.setReorgCheckEnabled(envCfg.getBoolean("reorgCheckEnabled", true));
        dbEnv.setChecksumDetectionEnabled(envCfg.getBoolean("checksumDetectionEnabled", false));
        Integer csvVersion = envCfg.getInteger("csvVersion", null);
        if (csvVersion != null) {
            dbEnv.setCsvVersion(csvVersion);
        }

        MutableMap<String, String> extraEnvAttrs = Maps.mutable.empty();
        for (String extraEnvAttr : dbPlatformConfiguration.getExtraEnvAttrs()) {
            extraEnvAttrs.put(extraEnvAttr, envCfg.getString(extraEnvAttr));
        }

        dbEnv.setExtraEnvAttrs(extraEnvAttrs.toImmutable());

        ImmutableList<ImmutableHierarchicalConfiguration> envPermissions = iterConfig(envCfg, "permissions.permission");
        if (!envPermissions.isEmpty()) {
            dbEnv.setPermissions(envPermissions.collectWith(DbEnvironmentXmlEnricher::convertCfgToPermission, tokenizer));
        }

        DbPlatform platform;
        if (envCfg.getString("inMemoryDbType") != null) {
            platform = dbPlatformConfiguration.valueOf(envCfg.getString("inMemoryDbType"));
        } else {
            platform = systemDbPlatform;
        }

        dbEnv.setSystemDbPlatform(systemDbPlatform);
        dbEnv.setPlatform(platform);

        String delim = envCfg.getString("dataDelimiter");
        if (delim != null) {
            if (delim.length() == 1) {
                dbEnv.setDataDelimiter(delim.charAt(0));
            } else {
                throw new IllegalArgumentException("dataDelimiter must be 1 character long. instead, got ["
                        + delim + "]");
            }
        }

        String nullToken = envCfg.getString("nullToken");
        if (nullToken != null) {
            dbEnv.setNullToken(nullToken);
        }

        dbEnv.setAuditTableSql(envCfg.getString("auditTableSql"));
    }

    private static Group convertCfgToGroup(ImmutableHierarchicalConfiguration cfg, Tokenizer tokenizer) {
        return new Group(tokenizer.tokenizeString(cfg.getString("name")));
    }

    private static User convertCfgToUser(ImmutableHierarchicalConfiguration cfg, Tokenizer tokenizer) {
        return new User(tokenizer.tokenizeString(cfg.getString("name")), cfg.getString("password"),
                cfg.getBoolean("admin", false));
    }

    private static ServerDirectory convertCfgToServerDirectory(ImmutableHierarchicalConfiguration cfg, Tokenizer tokenizer) {
        return new ServerDirectory(
                tokenizer.tokenizeString(cfg.getString("name")),
                tokenizer.tokenizeString(cfg.getString("directoryPath"))
        );
    }

    private static Permission convertCfgToPermission(ImmutableHierarchicalConfiguration cfg, Tokenizer tokenizer) {
        return new Permission(cfg.getString("scheme"),
                iterConfig(cfg, "grant").collect(_this -> convertCfgToGrant(_this, tokenizer)));
    }

    private static Grant convertCfgToGrant(ImmutableHierarchicalConfiguration cfg, Tokenizer tokenizer) {
        MutableListMultimap<GrantTargetType, String> grantTargetMap = Multimaps.mutable.list.empty();
        grantTargetMap.putAll(GrantTargetType.GROUP, iterString(cfg, "groups").collect(tokenizer::tokenizeString));
        grantTargetMap.putAll(GrantTargetType.USER, iterString(cfg, "users").collect(tokenizer::tokenizeString));
        return new Grant(
                iterString(cfg, "privileges").toImmutable(),
                grantTargetMap.toImmutable()
        );
    }
}
