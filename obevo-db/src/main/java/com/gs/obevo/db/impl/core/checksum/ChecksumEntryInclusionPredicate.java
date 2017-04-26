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
package com.gs.obevo.db.impl.core.checksum;

import com.gs.obevo.util.lookuppredicate.Index;
import com.gs.obevo.util.lookuppredicate.ObjectPredicate;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.impl.block.factory.Functions0;

/**
 * Convenience predicate to allow filtering of the ChecksumEntries based on the object type and name
 */
public class ChecksumEntryInclusionPredicate implements Predicate<ChecksumEntry> {
    private final ImmutableCollection<? extends Index> objectTypeIndexes;
    private final ImmutableCollection<? extends Index> objectNameIndexes;

    public ChecksumEntryInclusionPredicate(ImmutableCollection<? extends Index> objectTypeIndexes, ImmutableCollection<? extends Index> objectNameIndexes) {
        this.objectTypeIndexes = objectTypeIndexes;
        this.objectNameIndexes = objectNameIndexes;
    }

    @Override
    public boolean accept(ChecksumEntry each) {
        if (objectTypeIndexes != null && objectTypeIndexes.noneSatisfy(ObjectPredicate.create(Functions0.value(each.getObjectType())))) {
            return false;
        }
        if (objectNameIndexes != null && objectNameIndexes.noneSatisfy(ObjectPredicate.create(Functions0.value(each.getName1())))) {
            return false;
        }

        return true;
    }
}
