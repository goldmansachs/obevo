/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.impl;

import java.io.IOException;
import java.io.OutputStream;

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.platform.DeployerRuntimeException;
import com.gs.obevo.util.vfs.FileObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;

/**
 * The strategy for when onboarding is enabled. Here, we should look to move failed changes into the exception folder/s
 * if the change fails, or to keep/move to the regular folder if it succeeds.
 */
class EnabledOnboardingStrategy implements OnboardingStrategy {
    @Override
    public void handleSuccess(Change change) {
        if (change.getFileLocation().exists()) {
            final FileObject containingDir = change.getFileLocation().getParent();

            if (containingDir.getName().getBaseName().equals(EXCEPTION_DIR) || containingDir.getName().getBaseName().equals(DEPENDENT_EXCEPTION_DIR)) {
                change.getFileLocation().moveTo(change.getFileLocation().getParent().getParent().resolveFile(change.getFileLocation().getName().getBaseName()));
                FileObject exceptionFile = change.getFileLocation().getParent().resolveFile(change.getFileLocation().getName().getBaseName() + ".exception");
                exceptionFile.delete();
            }
        }
    }


    @Override
    public void handleException(Change change, Exception exc, MutableSet<String> failedDbObjectNames) {
        if (change.getFileLocation().exists()) {
            final FileObject containingDir = change.getFileLocation().getParent();

            final FileObject exceptionDir;
            if (containingDir.getName().getBaseName().equals(EXCEPTION_DIR)) {
                exceptionDir = containingDir;
            } else if (containingDir.getName().getBaseName().equals(DEPENDENT_EXCEPTION_DIR)) {
                if (isDependentException(change, failedDbObjectNames)) {
                    exceptionDir = containingDir;
                } else {
                    exceptionDir = containingDir.getParent().resolveFile(EXCEPTION_DIR);
                    exceptionDir.createFolder();

                    change.getFileLocation().moveTo(exceptionDir.resolveFile(change.getFileLocation().getName().getBaseName()));
                    final FileObject oldExceptionFile = containingDir.resolveFile(change.getFileLocation().getName().getBaseName() + ".exception");
                    oldExceptionFile.delete();
                }
            } else {
                String exceptionFolderName = isDependentException(change, failedDbObjectNames) ? DEPENDENT_EXCEPTION_DIR : EXCEPTION_DIR;

                exceptionDir = containingDir.resolveFile(exceptionFolderName);
                exceptionDir.createFolder();

                change.getFileLocation().moveTo(exceptionDir.resolveFile(change.getFileLocation().getName().getBaseName()));
            }

            FileObject exceptionFile = exceptionDir.resolveFile(change.getFileLocation().getName().getBaseName() + ".exception");
            exceptionFile.delete();
            exceptionFile.createFile();
            try {
                OutputStream outputStream = exceptionFile.getContent().getOutputStream(true);
                IOUtils.write(ExceptionUtils.getFullStackTrace(exc), outputStream);
                outputStream.close();
            } catch (FileSystemException e) {
                throw new DeployerRuntimeException(e);
            } catch (IOException e) {
                throw new DeployerRuntimeException(e);
            }
        }
    }

    private boolean isDependentException(Change change, MutableSet<String> failedDbObjectNames) {
        if (change.getConvertedContent() != null) {
            for (String failedDbObjectName : failedDbObjectNames) {
                // need to check for DbType here
                if (change.getConvertedContent().toUpperCase().contains(failedDbObjectName.toUpperCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void validateSourceDirs(RichIterable<FileObject> sourceDirs, ImmutableSet<String> schemaNames) {
        // no need to validate when onboardingStrategy is enabled
    }
}
