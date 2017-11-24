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
package com.gs.obevo.api.platform;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.collection.mutable.CollectionAdapter;

/**
 * Class to collect execution information around an individual ExecuteChangeCommand deployment for reporting at deploy completion, notably
 * around warnings during execution that shouldn't fail the deployment but need to be reported.
 *
 * Exceptions will not be included in this context, as we will rely on the exceptions being caught in MainDeployer and
 * handled there.
 *
 * MainDeployer will handle the exception handling spanning all changes.
 */
public class CommandExecutionContext {
    private final Queue<String> warnings = new ConcurrentLinkedQueue<>();  // should be thread-safe to accomodate parallel execution

    public CommandExecutionContext() {
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public RichIterable<String> getWarnings() {
        return CollectionAdapter.adapt(warnings);
    }
}
