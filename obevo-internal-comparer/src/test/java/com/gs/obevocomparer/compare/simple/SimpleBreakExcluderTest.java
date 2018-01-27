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

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.gs.obevocomparer.compare.breaks.Break;
import com.gs.obevocomparer.compare.breaks.BreakExclude;
import com.gs.obevocomparer.compare.breaks.FieldBreak;
import com.gs.obevocomparer.input.text.DelimitedStreamDataSource;
import com.gs.obevocomparer.util.CatoBaseUtil;
import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleBreakExcluderTest {

    private final SimpleBreakExcluder breakExcluder = new SimpleBreakExcluder(new SimpleDataComparator());
    private Iterable<Break> breaks;

    private final List<BreakExclude> breakExcludes = new ArrayList<BreakExclude>();

    @Before
    public void setUp() throws Exception {
        DelimitedStreamDataSource dataSource1 = new DelimitedStreamDataSource("test1", new FileReader("src/test/resources/testdata1.txt"), TestUtil.ALL_FIELDS, ",");
        DelimitedStreamDataSource dataSource2 = new DelimitedStreamDataSource("test2", new FileReader("src/test/resources/testdata2.txt"), TestUtil.ALL_FIELDS, ",");
        dataSource1.setTypeConverter(null);
        dataSource2.setTypeConverter(null);

        try {
            this.breaks = CatoBaseUtil.compare("comp", dataSource1, dataSource2, TestUtil.KEY_FIELDS, TestUtil.EXCLUDE_FIELDS).getBreaks();
        } catch (Exception ex) {
            Assert.fail();
        }
    }

    @Test
    public void invalidBreakExcludeTest() {
        try {
            new BreakExclude(null, null, null, null);
            Assert.fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void beforeExcludeTest() {
        Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(0, this.getExcludeFieldCount());
    }

    @Test
    public void excludeRowTest() {
        this.breakExcludes.add(TestUtil.createBreakExclude("B", "A", null, null, null));
        this.breakExcludes.add(TestUtil.createBreakExclude("C", "C", null, null, null));

        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(2, this.getExcludeCount());
    }

    @Test
    public void excludeRowGroupTest() {
        this.breakExcludes.add(TestUtil.createBreakExclude("D", "A", null, null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(3, this.getExcludeCount());
    }

    @Test
    public void excludeRowFieldTest() {
        this.breakExcludes.add(TestUtil.createBreakExclude("B", "B", TestUtil.ALL_FIELDS.get(0), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());

        this.breakExcludes.add(TestUtil.createBreakExclude("B", "B", TestUtil.ALL_FIELDS.get(3), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());

        this.breakExcludes.add(TestUtil.createBreakExclude("B", "B", TestUtil.ALL_FIELDS.get(4), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(1, this.getExcludeCount());
        org.junit.Assert.assertEquals(2, this.getExcludeFieldCount());
    }

    @Test
    public void excludeRowFieldTest2() {
        this.breakExcludes.add(TestUtil.createBreakExclude("D", "A", TestUtil.ALL_FIELDS.get(0), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());

        this.breakExcludes.add(TestUtil.createBreakExclude("B", "B", TestUtil.ALL_FIELDS.get(2), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());

        this.breakExcludes.add(TestUtil.createBreakExclude("B", "B", TestUtil.ALL_FIELDS.get(3), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
    }

    @Test
    public void excludeFieldTest() {
        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(3), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());
    }

    @Test
    public void excludeFieldTest2() {
        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(4), null, null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(1, this.getExcludeCount());
        org.junit.Assert.assertEquals(2, this.getExcludeFieldCount());
    }

    @Test
    public void excludeFieldValueTest() {
        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(4), "6", null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());
    }

    @Test
    public void excludeFieldValueTest2() {
        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(4), "6", "8"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(0, this.getExcludeFieldCount());

        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(4), "6", "7"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());
    }

    @Test
    public void excludeFieldValueTest3() {
        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(4), null, "6"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(0, this.getExcludeFieldCount());

        this.breakExcludes.add(TestUtil.createBreakExclude(TestUtil.ALL_FIELDS.get(4), null, "7"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());
    }

    @Test
    public void excludeValueTest() {
        this.breakExcludes.add(TestUtil.createBreakExclude(null, "7", "8"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(1, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());
    }

    @Test
    public void excludeValueTest2() {
        this.breakExcludes.add(TestUtil.createBreakExclude(null, "3", "4"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());

        this.breakExcludes.add(TestUtil.createBreakExclude(null, "3", null));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(1, this.getExcludeCount());
        org.junit.Assert.assertEquals(2, this.getExcludeFieldCount());
    }

    @Test
    public void excludeValueTest3() {
        this.breakExcludes.add(TestUtil.createBreakExclude(null, null, "4"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(0, this.getExcludeCount());
        org.junit.Assert.assertEquals(1, this.getExcludeFieldCount());

        this.breakExcludes.add(TestUtil.createBreakExclude(null, null, "7"));
        this.breakExcluder.excludeBreaks(this.breaks, this.breakExcludes);
        org.junit.Assert.assertEquals(1, this.getExcludeCount());
        org.junit.Assert.assertEquals(2, this.getExcludeFieldCount());
    }

    private int getExcludeFieldCount() {
        int breakCount = 0;
        FieldBreak fieldBreak;

        for (Break br : this.breaks) {
            if (br instanceof FieldBreak) {
                fieldBreak = (FieldBreak) br;
                for (String field : fieldBreak.getFields()) {
                    if (fieldBreak.isExcluded(field)) {
                        breakCount++;
                    }
                }
            }
        }

        return breakCount;
    }

    private int getExcludeCount() {
        int breakCount = 0;

        for (Break br : this.breaks) {
            if (br.isExcluded()) {
                breakCount++;
            }
        }

        return breakCount;
    }
}
