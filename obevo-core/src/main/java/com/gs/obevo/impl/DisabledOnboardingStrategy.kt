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
import com.gs.obevo.api.platform.DaConstants
import com.gs.obevo.util.vfs.BasicFileSelector
import com.gs.obevo.util.vfs.FileObject
import org.apache.commons.vfs2.FileFilter
import org.eclipse.collections.api.set.ImmutableSet
import org.eclipse.collections.impl.list.fixed.ArrayAdapter

/**
 * The strategy for when onboarding is disabled (i.e. the regular prod deployment mode). The main check here is to
 * ensure that teams did not leave leftover folders from onboarding and reverse-engineering here.
 */
internal class DisabledOnboardingStrategy : OnboardingStrategy {
    override fun handleSuccess(change: Change) {
        // no need to do anything extra upon actual deployment time
    }

    override fun handleException(change: Change, exc: Exception) {
        // no need to do anything extra upon actual deployment time
    }

    override fun validateSourceDirs(sourceDirs: Iterable<FileObject>, schemaNames: ImmutableSet<String>) {
        for (sourceDir in sourceDirs) {
            // Only check for the schema folders under the source dirs to minimize any noise in this check.
            // This logic matches DbDirectoryChangesetReader - ideally we should try to share this code logic
            val schemaDirs = sourceDir.findFiles(BasicFileSelector({ fileInfo ->
                schemaNames.any { schemaName ->
                    fileInfo.file.name.baseName.equals(schemaName, ignoreCase = true)
                }
            }))

            val onboardFiles = schemaDirs.flatMap { schemaDir ->
                schemaDir.findFiles(BasicFileSelector({ fileInfo ->
                    fileInfo.file.name.baseName.equals(OnboardingStrategy.exceptionDir, ignoreCase = true)
                            || fileInfo.file.name.baseName.endsWith(DaConstants.ANALYZE_FOLDER_SUFFIX)
                }, true)).toList()
            }

            if (onboardFiles.isNotEmpty()) {
                throw IllegalArgumentException("Directory $sourceDir has the exception folders in it that need to get removed before doing regular deployments: $onboardFiles")
            }
        }
    }
}
