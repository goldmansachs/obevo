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
package com.gs.obevo.impl

import com.gs.obevo.api.appdata.Change
import com.gs.obevo.api.platform.DeployerRuntimeException
import com.gs.obevo.util.vfs.FileObject
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.vfs2.FileSystemException
import org.eclipse.collections.api.set.ImmutableSet
import java.io.IOException

/**
 * The strategy for when onboarding is enabled. Here, we should look to move failed changes into the exception folder/s
 * if the change fails, or to keep/move to the regular folder if it succeeds.
 */
internal class EnabledOnboardingStrategy : OnboardingStrategy {
    override fun handleSuccess(change: Change) {
        if (change.fileLocation.exists()) {
            val containingDir = change.fileLocation.parent

            if (containingDir!!.name.baseName == OnboardingStrategy.exceptionDir) {
                change.fileLocation.moveTo(change.fileLocation.parent!!.parent!!.resolveFile(change.fileLocation.name.baseName))
                val exceptionFile = change.fileLocation.parent!!.resolveFile(change.fileLocation.name.baseName + ".exception")
                exceptionFile!!.delete()
            }
        }
    }

    override fun handleException(change: Change, exc: Exception) {
        if (change.fileLocation.exists()) {
            val containingDir = change.fileLocation.parent

            val exceptionDir: FileObject?
            if (containingDir!!.name.baseName == OnboardingStrategy.exceptionDir) {
                exceptionDir = containingDir
            } else {
                exceptionDir = containingDir.resolveFile(OnboardingStrategy.exceptionDir)
                exceptionDir!!.createFolder()

                change.fileLocation.moveTo(exceptionDir.resolveFile(change.fileLocation.name.baseName))
            }

            val exceptionFile = exceptionDir.resolveFile(change.fileLocation.name.baseName + ".exception")
            exceptionFile!!.delete()
            exceptionFile.createFile()
            try {
                val outputStream = exceptionFile.content.getOutputStream(true)
                IOUtils.write(ExceptionUtils.getStackTrace(exc), outputStream)
                outputStream.close()
            } catch (e: FileSystemException) {
                throw DeployerRuntimeException(e)
            } catch (e: IOException) {
                throw DeployerRuntimeException(e)
            }

        }
    }

    override fun validateSourceDirs(sourceDirs: Iterable<FileObject>, schemaNames: ImmutableSet<String>) {
        // no need to validate when onboardingStrategy is enabled
    }
}
