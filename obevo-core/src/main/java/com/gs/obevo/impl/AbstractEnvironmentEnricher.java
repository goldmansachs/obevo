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
package com.gs.obevo.impl;

import java.util.List;
import java.util.regex.Pattern;

import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.factory.EnvironmentEnricher;
import com.gs.obevo.api.factory.PlatformConfiguration;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileType;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.factory.HashingStrategySets;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public abstract class AbstractEnvironmentEnricher<E extends Environment> implements EnvironmentEnricher<E> {
    private static final Pattern PATTERN = Pattern.compile("[\\w+]+");
    private final PlatformConfiguration dbPlatformConfiguration = PlatformConfiguration.getInstance();

    @Override
    public ImmutableCollection<E> readSystem(final HierarchicalConfiguration sysCfg, final FileObject sourcePath) {
        final Platform systemDbPlatform = dbPlatformConfiguration.valueOf(sysCfg.getString("type"));

        // Check for dbEnvironments first for backwards-compatibility
        ImmutableList<HierarchicalConfiguration> envConfigs = iterConfigMutable(sysCfg, "environments.dbEnvironment");
        if (envConfigs.isEmpty()) {
            envConfigs = iterConfigMutable(sysCfg, "environments.environment");
        }

        ImmutableList<E> envList = envConfigs.collect(new Function<HierarchicalConfiguration, E>() {
            @Override
            public E valueOf(HierarchicalConfiguration envCfg) {
                E dbEnv = AbstractEnvironmentEnricher.this.createNewEnv();

                // combining the sys and env configurations before passing to downstream methods so that we can support only having env configs passed in
                CombinedConfiguration combinedConfiguration = new CombinedConfiguration(new OverrideCombiner());
                combinedConfiguration.addConfiguration(envCfg);
                combinedConfiguration.addConfiguration(sysCfg);
                combinedConfiguration.setExpressionEngine(sysCfg.getExpressionEngine());

                AbstractEnvironmentEnricher.this.enrich(dbEnv, combinedConfiguration, sourcePath, systemDbPlatform);
                AbstractEnvironmentEnricher.this.createEnv(dbEnv, combinedConfiguration, systemDbPlatform);
                return dbEnv;
            }
        });

        CollectionUtil.verifyNoDuplicates(envList, new Function<E, Object>() {
            @Override
            public Object valueOf(E e) {
                return e.getName();
            }
        }, "Invalid configuration from " + sourcePath + "; not expecting duplicate env names");
        return envList;
    }

    @Override
    public E readEnvironment(ImmutableHierarchicalConfiguration combinedConfiguration, FileObject sourcePath) {
        E dbEnv = createNewEnv();

        Platform systemDbPlatform = dbPlatformConfiguration.valueOf(combinedConfiguration.getString("type"));
        enrich(dbEnv, combinedConfiguration, sourcePath, systemDbPlatform);
        createEnv(dbEnv, combinedConfiguration, systemDbPlatform);

        return dbEnv;
    }

    protected abstract E createNewEnv();

    protected abstract void createEnv(E env, ImmutableHierarchicalConfiguration envCfg, Platform systemDbPlatform);

    private void enrich(Environment dbEnv, ImmutableHierarchicalConfiguration envCfg, FileObject sourcePath, Platform systemDbPlatform) {
        ImmutableList<String> sourceDirs = iterString(envCfg, "sourceDirs");
        ImmutableSet<String> acceptedExtensions = iterString(envCfg, "acceptedExtensions").toSet().toImmutable();
        FileObject rootDir = sourcePath.getType() == FileType.FILE ? sourcePath.getParent() : sourcePath;

        // Use coreSourcePath and additionalSourceDirs here (instead of setSourceDirs) to facilitate any external integrations
        dbEnv.setCoreSourcePath(rootDir);
        dbEnv.setAdditionalSourceDirs(sourceDirs);
        dbEnv.setAcceptedExtensions(acceptedExtensions);

        dbEnv.setCleanBuildAllowed(envCfg.getBoolean("cleanBuildAllowed", false));

        MutableMap<String, String> tokens = iterConfig(envCfg, "tokens.token")
                .toMap(new Function<ImmutableHierarchicalConfiguration, String>() {
                    @Override
                    public String valueOf(ImmutableHierarchicalConfiguration tok) {
                        return tok.getString("key");
                    }
                }, new Function<ImmutableHierarchicalConfiguration, String>() {
                    @Override
                    public String valueOf(ImmutableHierarchicalConfiguration tok) {
                        return tok.getString("value");
                    }
                });
        dbEnv.setTokens(tokens.toImmutable());

        dbEnv.setRollbackDetectionEnabled(envCfg.getBoolean("rollbackDetectionEnabled", true));

        Integer metadataLineReaderVersion = envCfg.getInteger("metadataLineReaderVersion", null);
        if (metadataLineReaderVersion != null) {
            dbEnv.setMetadataLineReaderVersion(metadataLineReaderVersion);
        }

        dbEnv.setForceEnvInfraSetup(envCfg.getBoolean("forceEnvInfraSetup", null));

        String sourceEncoding = envCfg.getString("sourceEncoding");
        if (sourceEncoding != null) {
            dbEnv.setSourceEncoding(sourceEncoding);
        }
        Integer legacyDirectoryStructureEnabledVersion = envCfg.getInteger("legacyDirectoryStructureEnabled", null);
        if (legacyDirectoryStructureEnabledVersion != null) {
            dbEnv.setLegacyDirectoryStructureEnabledVersion(legacyDirectoryStructureEnabledVersion);
        }

        enrichSchemas(dbEnv, envCfg, systemDbPlatform);
    }

    private void enrichSchemas(Environment dbEnv, ImmutableHierarchicalConfiguration envCfg, final Platform systemDbPlatform) {
        dbEnv.setName(envCfg.getString("name"));
        dbEnv.setDefaultUserId(envCfg.getString("defaultUserId"));
        dbEnv.setDefaultPassword(envCfg.getString("defaultPassword"));

        final int schemaNameValidationVersion = envCfg.getInt("schemaNameValidation", dbPlatformConfiguration.getFeatureToggleVersion("schemaNameValidation"));

        // TODO add include/exclude schemas functionality
        ImmutableList<Schema> schemaObjs = iterConfig(envCfg, "schemas.schema")
                .collect(new Function<ImmutableHierarchicalConfiguration, Schema>() {
                    @Override
                    public Schema valueOf(ImmutableHierarchicalConfiguration cfg) {
                        return AbstractEnvironmentEnricher.this.convertCfgToSchema(cfg, systemDbPlatform, schemaNameValidationVersion);
                    }
                });

        final MutableSet<String> schemasToInclude = iterString(envCfg, "includeSchemas").toSet();
        final MutableSet<String> schemasToExclude = iterString(envCfg, "excludeSchemas").toSet();

        if (!schemasToInclude.isEmpty() && !schemasToExclude.isEmpty()) {
            throw new IllegalArgumentException("Environment " + dbEnv.getName() + " has includeSchemas ["
                    + schemasToInclude + "] and excludeSchemas [" + schemasToExclude
                    + "] defined; please only specify one of them");
        } else if (!schemasToInclude.isEmpty()) {
            schemaObjs = schemaObjs.select(new Predicate<Schema>() {
                @Override
                public boolean accept(Schema it) {
                    return schemasToInclude.contains(it.getName());
                }
            });
        } else if (!schemasToExclude.isEmpty()) {
            schemaObjs = schemaObjs.reject(new Predicate<Schema>() {
                @Override
                public boolean accept(Schema it) {
                    return schemasToExclude.contains(it.getName());
                }
            });
        }

        MutableMap<String, String> schemaNameOverrides = Maps.mutable.empty();
        ImmutableSet<String> schemaNames = schemaObjs.collect(new Function<Schema, String>() {
            @Override
            public String valueOf(Schema schema1) {
                return schema1.getName();
            }
        }).toSet().toImmutable();
        for (ImmutableHierarchicalConfiguration schemaOverride : iterConfig(envCfg, "schemaOverrides.schemaOverride")) {
            String schema = schemaOverride.getString("schema");
            if (schemaNames.contains(schema)) {
                schemaNameOverrides.put(schema, schemaOverride.getString("overrideValue"));
            } else {
                throw new IllegalArgumentException("Schema override definition value "
                        + schema + " is not defined in the schema list " + schemaNames + " for environment " + dbEnv.getName());
            }
        }

        dbEnv.setSchemaNameOverrides(schemaNameOverrides.toImmutable());
        // ensure that we only store the unique schema names here
        dbEnv.setSchemas(HashingStrategySets.mutable.ofAll(HashingStrategies.fromFunction(new Function<Schema, String>() {
            @Override
            public String valueOf(Schema schema) {
                return schema.getName();
            }
        }), schemaObjs).toImmutable());
    }

    private Schema convertCfgToSchema(ImmutableHierarchicalConfiguration object, final Platform systemDbPlatform, final int schemaNameValidation) {
        String schemaName = object.getString("name");
        if (schemaNameValidation >= 2) {
            validateSchemaName(schemaName);
        }
        boolean readOnly = object.getBoolean("readOnly", false);

        MutableSetMultimap<String, String> excludedNameMap = Multimaps.mutable.set.empty();

        ImmutableList<ImmutableHierarchicalConfiguration> excludes = iterConfig(object, "excludes");
        if (!excludes.isEmpty()) {
            if (excludes.size() > 1) {
                throw new IllegalArgumentException("Only expecting 1 excludes element under <schema>");
            }
            ImmutableHierarchicalConfiguration excludesConfig = excludes.get(0);
            if (excludesConfig != null) {
                for (ChangeType changeType : systemDbPlatform.getChangeTypes()) {
                    ImmutableList<String> excludedNames = iterListString(excludesConfig, changeType.getName().toLowerCase());
                    if (excludedNames.notEmpty()) {
                        excludedNameMap.putAll(changeType.getName(), excludedNames);
                    }

                    ImmutableList<String> excludedPatterns = iterListString(excludesConfig, changeType.getName().toLowerCase() + "Pattern");
                    if (excludedPatterns.notEmpty()) {
                        throw new IllegalArgumentException("The <objectType>Pattern element is deprecated. Use just the <objectType> element w/ wildcards (% or *)");
                    }
                }

                if (iterListString(excludesConfig, "procedure").notEmpty() || iterListString(excludesConfig, "procedurePattern").notEmpty()) {
                    throw new IllegalArgumentException("The procedure and procedurePattern elements are no longer supported. Use <sp> only, with wildcards (% or *) if  needed");
                }
            }
        }

        return new Schema(schemaName, systemDbPlatform.getObjectExclusionPredicateBuilder().add(excludedNameMap.toImmutable()), readOnly);
    }

    @VisibleForTesting
    static void validateSchemaName(String schemaName) {
        if (!PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("SchemaName " + schemaName + " does not match regexp " + PATTERN.pattern());
        }
    }

    protected static ImmutableList<ImmutableHierarchicalConfiguration> iterConfig(ImmutableHierarchicalConfiguration c, String path) {
        List<ImmutableHierarchicalConfiguration> list = c.immutableConfigurationsAt(path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<ImmutableHierarchicalConfiguration>empty();
    }

    private static ImmutableList<HierarchicalConfiguration> iterConfigMutable(HierarchicalConfiguration c, String path) {
        List<HierarchicalConfiguration> list = c.configurationsAt(path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<HierarchicalConfiguration>empty();
    }

    protected static ImmutableList<String> iterString(ImmutableHierarchicalConfiguration c, String path) {
        String string = c.getString(path);
        if (!StringUtils.isBlank(string)) {
            String[] parts = string.trim().split("\\s*,\\s*");
            return ArrayAdapter.adapt(parts).toImmutable();
        }

        return Lists.immutable.empty();
    }

    private static ImmutableList<String> iterListString(ImmutableHierarchicalConfiguration c, String path) {
        List<String> list = c.getList(String.class, path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<String>empty();
    }
}
