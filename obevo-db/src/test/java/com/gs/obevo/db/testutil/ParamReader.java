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
package com.gs.obevo.db.testutil;

import java.sql.Driver;
import java.util.Collection;
import java.util.Objects;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.api.factory.XmlFileConfigReader;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentXmlEnricher;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import com.gs.obevo.util.vfs.FileObject;
import com.gs.obevo.util.vfs.FileRetrievalMode;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for reading in the test suite parameters from an input property file.
 */
public class ParamReader {
    private static final Logger LOG = LoggerFactory.getLogger(ParamReader.class);

    private final HierarchicalConfiguration<ImmutableNode> rootConfig;

    public static ParamReader fromPath(String configPath) {
        return fromPath(configPath, null);
    }

    public static ParamReader fromPath(String configPath, String defaultPath) {
        return new ParamReader(!StringUtils.isBlank(configPath) ? configPath : defaultPath);
    }

    private ParamReader(String configPath) {
        FileObject configFile = FileRetrievalMode.CLASSPATH.resolveSingleFileObject(configPath);
        if (configFile != null && configFile.exists()) {
            this.rootConfig = new XmlFileConfigReader().getConfig(configFile);
        } else {
            LOG.info("Test parameter file {} not found; will not run tests", configPath);
            this.rootConfig = new BaseHierarchicalConfiguration();
        }
    }

    private ParamReader(HierarchicalConfiguration rootConfig) {
        this.rootConfig = Objects.requireNonNull(rootConfig);
    }

    private MutableList<ImmutableHierarchicalConfiguration> getSysConfigs() {
        return ListAdapter.adapt(rootConfig.immutableConfigurationsAt("environments.environment"));
    }

    public Collection<Object[]> getAppContextParams() {
        return getSysConfigs().collect(new Function<ImmutableHierarchicalConfiguration, Object[]>() {
            @Override
            public Object[] valueOf(ImmutableHierarchicalConfiguration config) {
                return new Object[] { getAppContext(config) };
            }
        });
    }

    public Collection<Object[]> getAppContextAndJdbcDsParams() {
        return getAppContextAndJdbcDsParams(1);
    }

    private Collection<Object[]> getAppContextAndJdbcDsParams(final int numConnections) {
        return getSysConfigs().collect(new Function<ImmutableHierarchicalConfiguration, Object[]>() {
            @Override
            public Object[] valueOf(ImmutableHierarchicalConfiguration config) {
                return new Object[] {
                        getAppContext(config),
                        getJdbcDs(config, numConnections)
                };
            }
        });
    }

    public Collection<Object[]> getJdbcDsAndSchemaParams() {
        return getJdbcDsAndSchemaParams(1);
    }

    public Collection<Object[]> getJdbcDsAndSchemaParams(final int numConnections) {
        return getSysConfigs().collect(new Function<ImmutableHierarchicalConfiguration, Object[]>() {
            @Override
            public Object[] valueOf(ImmutableHierarchicalConfiguration config) {
                final PhysicalSchema schema = PhysicalSchema.parseFromString(config.getString("metaschema"));

                return new Object[] { getJdbcDs(config, numConnections), schema };
            }
        });
    }

    private static IntToObjectFunction<DbDeployerAppContext> getAppContext(final ImmutableHierarchicalConfiguration config) {
        return new IntToObjectFunction<DbDeployerAppContext>() {
            @Override
            public DbDeployerAppContext valueOf(int stepNumber) {
                return replaceStepNumber(config.getString("sourcePath"), stepNumber, config).buildAppContext();
            }
        };
    }

    private static DataSource getJdbcDs(final ImmutableHierarchicalConfiguration config, final int numConnections) {
        String jdbcUrl = config.getString("jdbcUrl");
        final String username = config.getString("defaultUserId");
        final String password = config.getString("defaultPassword");
        final String driver = config.getString("driverClass");
        try {
            return JdbcDataSourceFactory.createFromJdbcUrl(
                    (Class<? extends Driver>) Class.forName(driver),
                    jdbcUrl,
                    new Credential(username, password),
                    numConnections);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static DbEnvironment replaceStepNumber(String input, int stepNumber, ImmutableHierarchicalConfiguration config) {
        String stepPath = input.replace("${stepNumber}", String.valueOf(stepNumber));
        FileObject sourcePath = FileRetrievalMode.CLASSPATH.resolveSingleFileObject(stepPath);

        DbEnvironmentXmlEnricher enricher = new DbEnvironmentXmlEnricher();
        return enricher.readEnvironment(config, sourcePath);
    }
}
