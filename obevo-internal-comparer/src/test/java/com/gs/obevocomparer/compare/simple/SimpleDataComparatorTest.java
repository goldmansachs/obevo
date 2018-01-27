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
package com.gs.obevocomparer.compare.simple;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

import com.gs.obevocomparer.util.TestUtil;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;

public class SimpleDataComparatorTest {

    private static SimpleDataComparator comparator;

    @BeforeClass
    public static void setUp() {
        comparator = new SimpleDataComparator(4);
    }

    @Test
    public void testCompareNulls() {
        Assert.assertTrue(comparator.compareValues(null, null));
        Assert.assertEquals(0, comparator.compareKeyValues(null, null));

        Assert.assertFalse(comparator.compareValues(null, "a"));
        Assert.assertEquals(1, comparator.compareKeyValues(null, "a"));

        Assert.assertFalse(comparator.compareValues("a", null));
        Assert.assertEquals(-1, comparator.compareKeyValues("a", null));
    }

    @Test
    public void testCompareKeyIntegers() {
        Assert.assertEquals(0, comparator.compareKeyValues(3, 3));

        Assert.assertEquals(-1, comparator.compareKeyValues(3, 5));
        Assert.assertEquals(1, comparator.compareKeyValues(4, 2));
    }

    @Test
    public void testCompareIntegers() {
        Assert.assertTrue(comparator.compareValues(3, 3));

        Assert.assertFalse(comparator.compareValues(3, 5));
        Assert.assertFalse(comparator.compareValues(4, 2));
    }

    @Test
    public void testCompareKeyDoubles() {
        Assert.assertEquals(0, comparator.compareKeyValues(1.43567d, 1.43567d));

        Assert.assertEquals(-1, comparator.compareKeyValues(1.43552d, 2.45673d));
        Assert.assertEquals(1, comparator.compareKeyValues(1.43567d, 1.43552d));

        // test precision cut off
        Assert.assertEquals(0, comparator.compareKeyValues(1.43562d, 1.43564d));
        Assert.assertEquals(-1, comparator.compareKeyValues(1.43564d, 1.43566d));

        // test different precision
        SimpleDataComparator comparator2 = new SimpleDataComparator(0);
        Assert.assertEquals(0, comparator2.compareKeyValues(1.4, 1.2));
        Assert.assertEquals(-1, comparator2.compareKeyValues(1.4, 1.6));
        Assert.assertEquals(0, comparator2.compareKeyValues(1.5, 2.0));
        Assert.assertEquals(-1, comparator2.compareKeyValues(1.5, 2.51));
    }

    @Test
    public void testCompareDoubles() {
        Assert.assertTrue(comparator.compareValues(1.43567d, 1.43567d));

        Assert.assertFalse(comparator.compareValues(1.43552d, 2.45673d));
        Assert.assertFalse(comparator.compareValues(1.43567d, 1.43552d));

        // test precision cut off, second test is different to key compare
        Assert.assertTrue(comparator.compareValues(1.43562d, 1.43564d));
        Assert.assertTrue(comparator.compareValues(1.43562d, 1.43567d));

        // test different precision 0
        SimpleDataComparator comparator2 = new SimpleDataComparator(0);
        Assert.assertTrue(comparator2.compareValues(1.4d, 1.2d));
        Assert.assertTrue(comparator2.compareValues(1.4d, 1.6d));
        Assert.assertTrue(comparator2.compareValues(1.5d, 2.0d));
        Assert.assertFalse(comparator2.compareValues(1.5d, 2.51d));

        // test different precision -2
        SimpleDataComparator comparator3 = new SimpleDataComparator(-2);
        Assert.assertTrue(comparator3.compareValues(140, 120));
        Assert.assertTrue(comparator3.compareValues(140, 160));
        Assert.assertTrue(comparator3.compareValues(150, 200));
        Assert.assertFalse(comparator3.compareValues(150, 251));
    }

    @Test
    public void testCompareKeyFloats() {
        Assert.assertEquals(0, comparator.compareKeyValues(1.43567f, 1.43567f));

        Assert.assertEquals(-1, comparator.compareKeyValues(1.43552f, 2.45673f));
        Assert.assertEquals(1, comparator.compareKeyValues(1.43567f, 1.43552f));

        // test precision cut off
        Assert.assertEquals(0, comparator.compareKeyValues(1.43562f, 1.43564f));
        Assert.assertEquals(-1, comparator.compareKeyValues(1.43562f, 1.43567f));
    }

    @Test
    public void testCompareNumberTypes() {
        Assert.assertEquals(0, comparator.compareKeyValues(1.5d, 1.5f));
        Assert.assertEquals(1, comparator.compareKeyValues(3.1d, 3));
        Assert.assertEquals(0, comparator.compareKeyValues((short) 3, 3));
        Assert.assertEquals(1, comparator.compareKeyValues(new BigDecimal(5.0001), 5.0d));
        Assert.assertEquals(0, comparator.compareKeyValues(new BigDecimal(5), 5));
    }

    @Test
    public void testStringCompare() {
        Assert.assertTrue(comparator.compareValues("abcde", "abcde"));
        Assert.assertFalse(comparator.compareValues("abcde", "abcdq"));

        Assert.assertTrue(comparator.compareValues("abcde  ", "    abcde"));
        Assert.assertFalse(comparator.compareValues("abcde  ", "    abcdq"));
    }

    @Test
    public void testDateCompare() {
        Date date1 = new Date();
        Date date2 = new Date(date1.getTime());
        Date date3 = new Date(date1.getTime() + 100);

        Assert.assertTrue(comparator.compareValues(date1, date2));
        Assert.assertFalse(comparator.compareValues(date1, date3));

        Timestamp timestamp = new Timestamp(date1.getTime());

        Assert.assertEquals(date1.getTime(), timestamp.getTime());
        org.junit.Assert.assertTrue(comparator.compareValues(date1, timestamp));
        org.junit.Assert.assertTrue(comparator.compareValues(timestamp, date1));
        org.junit.Assert.assertTrue(comparator.compareValues(date2, timestamp));
        org.junit.Assert.assertFalse(comparator.compareValues(date3, timestamp));
    }

    @Test
    public void testObjectCompare() {
        Object obj1;
        Object obj2;
        obj1 = new Object() {
            public String toString() {
                return "abc";
            }
        };
        obj2 = obj1;

        org.junit.Assert.assertTrue(comparator.compareValues(obj1, obj1));

        TestUtil.clearLogged();
        obj2 = new Object() {
            public String toString() {
                return "def";
            }
        };

        org.junit.Assert.assertFalse(comparator.compareValues(obj1, obj2));
        TestUtil.assertLogged(Level.DEBUG,
                "Comparing value abc of type com.gs.obevocomparer.compare.simple.SimpleDataComparatorTest$1 " +
                        "to value def of different type com.gs.obevocomparer.compare.simple.SimpleDataComparatorTest$2");

        TestUtil.clearLogged();

        org.junit.Assert.assertTrue(comparator.compareKeyValues(obj1, obj2) < 0);
        TestUtil.assertLogged(Level.DEBUG,
                "Comparing toString() methods for value abc of type com.gs.obevocomparer.compare.simple.SimpleDataComparatorTest$1 " +
                        "to value def of different type com.gs.obevocomparer.compare.simple.SimpleDataComparatorTest$2");

        obj1 = Arrays.asList(1, 2, 3);
        obj2 = Arrays.asList(1, 2, 3);

        assertNotSame(obj1, obj2);
        org.junit.Assert.assertTrue(comparator.compareValues(obj1, obj1));
    }
}
