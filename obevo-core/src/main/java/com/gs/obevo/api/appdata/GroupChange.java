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
package com.gs.obevo.api.appdata;

import org.apache.commons.lang3.Validate;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * A composite {@link Change} for cases when we want to deploy a group of objects/changes in one shot.
 */
public class GroupChange extends Change {
    private final ImmutableList<Change> changes;

    public GroupChange(ImmutableList<Change> changes) {
        Validate.isTrue(changes.notEmpty(), "Cannot provide an empty list to a GroupChange");
        this.changes = changes;
        this.setChangeType(changes.get(0).getChangeType());
    }

    public ImmutableList<Change> getChanges() {
        return changes;
    }

    @Override
    public String getDisplayString() {
        if (this.changes.size() == 1) {
            return this.changes.get(0).getDisplayString();
        } else {
            return "Group of static data changes (executing the inserts/updates [if any] in order, " +
                    "and deletes [if any] in reverse-order):\n\t\t" +
                    this.changes.collect(Change.TO_DISPLAY_STRING).makeString("\n\t\t");
        }
    }

    @Override
    public int getOrderWithinObject() {
        return 0;
    }


}
