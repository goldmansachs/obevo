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
package com.gs.catodeployany.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortedGroupIterator<T> implements Iterator<List<T>> {

    private final Iterator<T> iterator;
    private final Comparator<T> comparator;
    private T next;

    private static final Logger LOG = LoggerFactory.getLogger(SortedGroupIterator.class);

    public SortedGroupIterator(Iterator<T> iterator, Comparator<T> comparator) {
        this.iterator = iterator;
        this.comparator = comparator;

        if (iterator.hasNext()) {
            this.next = this.iterator.next();
        }
    }

    public List<T> next() {
        if (!this.hasNext()) {
            return Collections.emptyList();
        }

        List<T> group = new ArrayList<T>();
        int compare = 0;
        T curr = null;

        while (compare == 0) {
            group.add(this.next);

            curr = this.next;
            this.next = this.iterator.hasNext() ? this.iterator.next() : null;
            compare = this.next == null ? -1 : this.comparator.compare(curr, this.next);
        }

        if (compare > 0) {
            LOG.warn("Objects not in sorted order - {} precedes {} but is greater", curr, this.next);
        }

        return group;
    }

    public boolean hasNext() {
        return this.next != null;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}