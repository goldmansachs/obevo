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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Driver;
import java.util.Collection;

import javax.sql.DataSource;

import com.gs.obevo.api.appdata.PhysicalSchema;
import com.gs.obevo.db.api.appdata.DbEnvironment;
import com.gs.obevo.db.api.factory.DbEnvironmentFactory;
import com.gs.obevo.db.api.platform.DbDeployerAppContext;
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory;
import com.gs.obevo.util.inputreader.Credential;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for reading in the test suite parameters from an input property file.
 */
public class ParamReader {
    private static final Logger LOG = LoggerFactory.getLogger(ParamReader.class);

    private final Config rootConfig;
    private final String dbType;
    private final Config defaultConfig;

    public ParamReader(Config rootConfig, String dbType, Config defaultConfig) {
        this.rootConfig = rootConfig;
        this.dbType = dbType;
        this.defaultConfig = defaultConfig;
    }

    private MutableList<Config> getSysConfigs() {
        if (rootConfig.isEmpty()) {
            return Lists.mutable.empty();
        }
        final Config instanceConfig = rootConfig.getConfig(dbType);
        return SetAdapter.adapt(instanceConfig.root().keySet()).collect(new Function<String, Config>() {
            @Override
            public Config valueOf(String envName) {
                return instanceConfig.getConfig(envName)
                        .withFallback(defaultConfig)
                        .withValue("envName", ConfigValueFactory.fromAnyRef(envName))
                        .resolve();
            }
        }).toList();
    }

    public Collection<Object[]> getAppContextParams() {
        return getSysConfigs().flatCollect(new Function<Config, ListIterable<Object[]>>() {
            @Override
            public ListIterable<Object[]> valueOf(final Config config) {
                ListIterable<IntToObjectFunction<DbDeployerAppContext>> appContexts = getAppContext(config);
                return appContexts.collect(new Function<IntToObjectFunction<DbDeployerAppContext>, Object[]>() {
                    @Override
                    public Object[] valueOf(IntToObjectFunction<DbDeployerAppContext> appContext) {
                        return new Object[] { appContext };
                    }
                });
            }
        });
    }

    public Collection<Object[]> getAppContextAndJdbcDsParams() {
        return getAppContextAndJdbcDsParams(1);
    }

    private Collection<Object[]> getAppContextAndJdbcDsParams(final int numConnections) {
        return getSysConfigs().flatCollect(new Function<Config, ListIterable<Object[]>>() {
            @Override
            public ListIterable<Object[]> valueOf(final Config config) {
                ListIterable<IntToObjectFunction<DbDeployerAppContext>> appContexts = getAppContext(config);
                return appContexts.collect(new Function<IntToObjectFunction<DbDeployerAppContext>, Object[]>() {
                    @Override
                    public Object[] valueOf(IntToObjectFunction<DbDeployerAppContext> appContext) {
                        return new Object[] {
                                appContext,
                                getJdbcDs(config, numConnections)
                        };
                    }
                });
            }
        });
    }

    public Collection<Object[]> getJdbcDsAndSchemaParams() {
        return getJdbcDsAndSchemaParams(1);
    }

    public Collection<Object[]> getJdbcDsAndSchemaParams(final int numConnections) {
        return getSysConfigs().collect(new Function<Config, Object[]>() {
            @Override
            public Object[] valueOf(Config config) {
                final PhysicalSchema schema = PhysicalSchema.parseFromString(config.getString("schema"));

                return new Object[] { getJdbcDs(config, numConnections), schema };
            }
        });
    }

    private static ListIterable<IntToObjectFunction<DbDeployerAppContext>> getAppContext(final Config config) {
        final String sourcePath = config.getString("sourcePath");
        String env = getStringOptional(config, "env");

        final String[] envArgs = env != null ? env.split(",") : new String[] { null };
        final String username = getStringOptional(config, "username");
        final String password = getStringOptional(config, "password");
        return ArrayAdapter.adapt(envArgs).collect(new Function<String, IntToObjectFunction<DbDeployerAppContext>>() {
            @Override
            public IntToObjectFunction<DbDeployerAppContext> valueOf(final String envArg) {
                return new IntToObjectFunction<DbDeployerAppContext>() {
                    @Override
                    public DbDeployerAppContext valueOf(int stepNumber) {
                        String stepSourcePath = replaceStepNumber(sourcePath, stepNumber, config);

                        DbEnvironment dbEnvironment = DbEnvironmentFactory.getInstance().readOneFromSourcePath(stepSourcePath, envArg != null ? new String[] { envArg } : new String[0]);
                        if (username != null && password != null) {
                            return dbEnvironment.buildAppContext(username, password);
                        } else {
                            return dbEnvironment.buildAppContext();
                        }
                    }
                };
            }
        });
    }

    private static DataSource getJdbcDs(final Config config, final int numConnections) {
        String jdbcUrl = config.getString("jdbcUrl");
        final String username = config.getString("username");
        final String password = config.getString("password");
        final String driver = config.getString("driver");
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

    private static String getStringOptional(Config config, String prop) {
        return config.hasPath(prop) ? config.getString(prop) : null;
    }

    private static String replaceStepNumber(String input, int stepNumber, Config config) {
        String stepPath = input.replace("${stepNumber}", String.valueOf(stepNumber));

        String templatePath = "deploytest/system-config.xml.ftl";
        URL templateUrl = ParamReader.class.getClassLoader().getResource(templatePath);
        if (templateUrl != null) {
            try {
                File obevoTempDir = new File(SystemUtils.JAVA_IO_TMPDIR, "obevo");
                obevoTempDir.mkdirs();
                File outputTemplate = File.createTempFile("obevo-system-config", ".xml", obevoTempDir);
                MutableMap<String, Object> params = Maps.mutable.empty();
                params.put("sourceDir", stepPath);
                params.put("envName", config.getString("envName"));
                if (config.hasPath("jdbcUrl")) {
                    params.put("jdbcUrl", config.getString("jdbcUrl"));
                }
                if (config.hasPath("envschema1")) {
                    params.put("schema1", config.getString("envschema1"));
                }
                if (config.hasPath("envschema2")) {
                    params.put("schema2", config.getString("envschema2"));
                }
                if (config.hasPath("dbDataSourceName")) {
                    params.put("dbDataSourceName", config.getString("dbDataSourceName"));
                }
                if (config.hasPath("dbServer")) {
                    params.put("dbServer", config.getString("dbServer"));
                }
                if (config.hasPath("driver")) {
                    params.put("driver", config.getString("driver"));
                }
                populateConfig(config, params, "envattrs");
                populateConfig(config, params, "sysattrs");
                populateConfig(config, params, "schemas");
                populateConfig(config, params, "logicalSchemas");

                TestTemplateUtil.getInstance().writeTemplate(templatePath, params, outputTemplate);
                LOG.info("System Config was written to {}", outputTemplate);
                return outputTemplate.getAbsolutePath();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return stepPath;
    }

    private static void populateConfig(Config config, MutableMap<String, Object> params, String attr) {
        if (config.hasPath(attr)) {
            Config attrs = config.getConfig(attr);
            MutableMap<String, String> attrsMap = Maps.mutable.empty();
            for (String key : attrs.root().keySet()) {
                attrsMap.put(key, config.getString(attr + "." + key));
            }
            params.put(attr, attrsMap);
        }
    }
}
