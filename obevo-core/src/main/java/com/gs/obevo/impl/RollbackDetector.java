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

import com.gs.obevo.api.platform.DeployExecutionDao;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Logic to indicate if a rollback should be executed based on the version requested and the deploy audit history.
 */
interface RollbackDetector {
    boolean determineRollback(String productVersion, ImmutableSet<String> schemas, DeployExecutionDao deployExecutionDao);
}
