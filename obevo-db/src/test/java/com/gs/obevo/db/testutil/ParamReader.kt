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
package com.gs.obevo.db.testutil

import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.api.factory.XmlFileConfigReader
import com.gs.obevo.db.api.appdata.DbEnvironment
import com.gs.obevo.db.api.factory.DbEnvironmentXmlEnricher
import com.gs.obevo.db.api.platform.DbDeployerAppContext
import com.gs.obevo.db.impl.core.jdbc.JdbcDataSourceFactory
import com.gs.obevo.util.inputreader.Credential
import com.gs.obevo.util.vfs.FileRetrievalMode
import org.apache.commons.configuration2.BaseHierarchicalConfiguration
import org.apache.commons.configuration2.HierarchicalConfiguration
import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration
import org.apache.commons.configuration2.tree.ImmutableNode
import org.apache.commons.lang3.StringUtils
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction
import org.slf4j.LoggerFactory
import java.sql.Driver
import javax.sql.DataSource

/**
 * Utility for reading in the test suite parameters from an input property file.
 */
class ParamReader(
        private val rootConfig: HierarchicalConfiguration<ImmutableNode>
) {
    private constructor(configPath: String) : this(getFromPath(configPath))

    private val sysConfigs: List<ImmutableHierarchicalConfiguration>
        get() = rootConfig.immutableConfigurationsAt("environments.environment")

    val appContextParams: Collection<Array<Any>>
        get() = sysConfigs.map(Companion::getAppContext).map { arrayOf(it as Any) }

    val appContextAndJdbcDsParams: Collection<Array<Any>>
        get() = getAppContextAndJdbcDsParams(1)

    val jdbcDsAndSchemaParams: Collection<Array<Any>>
        get() = getJdbcDsAndSchemaParams(1)

    fun getJdbcDsAndSchemaParams(numConnections: Int): Collection<Array<Any>> {
        return sysConfigs.map { config ->
            getAppContext(config).valueOf(1).setupEnvInfra()  // setup the environment upfront, since the calling tests here do not require the schema

            arrayOf(getJdbcDs(config, numConnections), PhysicalSchema.parseFromString(config.getString("metaschema")))
        }
    }

    private fun getAppContextAndJdbcDsParams(numConnections: Int): Collection<Array<Any>> {
        return sysConfigs.map { arrayOf(getAppContext(it), getJdbcDs(it, numConnections)) }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ParamReader::class.java)

        @JvmStatic
        fun fromPath(configPath: String, defaultPath: String): ParamReader {
            return fromPath(if (StringUtils.isNotBlank(configPath)) configPath else defaultPath)
        }

        @JvmStatic
        fun fromPath(configPath: String): ParamReader {
            return ParamReader(configPath)
        }

        private fun getFromPath(configPath: String): HierarchicalConfiguration<ImmutableNode> {
            val configFile = FileRetrievalMode.CLASSPATH.resolveSingleFileObject(configPath)
            if (configFile != null && configFile.exists()) {
                return XmlFileConfigReader().getConfig(configFile) as HierarchicalConfiguration<ImmutableNode>
            } else {
                LOG.info("Test parameter file {} not found; will not run tests", configPath)
                return BaseHierarchicalConfiguration()
            }
        }

        private fun getAppContext(config: ImmutableHierarchicalConfiguration): IntToObjectFunction<DbDeployerAppContext> {
            return IntToObjectFunction { stepNumber -> replaceStepNumber(config.getString("sourcePath"), stepNumber, config).buildAppContext() }
        }

        private fun getJdbcDs(config: ImmutableHierarchicalConfiguration, numConnections: Int): DataSource {
            val jdbcUrl = config.getString("jdbcUrl")
            val username = config.getString("defaultUserId")
            val password = config.getString("defaultPassword")
            val driver = config.getString("driverClass")
            return JdbcDataSourceFactory.createFromJdbcUrl(
                    Class.forName(driver) as Class<out Driver>,
                    jdbcUrl,
                    Credential(username, password),
                    numConnections)
        }

        private fun replaceStepNumber(input: String, stepNumber: Int, config: ImmutableHierarchicalConfiguration): DbEnvironment {
            val stepPath = input.replace("\${stepNumber}", stepNumber.toString())
            val sourcePath = FileRetrievalMode.CLASSPATH.resolveSingleFileObject(stepPath)
            checkNotNull(sourcePath, { "Could not find directory path $stepPath" })

            return DbEnvironmentXmlEnricher().readEnvironment(config, sourcePath)
        }
    }
}
