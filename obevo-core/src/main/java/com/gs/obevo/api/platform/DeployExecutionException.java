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
package com.gs.obevo.api.platform;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Exception to be used to indicate the failure of a deploy execution. This will include the list of failed changes to
 * facilitate programmatic access, particularly for testing verification.
 */
public class DeployExecutionException extends DeployerRuntimeException {
    private final ImmutableList<FailedChange> failedChanges;

    public DeployExecutionException(String exceptionMessage, RichIterable<FailedChange> failedChanges) {
        super(exceptionMessage);
        this.failedChanges = failedChanges.toList().toImmutable();
    }

    public ImmutableList<FailedChange> getFailedChanges() {
        return failedChanges;
    }
}
