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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.util.vfs.FileObject;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Strategy class to facilitate onboarding (i.e. moving files for exception conditions) while also being able to
 * validate against such folders existing for regular deployments.
 */
public interface OnboardingStrategy {
    String EXCEPTION_DIR = "exceptions";
    String EXCEPTION_EXTENSION = "exception";

    void handleSuccess(Change change);

    void handleException(Change change, Exception exc);

    void validateSourceDirs(RichIterable<FileObject> sourceDirs, ImmutableSet<String> schemaNames);
}
