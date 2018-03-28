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

import com.gs.obevo.api.appdata.DeploySystem;
import com.gs.obevo.api.appdata.Environment;
import com.gs.obevo.api.appdata.Schema;
import com.gs.obevo.api.factory.EnvironmentEnricher;
import com.gs.obevo.api.factory.PlatformConfiguration;
import com.gs.obevo.api.platform.ChangeType;
import com.gs.obevo.api.platform.Platform;
import com.gs.obevo.util.CollectionUtil;
import com.gs.obevo.util.VisibleForTesting;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.vfs2.FileType;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
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

public abstract class AbstractEnvironmentEnricher<E extends Environment> implements EnvironmentEnricher<E> {
    private static final Pattern PATTERN = Pattern.compile("[\\w+]+");
    private final PlatformConfiguration dbPlatformConfiguration = PlatformConfiguration.getInstance();

    @Override
    public DeploySystem<E> readSystem(HierarchicalConfiguration sysCfg, FileObject sourcePath) {
        Platform systemDbPlatform = dbPlatformConfiguration.valueOf(sysCfg.getString("[@type]"));

        MutableList<E> envList = Lists.mutable.empty();
        ImmutableList<HierarchicalConfiguration> envConfigs = iterConfig(sysCfg, "environments.dbEnvironment");
        if (envConfigs.isEmpty()) {
            envConfigs = iterConfig(sysCfg, "environments.environment");
        }

        for (HierarchicalConfiguration envCfg : envConfigs) {
            E dbEnv = createNewEnv();

            enrich(dbEnv, sysCfg, envCfg, sourcePath, systemDbPlatform);
            createEnv(dbEnv, sysCfg, envCfg, systemDbPlatform);
            envList.add(dbEnv);
        }

        CollectionUtil.verifyNoDuplicates(envList, Environment.TO_NAME, "Invalid configuration from " + sourcePath + "; not expecting duplicate env names");
        return new DeploySystem<E>(envList);
    }

    protected abstract E createNewEnv();

    protected abstract void createEnv(E env, HierarchicalConfiguration sysCfg, HierarchicalConfiguration envCfg, Platform systemDbPlatform);

    private void enrich(Environment dbEnv, HierarchicalConfiguration sysCfg, HierarchicalConfiguration envCfg, FileObject sourcePath, Platform systemDbPlatform) {
        MutableList<String> sourceDirs = ListAdapter.adapt(sysCfg.getList("[@sourceDirs]"));
        ImmutableSet<String> acceptedExtensions = ListAdapter.adapt(sysCfg.getList("[@acceptedExtensions]")).toSet().toImmutable();
        FileObject rootDir = sourcePath.getType() == FileType.FILE ? sourcePath.getParent() : sourcePath;

        // Use coreSourcePath and additionalSourceDirs here (instead of setSourceDirs) to facilitate any external integrations
        dbEnv.setCoreSourcePath(rootDir);
        dbEnv.setAdditionalSourceDirs(sourceDirs);
        dbEnv.setAcceptedExtensions(acceptedExtensions);

        dbEnv.setCleanBuildAllowed(envCfg.getBoolean("[@cleanBuildAllowed]", false));

        MutableMap<String, String> tokens = Maps.mutable.empty();

        for (HierarchicalConfiguration tok : iterConfig(envCfg, "tokens.token")) {
            tokens.put(tok.getString("[@key]"), tok.getString("[@value]"));
        }
        dbEnv.setTokens(tokens.toImmutable());

        dbEnv.setRollbackDetectionEnabled(
                envCfg.getBoolean("[@rollbackDetectionEnabled]", sysCfg.getBoolean("[@rollbackDetectionEnabled]", true))
        );

        Integer metadataLineReaderVersion = envCfg.getInteger("[@metadataLineReaderVersion]", sysCfg.getInteger("[@metadataLineReaderVersion]", null));
        if (metadataLineReaderVersion != null) {
            dbEnv.setMetadataLineReaderVersion(metadataLineReaderVersion);
        }

        String sourceEncoding = envCfg.getString("[@sourceEncoding]", sysCfg.getString("[@sourceEncoding]"));
        if (sourceEncoding != null) {
            dbEnv.setSourceEncoding(sourceEncoding);
        }
        Integer legacyDirectoryStructureEnabledVersion = envCfg.getInteger("[@legacyDirectoryStructureEnabled]", sysCfg.getInteger("[@legacyDirectoryStructureEnabled]", null));
        if (legacyDirectoryStructureEnabledVersion != null) {
            dbEnv.setLegacyDirectoryStructureEnabledVersion(legacyDirectoryStructureEnabledVersion);
        }

        enrichSchemas(dbEnv, sysCfg, envCfg, systemDbPlatform);
    }

    private void enrichSchemas(Environment dbEnv, HierarchicalConfiguration sysCfg, HierarchicalConfiguration envCfg, Platform systemDbPlatform) {
        dbEnv.setName(envCfg.getString("[@name]"));
        dbEnv.setDefaultUserId(envCfg.getString("[@defaultUserId]"));
        dbEnv.setDefaultPassword(envCfg.getString("[@defaultPassword]"));

        // Note - schemaNameValidation attribute should have been read as [@schemaNameValidation]. But some clients have already
        // pulled in this feature, so we'll have to leave it in.
        int schemaNameValidationVersion = envCfg.getInt("schemaNameValidation", sysCfg.getInt("schemaNameValidation", dbPlatformConfiguration.getFeatureToggleVersion("schemaNameValidation")));

        // TODO add include/exclude schemas functionality
        MutableList<Schema> schemaObjs = Lists.mutable.withAll(iterConfig(sysCfg, "schemas.schema"))
                .collect(convertCfgToSchema(systemDbPlatform, schemaNameValidationVersion));

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
    }

    private Function<HierarchicalConfiguration, Schema> convertCfgToSchema(final Platform systemDbPlatform, final int schemaNameValidation) {
        return new Function<HierarchicalConfiguration, Schema>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Schema valueOf(HierarchicalConfiguration object) {
                String schemaName = object.getString("[@name]");
                if (schemaNameValidation >= 2) {
                    validateSchemaName(schemaName);
                }
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

    @VisibleForTesting
    static void validateSchemaName(String schemaName) {
        if (!PATTERN.matcher(schemaName).matches()) {
            throw new IllegalArgumentException("SchemaName " + schemaName + " does not match regexp " + PATTERN.pattern());
        }
    }

    protected static ImmutableList<HierarchicalConfiguration> iterConfig(HierarchicalConfiguration c, String path) {
        List<HierarchicalConfiguration> list = c.configurationsAt(path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<HierarchicalConfiguration>empty();
    }

    protected static ImmutableList<String> iterString(HierarchicalConfiguration c, String path) {
        List<String> list = c.getList(path);
        return list != null ? ListAdapter.adapt(list).toImmutable() : Lists.immutable.<String>empty();
    }
}
