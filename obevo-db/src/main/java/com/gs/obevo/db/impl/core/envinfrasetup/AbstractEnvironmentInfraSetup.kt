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
package com.gs.obevo.db.impl.core.envinfrasetup

import com.gs.obevo.api.appdata.PhysicalSchema
import com.gs.obevo.api.platform.ChangeType
import com.gs.obevo.api.platform.DeployMetrics
import com.gs.obevo.db.api.appdata.*
import com.gs.obevo.db.api.platform.DbChangeTypeBehavior
import com.gs.obevo.db.impl.core.jdbc.DataAccessException
import com.gs.obevo.db.impl.core.jdbc.JdbcHelper
import com.gs.obevo.dbmetadata.api.DaDirectory
import com.gs.obevo.dbmetadata.api.DaExtension
import com.gs.obevo.dbmetadata.api.DbMetadataManager
import com.gs.obevo.impl.ChangeTypeBehaviorRegistry
import com.gs.obevo.impl.DeployMetricsCollector
import org.eclipse.collections.api.RichIterable
import org.eclipse.collections.api.list.ImmutableList
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/**
 * Setup mode:
 * - do setups and fail
 * - do setups and don't fail if error
 * - only check and fail if missing
 * - only check and don't fail if missing
 */
open class AbstractEnvironmentInfraSetup(
        protected val env: DbEnvironment,
        protected val ds: DataSource,
        private val deployMetricsCollector: DeployMetricsCollector,
        protected val dbMetadataManager: DbMetadataManager,
        private val changeTypeBehaviorRegistry: ChangeTypeBehaviorRegistry
) : EnvironmentInfraSetup<DbEnvironment> {
    protected val jdbc: JdbcHelper = JdbcHelper()

    override fun setupEnvInfra(failOnSetupException: Boolean, forceCreation: Boolean) {
        try {
            ds.connection.use { conn ->
                for (schema in env.physicalSchemas) {
                    val schemaValue = dbMetadataManager.getDatabaseOptional(schema)
                    if (schemaValue == null) {
                        if (forceCreation) {
                            LOG.info("Creating schema/database {}", schema.physicalName)
                            createSchema(conn, schema)
                        } else if (failOnSetupException) {
                            handleException(failOnSetupException, "Failed to create schema $schema", "schemaCreationFailed")
                        }
                    }
                }

                if (env.platform.isSubschemaSupported) {
                    for (schema in env.physicalSchemas) {
                        handleGroups(conn, schema, failOnSetupException, forceCreation)
                        handleUsers(conn, schema, failOnSetupException, forceCreation)
                    }
                } else {
                    handleExtensions(conn, failOnSetupException, forceCreation)

                    handleGroups(conn, null, failOnSetupException, forceCreation)
                    handleUsers(conn, null, failOnSetupException, forceCreation)

                    handleDirectories(conn, failOnSetupException, forceCreation)
                }
            }
        } catch (e: SQLException) {
            handleException(failOnSetupException, "Failed to open connection", "openConnectionFailed", e)
        }

    }

    private fun handleExtensions(conn: Connection, failOnSetupException: Boolean, forceCreation: Boolean) {
        handleObject(conn, null, failOnSetupException, forceCreation,
                "extension",
                { _ -> dbMetadataManager.extensionsOptional },
                DaExtension::getName,
                DbEnvironment::getExtensions,
                Extension::name,
                { conn, extension, _ -> createExtension(conn, extension) }
        )
    }

    private fun handleGroups(conn: Connection, physicalSchema: PhysicalSchema?, failOnSetupException: Boolean, forceCreation: Boolean) {
        handleObject(conn, physicalSchema, failOnSetupException, forceCreation,
                "group",
                dbMetadataManager::getGroupNamesOptional,
                { it },
                DbEnvironment::getGroups,
                Group::name,
                this::createGroup
        )
    }

    private fun handleUsers(conn: Connection, physicalSchema: PhysicalSchema?, failOnSetupException: Boolean, forceCreation: Boolean) {
        handleObject(conn, physicalSchema, failOnSetupException, forceCreation,
                "user",
                dbMetadataManager::getUserNamesOptional,
                { it },
                DbEnvironment::getUsers,
                User::name,
                this::createUser
        )
    }

    private fun handleDirectories(conn: Connection, failOnSetupException: Boolean, forceCreation: Boolean) {
        val changeTypeName = ChangeType.DIRECTORY

        handleObject(conn, null, failOnSetupException, forceCreation,
                changeTypeName,
                { _ -> dbMetadataManager.directoriesOptional },
                DaDirectory::getName,
                DbEnvironment::getServerDirectories,
                ServerDirectory::name,
                this::createDirectory
        )

        if (env.serverDirectories.notEmpty()) {
            val tableChangeType = changeTypeBehaviorRegistry.getChangeTypeBehavior(changeTypeName) as DbChangeTypeBehavior

            env.serverDirectories.forEach {
                tableChangeType.applyGrants(conn, null, it.name, env.getPermissions(changeTypeName))
            }
        }
    }

    /**
     * General pattern for creating DB objects during infrastructure setup.
     */
    private fun <SRCOBJ, DBOBJ> handleObject(conn: Connection, physicalSchema: PhysicalSchema?, failOnSetupException: Boolean, forceCreation: Boolean, objectTypeName: String, getDbObjects: (PhysicalSchema?) -> RichIterable<DBOBJ>?, getDbObjectName: (DBOBJ) -> String, getSourceObjects: (DbEnvironment) -> ImmutableList<SRCOBJ>, getSourceObjectName: (SRCOBJ) -> String, createObject: (Connection, SRCOBJ, PhysicalSchema?) -> Unit) {
        val existingObjects: RichIterable<DBOBJ>?
        try {
            existingObjects = getDbObjects(physicalSchema)
            if (existingObjects == null) {
                LOG.debug("{} setup is not supported in this implementation; skipping...", objectTypeName)
                return
            }
        } catch (e: DataAccessException) {
            handleException(failOnSetupException, "$objectTypeName validation query failed", objectTypeName + "QueryFailed", e)
            return
        }

        LOG.debug("{} objects existing in DB: {}", objectTypeName, existingObjects)

        val existingObjectNames = existingObjects.collect(getDbObjectName).collect { s -> s.toLowerCase() }.toSet().toImmutable()

        val sourceObjects = getSourceObjects(env)
        LOG.debug("{} objects from configuration: {}", objectTypeName, sourceObjects)

        val missingObjects = sourceObjects.reject { existingObjectNames.contains(getSourceObjectName(it).toLowerCase()) }

        if (forceCreation) {
            for (missingObject in missingObjects) {
                LOG.info("Creating missing {} {}", objectTypeName, missingObject)
                try {
                    createObject(conn, missingObject, physicalSchema)
                } catch (e: RuntimeException) {
                    handleException(failOnSetupException,
                            "Failed to create $objectTypeName $missingObject",
                            objectTypeName + "CreationFailure",
                            e
                    )
                }

            }
        } else if (missingObjects.notEmpty()) {
            handleException(failOnSetupException,
                    "The following $objectTypeName objects were not found in your environment: $missingObjects",
                    objectTypeName + "InConfigButNotInDb"
            )
        }
    }

    private fun handleException(failOnSetupException: Boolean, errorMessage: String, metricsMessage: String, e: Exception? = null) {
        if (failOnSetupException) {
            throw IllegalArgumentException(errorMessage, e)
        } else {
            LOG.warn("$errorMessage: {}", e!!.message)
            LOG.warn("Will proceed with deployment as you have configured this to just be a warning")
            deployMetricsCollector.addMetric(DeployMetrics.WARNINGS_PREFIX + ".envSetup." + metricsMessage, errorMessage)
        }
    }

    protected open fun createSchema(conn: Connection, schema: PhysicalSchema) {
        LOG.info("Schema creation is not supported; skipping this step")
    }

    protected open fun createExtension(conn: Connection, directory: Extension) {
        LOG.info("Extension creation is not supported; skipping this step")
    }

    protected open fun createGroup(conn: Connection, group: Group, physicalSchema: PhysicalSchema?) {
        LOG.info("Group creation is not supported; skipping this step")
    }

    protected open fun createUser(conn: Connection, user: User, physicalSchema: PhysicalSchema?) {
        LOG.info("User creation is not supported; skipping this step")
    }

    protected open fun createDirectory(conn: Connection, directory: ServerDirectory, physicalSchema: PhysicalSchema?) {
        LOG.info("Directory creation is not supported; skipping this step")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AbstractEnvironmentInfraSetup::class.java)
    }
}
