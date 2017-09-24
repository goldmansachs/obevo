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

import java.util.List;

import com.gs.obevo.api.appdata.DeploySystem;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.factory.EnvironmentEnricher;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.appdata.Grant;
import com.gs.obevo.db.api.appdata.GrantTargetType;
import com.gs.obevo.db.api.appdata.Group;
import com.gs.obevo.db.api.appdata.Permission;
import com.gs.obevo.db.api.appdata.User;
import com.gs.obevo.db.api.platform.DbPlatform;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.Tokenizer;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.vfs2.FileType;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;

public class DbEnvironmentXmlEnricher implements EnvironmentEnricher<DbEnvironment> {
    private final DbPlatformConfiguration dbPlatformConfiguration;

    public DbEnvironmentXmlEnricher() {
        this.dbPlatformConfiguration = DbPlatformConfiguration.getInstance();
    }

    private FileObject getEnvFileToRead(FileObject sourcePath) {
        if (sourcePath.getType() == FileType.FILE && sourcePath.isReadable() && sourcePath.getName().getExtension().equalsIgnoreCase("xml")) {
            return sourcePath;
        } else {
            return sourcePath.getChild("system-config.xml");
        }
    }

    @Override
    public boolean isEnvironmentOfThisTypeHere(FileObject sourcePath) {
        return this.getEnvFileToRead(sourcePath) != null;
    }

    private static Function<HierarchicalConfiguration, Group> convertCfgToGroup(final Tokenizer tokenizer) {
        return new Function<HierarchicalConfiguration, Group>() {
            @Override
            public Group valueOf(HierarchicalConfiguration cfg) {
                return new Group(tokenizer.tokenizeString(cfg.getString("[@name]")));
            }
        };
    }

    private static Function<HierarchicalConfiguration, User> convertCfgToUser(final Tokenizer tokenizer) {
        return new Function<HierarchicalConfiguration, User>() {
            @Override
            public User valueOf(HierarchicalConfiguration cfg) {
                return new User(tokenizer.tokenizeString(cfg.getString("[@name]")), cfg.getString("[@password]"),
                        cfg.getBoolean("[@admin]", false));
            }
        };
    }

