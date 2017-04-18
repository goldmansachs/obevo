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

import com.gs.obevo.api.platform.ChangeCommand;
import org.eclipse.collections.api.block.predicate.Predicate;

/**
 * A command that indicates a warning or exception from the given {@link this#getChanges()}.
 *
 * The {@link this#getCommandDescription()} would be used to describe the problem.
 */
public interface ChangeCommandWarning extends ChangeCommand {
    Predicate<ChangeCommandWarning> IS_FATAL = new Predicate<ChangeCommandWarning>() {
        @Override
        public boolean accept(ChangeCommandWarning each) {
            return each.isFatal();
        }
    };

    /**
     * If true, then this warning should prevent the deployment from happening.
     * If false, then it is just something to alert the user about, but deployment can proceed.
     */
    boolean isFatal();
}
