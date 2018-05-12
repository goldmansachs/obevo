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
package com.gs.obevo.api.factory;

import java.util.List;

import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.configuration2.FixedYAMLConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.NodeHandler;
import org.apache.commons.configuration2.tree.QueryResult;
import org.apache.commons.vfs2.FileType;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.ListAdapter;

public class XmlFileConfigReader implements FileConfigReader {
    private FileObject getEnvFileToRead(FileObject sourcePath) {
        if (sourcePath.getType() == FileType.FILE && sourcePath.isReadable()
                && (sourcePath.getName().getExtension().equalsIgnoreCase("xml") || sourcePath.getName().getExtension().equalsIgnoreCase("yaml"))) {
            return sourcePath;
        } else {
            return sourcePath.getChild("system-config.xml");
        }
    }

    @Override
    public boolean isEnvironmentOfThisTypeHere(FileObject sourcePath) {
        return this.getEnvFileToRead(sourcePath) != null;
    }

    @Override
    public HierarchicalConfiguration getConfig(FileObject checkoutFolder) {
        try {
            FileObject envFileToRead = getEnvFileToRead(checkoutFolder);
            if (envFileToRead.getName().getExtension().equals("xml")) {
                // For the XML lookup, we want all access to fall to attribute queries if we choose. To do that, we override
                // the expression engine.
                DefaultExpressionEngine engine = new DefaultExpressionEngine(DefaultExpressionEngineSymbols.DEFAULT_SYMBOLS) {
                    @Override
                    public <T> List<QueryResult<T>> query(T root, String key, NodeHandler<T> handler) {
                        List<QueryResult<T>> results = super.query(root, key, handler);
                        if (!results.isEmpty()) {
                            return results;
                        }

                        // If we find no results, fall back to the query that specifies the attribute handler
                        return super.query(root, this.attributeKey(null, key), handler);
                    }
                };

                XMLConfiguration configuration = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                        .configure(new Parameters().hierarchical()
                                .setURL(envFileToRead.getURLDa())
                                .setListDelimiterHandler(new DisabledListDelimiterHandler())
                                .setExpressionEngine(engine)
                        )
                        .getConfiguration();
                postProcess(configuration);
                return configuration;
            } else {
                return new FileBasedConfigurationBuilder<>(FixedYAMLConfiguration.class)
                        .configure(new Parameters().hierarchical()
                                .setURL(envFileToRead.getURLDa())
                                .setListDelimiterHandler(new DisabledListDelimiterHandler())
                        )
                        .getConfiguration();
            }
        } catch (ConfigurationException e) {
            throw new DeployerRuntimeException(e);
        }
    }

    /**
     * Backwards-compatible changes to ensure existing XML consumers are not negatively affected.
     */
    private void postProcess(XMLConfiguration sysCfg) {
        ImmutableSet<String> ignorableSysAttributes = Sets.immutable.of(
                "cleanBuildAllowed",
                "name",
                "defaultUserId",
                "defaultPassword",
                "dbHost",
                "dbPort",
                "dbServer",
                "dbSchemaPrefix",
                "dbSchemaSuffix",
                "dbDataSourceName",
                "jdbcUrl",
                "driverClass",
                "defaultTablespace",
                "persistToFile",
                "disableAuditTracking",
                "inMemoryDbType",
                "includeSchemas",
                "excludeSchemas"
        );
        ImmutableSet<String> ignorableSysNodes = Sets.immutable.of("excludeSchemas", "includeSchemas", "schemaOverrides", "tokens");
        final ImmutableSet<String> ignorableEnvNodes = Sets.immutable.of("groups", "users");
        final ImmutableSet<String> ignorableEnvAttributes = Sets.immutable.of("sourceDirs", "acceptedExtensions", "dataDelimiter", "nullToken");

        for (String ignorableAttribute : ignorableSysAttributes) {
            if (sysCfg.containsKey(ignorableAttribute)) {
                sysCfg.clearProperty(ignorableAttribute);
            }
        }
        for (String ignorableSysNode : ignorableSysNodes) {
            if (!sysCfg.configurationsAt(ignorableSysNode).isEmpty()) {
                sysCfg.clearTree(ignorableSysNode);
            }
        }

        for (String ignorableAttribute : ignorableEnvAttributes) {
            sysCfg.clearProperty("environments.environment[@" + ignorableAttribute + "]");
            sysCfg.clearProperty("environments.dbEnvironment[@" + ignorableAttribute + "]");
        }
        for (String ignorableSysNode : ignorableEnvNodes) {
            sysCfg.clearTree("environments.environment." + ignorableSysNode);
            sysCfg.clearTree("environments.dbEnvironment." + ignorableSysNode);
        }

        ImmutableList<HierarchicalConfiguration<ImmutableNode>> envConfigs = ListAdapter.adapt(sysCfg.configurationsAt("environments.dbEnvironment")).toImmutable();
        if (envConfigs.isEmpty()) {
            envConfigs = ListAdapter.adapt(sysCfg.configurationsAt("environments.environment")).toImmutable();
        }
        envConfigs.each(new Procedure<HierarchicalConfiguration<ImmutableNode>>() {
            @Override
            public void value(HierarchicalConfiguration<ImmutableNode> envCfg) {
                for (String ignorableAttribute : ignorableEnvAttributes) {
                    if (envCfg.containsKey(ignorableAttribute)) {
                        envCfg.clearProperty(ignorableAttribute);
                    }
                }
                for (String ignorableSysNode : ignorableEnvNodes) {
                    if (!envCfg.configurationsAt(ignorableSysNode).isEmpty()) {
                        envCfg.clearTree(ignorableSysNode);
                    }
                }
            }
        });
    }
}
