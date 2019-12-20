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
package com.gs.obevo.mongodb.impl

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.platform.ChangeTypeBehavior
import com.gs.obevo.api.platform.CommandExecutionContext
import com.gs.obevo.api.platform.DeployerRuntimeException
import com.gs.obevo.mongodb.api.appdata.MongoDbEnvironment
import org.slf4j.LoggerFactory

class MongoDeployBehavior(private val env: MongoDbEnvironment) : ChangeTypeBehavior {
    private val processExecutor = ProcessExecutor()
    private val mongoCommandLine = "mongo"

    override fun deploy(change: Change, cec: CommandExecutionContext) {
        val tempFile = createTempFile()
        tempFile.deleteOnExit()
        tempFile.writeText(change.convertedContent)

        val commandResult = processExecutor.runCommand("$mongoCommandLine ${env.host}:${env.port} ${tempFile.absolutePath}")
        LOG.info("Result: {}", commandResult)
        if (commandResult.exitCode != 0) {
            throw DeployerRuntimeException("Failed to deploy script: ${commandResult.stdoutText}")
        }
    }

    override fun undeploy(change: Change) {}

    override fun dropObject(change: Change, dropForRecreate: Boolean) {}

    override fun getDefinitionFromEnvironment(exampleChange: Change): String? {
        return null
    }

    fun validateMongoEnvironmentSetup() {
        // Main requirement: that the "mongo" CLI is available in the path
        val commandResult = processExecutor.runCommand("$mongoCommandLine --version")
        if (commandResult.exitCode != 0) {
            throw DeployerRuntimeException("Could not find the \"${mongoCommandLine}\" binary in your environment PATH variable." +
                    "Please set this prior to invoking Obevo. Error message: ${commandResult.stdoutText}")
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MongoDeployBehavior::class.java)
    }
}