    private static ImmutableList<HierarchicalConfiguration> iterConfig(HierarchicalConfiguration c, String path) {
        List<HierarchicalConfiguration> list = c.configurationsAt(path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<HierarchicalConfiguration>empty();
    }

    private static ImmutableList<String> iterString(HierarchicalConfiguration c, String path) {
        List<String> list = c.getList(path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<String>empty();
    }

    private static Function<HierarchicalConfiguration, Permission> convertCfgToPermission(final Tokenizer tokenizer) {
        return new Function<HierarchicalConfiguration, Permission>() {
            @Override
            public Permission valueOf(HierarchicalConfiguration cfg) {
                return new Permission(cfg.getString("[@scheme]"),
                        iterConfig(cfg, "grant").collect(convertCfgToGrant(tokenizer)));
            }
        };
    }

    private static Function<HierarchicalConfiguration, Grant> convertCfgToGrant(final Tokenizer tokenizer) {
        return new Function<HierarchicalConfiguration, Grant>() {
            @Override
            public Grant valueOf(HierarchicalConfiguration cfg) {
                MutableListMultimap<GrantTargetType, String> grantTargetMap = Multimaps.mutable.list.empty();
                grantTargetMap.putAll(GrantTargetType.GROUP, iterString(cfg, "[@groups]").collect(tokenizer.tokenizeString()));
                grantTargetMap.putAll(GrantTargetType.USER, iterString(cfg, "[@users]").collect(tokenizer.tokenizeString()));
                return new Grant(
                        iterString(cfg, "[@privileges]").toImmutable(),
                        grantTargetMap.toImmutable()
                );
            }
        };
    }

    private Function<HierarchicalConfiguration, Schema> convertCfgToSchema(final DbPlatform systemDbPlatform) {
        return new Function<HierarchicalConfiguration, Schema>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Schema valueOf(HierarchicalConfiguration object) {
                String schemaName = object.getString("[@name]");
                boolean readOnly = object.getBoolean("[@readOnly]", false);

                MutableSetMultimap<String, String> excludedNameMap = Multimaps.mutable.set.empty();

                ImmutableList<HierarchicalConfiguration> excludes = iterConfig(object, "excludes");
                if (!excludes.isEmpty()) {
                    if (excludes.size() > 1) {
                        throw new IllegalArgumentException("Only expecting 1 excludes element under <schema>");
                    }
                    HierarchicalConfiguration excludesConfig = excludes.get(0);
                    if (excludesConfig != null) {
                        for (ChangeType changeType : systemDbPlatform.getChangeTypes()) {
                            ImmutableList<String> excludedNames = iterString(excludesConfig, changeType.getName().toLowerCase());
                            if (excludedNames.notEmpty()) {
                                excludedNameMap.putAll(changeType.getName(), excludedNames);
                            }

                            ImmutableList<String> excludedPatterns = iterString(excludesConfig, changeType.getName().toLowerCase() + "Pattern");
                            if (excludedPatterns.notEmpty()) {
                                throw new IllegalArgumentException("The <objectType>Pattern element is deprecated. Use just the <objectType> element w/ wildcards (% or *)");
                            }
                        }

                        if (iterString(excludesConfig, "procedure").notEmpty() || iterString(excludesConfig, "procedurePattern").notEmpty()) {
                            throw new IllegalArgumentException("The procedure and procedurePattern elements are no longer supported. Use <sp> only, with wildcards (% or *) if  needed");
                        }

                    }
                }

                return new Schema(schemaName, systemDbPlatform.getObjectExclusionPredicateBuilder().add(excludedNameMap.toImmutable()), readOnly);
            }
        };
    }

    private HierarchicalConfiguration getConfig(FileObject checkoutFolder) {
        XMLConfiguration config;
        try {
            config = new XMLConfiguration();
            config.load(getEnvFileToRead(checkoutFolder).getURLDa());
            return config;
        } catch (ConfigurationException exc) {
            throw new DeployerRuntimeException(exc);
        }
    }

    @Override
    public DeploySystem<DbEnvironment> readSystem(FileObject sourcePath) {
        HierarchicalConfiguration sysCfg = getConfig(sourcePath);

        DbPlatform systemDbPlatform = dbPlatformConfiguration.valueOf(sysCfg.getString("[@type]"));
        MutableList<String> sourceDirs = ListAdapter.adapt(sysCfg.getList("[@sourceDirs]"));
        ImmutableSet<String> acceptedExtensions = ListAdapter.adapt(sysCfg.getList("[@acceptedExtensions]")).toSet().toImmutable();

        MutableList<DbEnvironment> envList = Lists.mutable.empty();
        for (HierarchicalConfiguration envCfg : iterConfig(sysCfg, "environments.dbEnvironment")) {
            DbEnvironment dbEnv = new DbEnvironment();

            FileObject rootDir = sourcePath.getType() == FileType.FILE ? sourcePath.getParent() : sourcePath;

            // Use coreSourcePath and additionalSourceDirs here (instead of setSourceDirs) to facilitate any external integrations
            dbEnv.setCoreSourcePath(rootDir);
            dbEnv.setAdditionalSourceDirs(sourceDirs);
            dbEnv.setAcceptedExtensions(acceptedExtensions);

            dbEnv.setCleanBuildAllowed(envCfg.getBoolean("[@cleanBuildAllowed]", false));
            dbEnv.setDbHost(envCfg.getString("[@dbHost]"));
            dbEnv.setDbPort(envCfg.getInt("[@dbPort]", 0));
            dbEnv.setDbServer(envCfg.getString("[@dbServer]"));
            dbEnv.setDbSchemaPrefix(envCfg.getString("[@dbSchemaPrefix]"));
            dbEnv.setDbSchemaSuffix(envCfg.getString("[@dbSchemaSuffix]"));
            dbEnv.setDbDataSourceName(envCfg.getString("[@dbDataSourceName]"));
            dbEnv.setJdbcUrl(envCfg.getString("[@jdbcUrl]"));

            MutableMap<String, String> tokens = Maps.mutable.empty();

            for (HierarchicalConfiguration tok : iterConfig(envCfg, "tokens.token")) {
                tokens.put(tok.getString("[@key]"), tok.getString("[@value]"));
            }
            dbEnv.setTokens(tokens.toImmutable());

            // Allow the groups + users to be tokenized upfront for compatibility w/ the EnvironmentInfraSetup classes
            Tokenizer tokenizer = new Tokenizer(dbEnv.getTokens(), dbEnv.getTokenPrefix(), dbEnv.getTokenSuffix());
            dbEnv.setGroups(iterConfig(sysCfg, "groups.group").collect(convertCfgToGroup(tokenizer)));
            dbEnv.setUsers(iterConfig(sysCfg, "users.user").collect(convertCfgToUser(tokenizer)));

            if (envCfg.getString("[@driverClass]") != null) {
                dbEnv.setDriverClassName(envCfg.getString("[@driverClass]"));
            }

            dbEnv.setName(envCfg.getString("[@name]"));
            dbEnv.setDefaultUserId(envCfg.getString("[@defaultUserId]"));
            dbEnv.setDefaultPassword(envCfg.getString("[@defaultPassword]"));
            dbEnv.setDefaultTablespace(envCfg.getString("[@defaultTablespace]"));

            // TODO add include/exclude schemas functionality
            MutableList<Schema> schemaObjs = Lists.mutable.withAll(iterConfig(sysCfg, "schemas.schema"))
                    .collect(convertCfgToSchema(systemDbPlatform));

            MutableSet<String> schemasToInclude = iterString(envCfg, "includeSchemas").toSet();
            MutableSet<String> schemasToExclude = iterString(envCfg, "excludeSchemas").toSet();

            if (!schemasToInclude.isEmpty() && !schemasToExclude.isEmpty()) {
                throw new IllegalArgumentException("Environment " + dbEnv.getName() + " has includeSchemas ["
                        + schemasToInclude + "] and excludeSchemas [" + schemasToExclude
                        + "] defined; please only specify one of them");
            } else if (!schemasToInclude.isEmpty()) {
                schemaObjs = schemaObjs.select(Predicates.attributeIn(Schema.TO_NAME, schemasToInclude));
            } else if (!schemasToExclude.isEmpty()) {
                schemaObjs = schemaObjs.reject(Predicates.attributeIn(Schema.TO_NAME, schemasToExclude));
            }

            MutableMap<String, String> schemaNameOverrides = Maps.mutable.empty();
            MutableSet<String> schemaNames = schemaObjs.collect(Schema.TO_NAME).toSet();
            for (HierarchicalConfiguration schemaOverride : iterConfig(envCfg, "schemaOverrides.schemaOverride")) {
                String schema = schemaOverride.getString("[@schema]");
                if (schemaObjs.collect(Schema.TO_NAME).contains(schema)) {
                    schemaNameOverrides.put(schema, schemaOverride.getString("[@overrideValue]"));
                } else {
                    throw new IllegalArgumentException("Schema override definition value "
                            + schema + " is not defined in the schema list " + schemaNames + " for environment " + dbEnv.getName());
                }
            }

            dbEnv.setSchemaNameOverrides(schemaNameOverrides.toImmutable());
            // ensure that we only store the unique schema names here
            dbEnv.setSchemas(UnifiedSetWithHashingStrategy.newSet(HashingStrategies.fromFunction(Schema.TO_NAME), schemaObjs).toImmutable());

            dbEnv.setPersistToFile(envCfg.getBoolean("[@persistToFile]", false));
            dbEnv.setDisableAuditTracking(envCfg.getBoolean("[@disableAuditTracking]", false));

            dbEnv.setRollbackDetectionEnabled(
                    envCfg.getBoolean("[@rollbackDetectionEnabled]", sysCfg.getBoolean("[@rollbackDetectionEnabled]", true))
            );
            dbEnv.setAutoReorgEnabled(
                    envCfg.getBoolean("[@autoReorgEnabled]", sysCfg.getBoolean("[@autoReorgEnabled]", true))
            );
            dbEnv.setInvalidObjectCheckEnabled(
                    envCfg.getBoolean("[@invalidObjectCheckEnabled]", sysCfg.getBoolean("[@invalidObjectCheckEnabled]", true))
            );
            dbEnv.setReorgCheckEnabled(
                    envCfg.getBoolean("[@reorgCheckEnabled]", sysCfg.getBoolean("[@reorgCheckEnabled]", true))
            );
            dbEnv.setChecksumDetectionEnabled(
                    envCfg.getBoolean("[@checksumDetectionEnabled]", sysCfg.getBoolean("[@checksumDetectionEnabled]", false))
            );
            Integer metadataLineReaderVersion = envCfg.getInteger("[@metadataLineReaderVersion]", sysCfg.getInteger("[@metadataLineReaderVersion]", null));
            if (metadataLineReaderVersion != null) {
                dbEnv.setMetadataLineReaderVersion(metadataLineReaderVersion);
            }
            Integer csvVersion = envCfg.getInteger("[@csvVersion]", sysCfg.getInteger("[@csvVersion]", null));
            if (csvVersion != null) {
                dbEnv.setCsvVersion(csvVersion);
            }
            String sourceEncoding = envCfg.getString("[@sourceEncoding]", sysCfg.getString("[@sourceEncoding]"));
            if (sourceEncoding != null) {
                dbEnv.setSourceEncoding(sourceEncoding);
            }
            Integer legacyDirectoryStructureEnabledVersion = envCfg.getInteger("[@legacyDirectoryStructureEnabled]", sysCfg.getInteger("[@legacyDirectoryStructureEnabled]", null));
            if (legacyDirectoryStructureEnabledVersion != null) {
                dbEnv.setLegacyDirectoryStructureEnabledVersion(legacyDirectoryStructureEnabledVersion);
            }


            MutableMap<String, String> extraEnvAttrs = Maps.mutable.empty();
            for (String extraEnvAttr : dbPlatformConfiguration.getExtraEnvAttrs()) {
                String attrStr = "[@" + extraEnvAttr + "]";
                extraEnvAttrs.put(extraEnvAttr, envCfg.getString(attrStr, sysCfg.getString(attrStr)));
            }

            dbEnv.setExtraEnvAttrs(extraEnvAttrs.toImmutable());

            ImmutableList<HierarchicalConfiguration> envPermissions = iterConfig(envCfg, "permissions.permission");
            ImmutableList<HierarchicalConfiguration> sysPermissions = iterConfig(sysCfg, "permissions.permission");
            if (!envPermissions.isEmpty()) {
                dbEnv.setPermissions(envPermissions.collect(convertCfgToPermission(tokenizer)));
            } else if (!sysPermissions.isEmpty()) {
                dbEnv.setPermissions(sysPermissions.collect(convertCfgToPermission(tokenizer)));
            }

            DbPlatform platform;
            if (envCfg.getString("[@inMemoryDbType]") != null) {
                platform = dbPlatformConfiguration.valueOf(envCfg.getString("[@inMemoryDbType]"));
            } else {
                platform = systemDbPlatform;
            }

            dbEnv.setSystemDbPlatform(systemDbPlatform);
            dbEnv.setPlatform(platform);

            String delim = sysCfg.getString("[@dataDelimiter]");
            if (delim != null) {
                if (delim.length() == 1) {
                    dbEnv.setDataDelimiter(delim.charAt(0));
                } else {
                    throw new IllegalArgumentException("dataDelimiter must be 1 character long. instead, got ["
                            + delim + "]");
                }
            }

            String nullToken = sysCfg.getString("[@nullToken]");
            if (nullToken != null) {
                dbEnv.setNullToken(nullToken);
            }

            dbEnv.setAuditTableSql(getProperty(sysCfg, envCfg, "auditTableSql"));
            envList.add(dbEnv);
        }

        CollectionUtil.verifyNoDuplicates(envList, DbEnvironment.TO_NAME, "Invalid configuration from " + sourcePath + "; not expecting duplicate env names");
        return new DeploySystem<DbEnvironment>(envList);
    }

    /**
     * Returns the desired property from the config, giving preference to the environment but defaulting to the system if it exists.
     * The calls above should look to adopt this method.
     */
    private String getProperty(HierarchicalConfiguration sysCfg, HierarchicalConfiguration envCfg, String property) {
        String envVal = envCfg.getString(property);
        if (envVal != null) {
            return envVal;
        }

        String sysVal = sysCfg.getString(property);
        if (sysVal != null) {
            return sysVal;
        }

        return null;
    }
}
