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

import com.gs.obevo.api.appdata.Change;
import com.gs.obevo.api.appdata.ChangeKey;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Used by the default {@link com.gs.obevo.impl.ChangesetCreator} implementation to determine how the changes need to be treated.
 * The ChangesetCreator does a basic difference on {@link ChangeKey} to find what changed, but this
 * interface is used to drill and determine how those changes should be trated.
 */
public interface ChangeTypeCommandCalculator {
    ImmutableList<ChangeCommand> calculateCommands(ChangeType changeType, RichIterable<ChangePair> changePairs, RichIterable<Change> sources, boolean initAllowedOnHashExceptions);
}
