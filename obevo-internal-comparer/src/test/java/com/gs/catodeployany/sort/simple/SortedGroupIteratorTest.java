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
package com.gs.catodeployany.sort.simple;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gs.catodeployany.sort.SortedGroupIterator;
import com.gs.catodeployany.util.TestUtil;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;

public class SortedGroupIteratorTest {

    private final Comparator<Integer> comparator = new Comparator<Integer>() {
        public int compare(Integer int1, Integer int2) {
            return int1.compareTo(int2);
        }
    };

    @Test
    public void testEmpty() {

        SortedGroupIterator<Integer> iter = new SortedGroupIterator<Integer>(Collections.<Integer>emptyList().iterator(), this.comparator);

        Assert.assertFalse(iter.hasNext());
        Assert.assertEquals(0, iter.next().size());
    }

    @Test
    public void testOneElement() {

        SortedGroupIterator<Integer> iter = new SortedGroupIterator<Integer>(Arrays.asList(1).iterator(), this.comparator);

        Assert.assertTrue(iter.hasNext());
        List<Integer> group = iter.next();
        Assert.assertEquals(1, group.size());
        Assert.assertEquals(1, group.get(0).intValue());

        Assert.assertFalse(iter.hasNext());
        Assert.assertEquals(0, iter.next().size());
    }

    @Test
    public void testSimpleGroup() {
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
        SortedGroupIterator<Integer> iter = new SortedGroupIterator<Integer>(values.iterator(), this.comparator);

        for (int value : values) {
            this.assertGroup(value, 1, iter.next());
        }

        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void testComplexGroup() {

        SortedGroupIterator<Integer> iter = new SortedGroupIterator<Integer>(
                Arrays.asList(1, 1, 1, 2, 2, 3, 4, 5, 6, 6, 7, 8, 8).iterator(), this.comparator);

        this.assertGroup(1, 3, iter.next());
        this.assertGroup(2, 2, iter.next());
        this.assertGroup(3, 1, iter.next());
        this.assertGroup(4, 1, iter.next());
        this.assertGroup(5, 1, iter.next());
        this.assertGroup(6, 2, iter.next());
        this.assertGroup(7, 1, iter.next());
        this.assertGroup(8, 2, iter.next());

        Assert.assertFalse(iter.hasNext());
    }

    @Test
    public void testOutOfOrderGroup() {

        TestUtil.clearLogged();
        SortedGroupIterator<Integer> iter = new SortedGroupIterator<Integer>(
                Arrays.asList(1, 1, 1, 3, 2, 2, 4, 5).iterator(), this.comparator);

        this.assertGroup(1, 3, iter.next());
        this.assertGroup(3, 1, iter.next());
        this.assertGroup(2, 2, iter.next());
        this.assertGroup(4, 1, iter.next());
        this.assertGroup(5, 1, iter.next());

        Assert.assertFalse(iter.hasNext());
        TestUtil.assertLogged(Level.WARN, "Objects not in sorted order - 3 precedes 2 but is greater");
    }

    @Test
    public void testRemove() {
        SortedGroupIterator<Integer> iter = new SortedGroupIterator<Integer>(
                Arrays.asList(1).iterator(), this.comparator);
        try {
            iter.next();
            iter.remove();
            Assert.fail();
        } catch (Exception ex) {
        }
    }

    private void assertGroup(int value, int size, List<Integer> group) {
        org.junit.Assert.assertEquals(size, group.size());
        for (int val : group) {
            org.junit.Assert.assertEquals(value, val);
        }
    }
}
