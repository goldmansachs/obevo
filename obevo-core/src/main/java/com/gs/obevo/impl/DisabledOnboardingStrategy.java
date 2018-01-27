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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.DaConstants;
import com.gs.obevo.util.vfs.BasicFileSelector;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.vfs2.FileFilter;
import org.apache.commons.vfs2.FileSelectInfo;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;

/**
 * The strategy for when onboarding is disabled (i.e. the regular prod deployment mode). The main check here is to
 * ensure that teams did not leave leftover folders from onboarding and reverse-engineering here.
 */
class DisabledOnboardingStrategy implements OnboardingStrategy {
    @Override
    public void handleSuccess(Change change) {
        // no need to do anything extra upon actual deployment time
    }

    @Override
    public void handleException(Change change, Exception exc, MutableSet<String> failedDbObjectNames) {
        // no need to do anything extra upon actual deployment time
    }

    @Override
    public void validateSourceDirs(RichIterable<FileObject> sourceDirs, final ImmutableSet<String> schemaNames) {
        for (FileObject sourceDir : sourceDirs) {
            // Only check for the schema folders under the source dirs to minimize any noise in this check.
            // This logic matches DbDirectoryChangesetReader - ideally we should try to share this code logic
            MutableList<FileObject> schemaDirs = ArrayAdapter.adapt(sourceDir.findFiles(new BasicFileSelector(new FileFilter() {
                @Override
                public boolean accept(final FileSelectInfo fileInfo) {
                    return schemaNames.anySatisfy(new Predicate<String>() {
                        @Override
                        public boolean accept(String schemaName) {
                            return fileInfo.getFile().getName().getBaseName().equalsIgnoreCase(schemaName);
                        }
                    });
                }
            })));

            MutableList<FileObject> onboardFiles = schemaDirs.flatCollect(new Function<FileObject, List<FileObject>>() {
                @Override
                public List<FileObject> valueOf(FileObject schemaDir) {
                    return ArrayAdapter.adapt(schemaDir.findFiles(new BasicFileSelector(new FileFilter() {
                        @Override
                        public boolean accept(FileSelectInfo fileInfo) {
                            return fileInfo.getFile().getName().getBaseName().equalsIgnoreCase(EXCEPTION_DIR)
                                    || fileInfo.getFile().getName().getBaseName().equalsIgnoreCase(DEPENDENT_EXCEPTION_DIR)
                                    || fileInfo.getFile().getName().getBaseName().endsWith(DaConstants.ANALYZE_FOLDER_SUFFIX)
                                    ;
                        }
                    }, true)));
                }
            });

            if (onboardFiles.notEmpty()) {
                throw new IllegalArgumentException("Directory " + sourceDir + " has the exception folders in it that need to get removed before doing regular deployments: " + onboardFiles);
            }
        }
    }
}
