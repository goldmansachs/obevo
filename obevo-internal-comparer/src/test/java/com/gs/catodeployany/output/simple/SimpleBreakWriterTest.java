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
package com.gs.catodeployany.output.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.compare.breaks.Break;
import com.gs.catodeployany.compare.breaks.DataObjectBreak;
import com.gs.catodeployany.compare.breaks.FieldBreak;
import com.gs.catodeployany.compare.breaks.GroupBreak;
import com.gs.catodeployany.compare.simple.SimpleCatoProperties;
import com.gs.catodeployany.data.CatoDataObject;
import com.gs.catodeployany.output.CatoContentRow;
import com.gs.catodeployany.output.CatoContentRow.ValueType;
import com.gs.catodeployany.output.MockContentWriter;
import com.gs.catodeployany.util.MockDataSource;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import static com.gs.catodeployany.compare.CatoDataSide.LEFT;
import static com.gs.catodeployany.compare.CatoDataSide.RIGHT;
import static java.util.Arrays.asList;

public class SimpleBreakWriterTest {

    private final List<String> keyFields = asList("a", "b");
    private final List<String> excludeFields = asList("x", "y", "z");

    @Test
    public void dataObjectBreakTest() throws Exception {

        List<Break> breaks = new ArrayList<Break>();
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 2, "c", 3, "d", 4, "f", 10, "x", 5, "y", 6), LEFT));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 3, "c", 8, "d", 9, "x", 10, "y", 11), LEFT));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 4, "c", 10, "d", 11, "x", 15, "y", 16), RIGHT));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 5, "c", 11, "d", 12, "e", 15, "x", 15, "y", 16), RIGHT));

        this.assertSheetValues(this.getContentRows(breaks), new Object[][]{
                {"Break Type", "a", "b", "c", "d", "f", "e", "x", "y"},
                {"Only in C", 1, 2, 3, 4, 10, null, 5, 6},
                {"Only in C", 1, 3, 8, 9, null, null, 10, 11},
                {"Only in R", 1, 4, 10, 11, null, null, 15, 16},
                {"Only in R", 1, 5, 11, 12, null, 15, 15, 16}});

        this.assertSheetColors(this.getContentRows(breaks), new ValueType[][]{
                {N, K, K, N, N, N, N, E, E},
                {A, A, A, A, A, A, A, E, E},
                {A, A, A, A, A, A, A, E, E},
                {M, M, M, M, M, M, M, E, E},
                {M, M, M, M, M, M, M, E, E}});
    }

    @Test
    public void groupBreakTest() throws Exception {

        List<Break> breaks = new ArrayList<Break>();
        List<String> fields = asList("c");
        breaks.add(new GroupBreak(this.create("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "x", 5, "y", 6), LEFT, fields, 1));
        breaks.add(new GroupBreak(this.create("a", 1, "b", 2, "c", 8, "d", 9, "e", 21, "x", 10, "y", 11), RIGHT, fields, 1));
        breaks.add(new GroupBreak(this.create("a", 1, "b", 2, "c", 10, "d", 11, "e", 22, "x", 15, "y", 16), RIGHT, fields, 1));

        breaks.add(new GroupBreak(this.create("a", 1, "b", 3, "c", 3, "d", 4, "e", 5, "x", 5, "y", 6), LEFT, asList("e"), 2));

        this.assertSheetValues(this.getContentRows(breaks), new Object[][]{
                {"Break Type", "Group", "a", "b", "c", "d", "e", "x", "y"},
                {"Only in C group", 1, 1, 2, 3, 4, 5, 5, 6},
                {"Only in R group", 1, 1, 2, 8, 9, 21, 10, 11},
                {"Only in R group", 1, 1, 2, 10, 11, 22, 15, 16},
                {"Only in C group", 2, 1, 3, 3, 4, 5, 5, 6}});

        this.assertSheetColors(this.getContentRows(breaks), new ValueType[][]{
                {N, N, K, K, F, N, F, E, E},
                {A, A, A, A, F, A, A, E, E},
                {M, M, M, M, F, M, M, E, E},
                {M, M, M, M, F, M, M, E, E},
                {A, A, A, A, A, A, F, E, E}});
    }

    @Test
    public void fieldBreakTest() throws Exception {
        FieldBreak br;
        List<Break> breaks = new ArrayList<Break>();

        breaks.add(new FieldBreak(this.create("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "x", 5, "y", 6),
                createBreakMap("c", 5)));
        breaks.add(new FieldBreak(this.create("a", 1, "b", 3, "c", 8, "d", 9, "e", 21, "f", 6, "x", 10, "y", 11),
                createBreakMap("c", 6, "e", 25)));
        br = new FieldBreak(this.create("a", 1, "b", 4, "c", 10, "d", 11, "e", 22, "f", 6, "x", 15, "y", 16),
                createBreakMap("c", 11, "e", 26, "f", 10));
        br.setExcluded("e", true);
        br.setExcluded("f", true);
        breaks.add(br);

        this.assertSheetValues(this.getContentRows(breaks), new Object[][]{
                {"Break Type", "a", "b", "c", "c (R)", "d", "e", "e (R)", "f", "f (R)", "x", "y"},
                {"Different values", 1, 2, 3, 5, 4, 5, null, 6, null, 5, 6},
                {"Different values", 1, 3, 8, 6, 9, 21, 25, 6, null, 10, 11},
                {"Different values", 1, 4, 10, 11, 11, 22, 26, 6, 10, 15, 16}});

        this.assertSheetColors(this.getContentRows(breaks), new ValueType[][]{
                {N, K, K, F, R, N, F, R, E, R, E, E},
                {N, N, N, F, R, N, N, N, N, N, E, E},
                {N, N, N, F, R, N, F, R, N, N, E, E},
                {N, N, N, F, R, N, E, R, E, R, E, E}});
    }

    @Test
    public void allBreakTypesTest() throws Exception {
        List<Break> breaks = new ArrayList<Break>();

        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 10, "c", 1, "d", 2, "e", 5, "f", 6, "x", 3, "y", 4),
                LEFT));

        breaks.add(new FieldBreak(this.create("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "x", 5, "y", 6),
                createBreakMap("c", 5)));
        breaks.add(new FieldBreak(this.create("a", 1, "b", 3, "c", 8, "d", 9, "e", 21, "f", 6, "x", 10, "y", 11),
                createBreakMap("c", 6, "e", 25)));

        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 11, "c", 1, "d", 2, "e", 5, "x", 3, "y", 4),
                RIGHT));

        breaks.add(new GroupBreak(this.create("a", 1, "b", 6, "c", 3, "d", 4, "e", 5, "f", 6, "x", 5, "y", 6),
                LEFT, asList("c", "f"), 1));
        breaks.add(new GroupBreak(this.create("a", 1, "b", 6, "c", 8, "d", 9, "e", 21, "f", 7, "x", 10, "y", 11),
                RIGHT, asList("c", "f"), 1));

        this.assertSheetValues(this.getContentRows(breaks), new Object[][]{
                {"Break Type", "Group", "a", "b", "c", "c (R)", "d", "e", "e (R)", "f", "x", "y"},
                {"Only in C", null, 1, 10, 1, null, 2, 5, null, 6, 3, 4},
                {"Different values", null, 1, 2, 3, 5, 4, 5, null, 6, 5, 6},
                {"Different values", null, 1, 3, 8, 6, 9, 21, 25, 6, 10, 11},
                {"Only in R", null, 1, 11, 1, null, 2, 5, null, null, 3, 4},
                {"Only in C group", 1, 1, 6, 3, null, 4, 5, null, 6, 5, 6},
                {"Only in R group", 1, 1, 6, 8, null, 9, 21, null, 7, 10, 11}});

        this.assertSheetColors(this.getContentRows(breaks), new ValueType[][]{
                {N, N, K, K, F, R, N, F, R, F, E, E},
                {A, A, A, A, A, A, A, A, A, A, E, E},
                {N, N, N, N, F, R, N, N, N, N, E, E},
                {N, N, N, N, F, R, N, F, R, N, E, E},
                {M, M, M, M, M, M, M, M, M, M, E, E},
                {A, A, A, A, F, A, A, A, A, F, E, E},
                {M, M, M, M, F, M, M, M, M, F, E, E}});
    }

    @Test
    public void simpleMappedFieldsTest() throws Exception {
        List<Break> breaks = new ArrayList<Break>();

        breaks.add(new FieldBreak(this.create("a", 1, "b", 2, "c", 3, "e", 5, "x", 5), createBreakMap("c", 15)));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 3, "c", 8, "e", 21, "x", 10), LEFT));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 4, "d", 9, "e", 21, "x", 10), RIGHT));

        CatoComparison comparison = this.getComparison(breaks);
        comparison.getProperties().getMappedFields().put("c", "d");

        this.assertSheetValues(this.getContentRows(breaks, comparison), new Object[][]{
                {"Break Type", "a", "b", "c", "d", "e", "x"},
                {"Different values", 1, 2, 3, 15, 5, 5},
                {"Only in C", 1, 3, 8, null, 21, 10},
                {"Only in R", 1, 4, null, 9, 21, 10}});

        this.assertSheetColors(this.getContentRows(breaks, comparison), new ValueType[][]{
                {N, K, K, F, R, N, E},
                {N, N, N, F, R, N, E},
                {A, A, A, A, A, A, E},
                {M, M, M, M, M, M, E}});
    }

    @Test
    public void complexMappedFieldsTest() throws Exception {
        List<Break> breaks = new ArrayList<Break>();

        breaks.add(new FieldBreak(this.create("a", 1, "b", 2, "c", 3, "d", 4, "e", 15, "x", 5),
                createBreakMap("c", 15, "d", 15)));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 3, "c", 8, "d", 9, "e", 21, "x", 10), LEFT));
        breaks.add(new DataObjectBreak(this.create("a", 1, "b", 4, "d", 9, "e", 21, "x", 10), RIGHT));

        CatoComparison comparison = this.getComparison(breaks);
        comparison.getProperties().getMappedFields().put("c", "d");
        comparison.getProperties().getMappedFields().put("e", "d");

        this.assertSheetValues(this.getContentRows(breaks, comparison), new Object[][]{
                {"Break Type", "a", "b", "c", "d (R)", "d", "d (R)", "e", "x"},
                {"Different values", 1, 2, 3, 15, 4, 15, 15, 5},
                {"Only in C", 1, 3, 8, null, 9, null, 21, 10},
                {"Only in R", 1, 4, null, null, 9, null, 21, 10}});

        this.assertSheetColors(this.getContentRows(breaks, comparison), new ValueType[][]{
                {N, K, K, F, R, F, R, N, E},
                {N, N, N, F, R, F, R, N, E},
                {A, A, A, A, A, A, A, A, E},
                {M, M, M, M, M, M, M, M, E}});
    }

    @Test
    public void reorderDataObjectBreakTest() throws Exception {

        List<Break> breaks = new ArrayList<Break>();
        breaks.add(new DataObjectBreak(this.create("x", 5, "c", 3, "a", 1, "y", 6, "b", 2, "d", 4, "f", 10), LEFT));
        breaks.add(new DataObjectBreak(this.create("x", 10, "c", 8, "a", 1, "y", 11, "b", 3, "d", 9), LEFT));
        breaks.add(new DataObjectBreak(this.create("x", 15, "c", 10, "a", 1, "y", 16, "b", 4, "d", 11), RIGHT));
        breaks.add(new DataObjectBreak(this.create("x", 15, "c", 11, "a", 1, "y", 16, "b", 5, "d", 12, "e", 15, "f", 10), RIGHT));

        this.assertSheetValues(this.getContentRows(breaks), new Object[][]{
                {"Break Type", "a", "b", "c", "d", "f", "e", "x", "y"},
                {"Only in C", 1, 2, 3, 4, 10, null, 5, 6},
                {"Only in C", 1, 3, 8, 9, null, null, 10, 11},
                {"Only in R", 1, 4, 10, 11, null, null, 15, 16},
                {"Only in R", 1, 5, 11, 12, 10, 15, 15, 16}});

        this.assertSheetColors(this.getContentRows(breaks), new ValueType[][]{
                {N, K, K, N, N, N, N, E, E},
                {A, A, A, A, A, A, A, E, E},
                {A, A, A, A, A, A, A, E, E},
                {M, M, M, M, M, M, M, E, E},
                {M, M, M, M, M, M, M, E, E}});
    }

    private void assertSheetValues(List<CatoContentRow> rows, Object[][] values) {
        Assert.assertEquals("Incorrect number of rows", values.length, rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Assert.assertEquals("Incorrect length for row " + i, values[i].length, rows.get(i).getSize());
            for (int j = 0; j < rows.get(i).getSize(); j++) {
                Assert.assertEquals("Incorrect value for col " + j + " on row " + i, values[i][j], rows.get(i).getValue(j));
            }
        }
    }

    private void assertSheetColors(List<CatoContentRow> rows, ValueType[][] types) {
        Assert.assertEquals(types.length, rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Assert.assertEquals(types[i].length, rows.get(i).getSize());
            for (int j = 0; j < rows.get(i).getSize(); j++) {
                Assert.assertEquals(types[i][j], rows.get(i).getValueType(j));
            }
        }
    }

    private CatoDataObject create(Object... fields) {
        return TestUtil.createDataObjectWithKeys(fields);
    }

    public static Map<String, Object> createBreakMap(Object... data) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < data.length; i += 2) {
            map.put(data[i].toString(), data[i + 1]);
        }

        return map;
    }

    private List<CatoContentRow> getContentRows(List<Break> breaks) throws IOException {
        return this.getContentRows(breaks, this.getComparison(breaks));
    }

    private List<CatoContentRow> getContentRows(List<Break> breaks, CatoComparison comparison) throws IOException {
        MockContentWriter contentWriter = new MockContentWriter();
        new SimpleBreakFormatter(false).writeData(new SimpleComparisonMetadata(comparison), contentWriter);
        return contentWriter.getRows();
    }

    private CatoComparison getComparison(List<Break> breaks) {
        return new CatoComparison("Test", new SimpleCatoProperties(this.keyFields, this.excludeFields),
                breaks,
                new MockDataSource("C"), Collections.<CatoDataObject>emptyList(),
                new MockDataSource("R"), Collections.<CatoDataObject>emptyList());
    }

    private static final ValueType N = null;
    private static final ValueType K = ValueType.KEY;
    private static final ValueType E = ValueType.EXCLUDE;
    private static final ValueType F = ValueType.FIELD_BREAK;
    private static final ValueType M = ValueType.RIGHT_ONLY;
    private static final ValueType A = ValueType.LEFT_ONLY;
    private static final ValueType R = ValueType.RIGHT_VALUE;
}
