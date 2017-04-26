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
package com.gs.obevo.db.impl.core.util;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MultiLineStringSplitterTest {
    private static final String sql1 = "create table mynewtable (id int)";
    private static final String sql2 = "create table mynewtable2 (id int)";
    private static final String sql3 = "create table mynewtable3 (id int)";

    private final MultiLineStringSplitter splitter = new MultiLineStringSplitter(";");
    private final MultiLineStringSplitter splitterWithSplaces = MultiLineStringSplitter
            .createSplitterOnSpaceAndLine(";");
    private final MultiLineStringSplitter splitterGoWithSplaces = MultiLineStringSplitter
            .createSplitterOnSpaceAndLine("GO");

    @Test
    public void deployMultilinePreservationWithSpaces() {
        assertEquals(Lists.fixedSize.of(sql1 + "\n\n", sql2),
                this.splitterWithSplaces.valueOf(sql1 + "\n\n" + "\r\n;\n" + sql2));
    }

    @Test
    public void testGo() {
        assertEquals(Lists.fixedSize.of(sql1, sql2, sql3),
                this.splitterGoWithSplaces.valueOf(sql1 + "\nGO\n" + sql2 + "\nGO\n" + sql3));
    }

    @Test
    public void testMixedCaseGo() {
        assertEquals(Lists.fixedSize.of(sql1, sql2, sql3),
                this.splitterGoWithSplaces.valueOf(sql1 + "\ngO\n" + sql2 + "\n Go  \n" + sql3));
    }

    @Test
    public void testGoWithSpaces() {
        assertEquals(Lists.fixedSize.of(sql1 + "\n\n", "\n\n" + sql2, sql3),
                this.splitterGoWithSplaces.valueOf(sql1 + "\n\n\nGO  		    \n\n\n" + sql2 + "\n        GO\n" + sql3));
    }

    @Test
    public void deployTestRegularLineBreaks() {
        assertEquals(Lists.fixedSize.of(sql1, sql2, sql3),
                this.splitter.valueOf(sql1 + "\n;\n" + sql2 + "\n;\n" + sql3));
    }

    @Test
    public void deployTestLineBreaksWithCarriage() {
        assertEquals(Lists.fixedSize.of(sql1, sql2, sql3),
                this.splitter.valueOf(sql1 + "\r\n;\r\n" + sql2 + "\n;\n" + sql3));
    }

    @Test
    public void deployTestMixedLineBreaks() {
        assertEquals(Lists.fixedSize.of(sql1, sql2, sql3),
                this.splitter.valueOf(sql1 + "\n;\r\n" + sql2 + "\r\n;\r\n" + sql3));
        assertEquals(Lists.fixedSize.of(sql1, sql2, sql3),
                this.splitter.valueOf(sql1 + "\r\n;\n" + sql2 + "\r\n;\r\n" + sql3));
    }

    @Test
    public void deployMultilinePreservation() {
        assertEquals(Lists.fixedSize.of(sql1 + "\n\n", sql2), this.splitter.valueOf(sql1 + "\n\n" + "\r\n;\n" + sql2));
    }

    @Test
    public void testNoSplitIfLineDoesNotExactlyMatch() {
        // note the spaces in the first break
        assertEquals(Lists.fixedSize.of(sql1 + "\n ; \n" + sql2, sql2),
                this.splitter.valueOf(sql1 + "\n ; \n" + sql2 + "\n;\n" + sql2));
    }

    @Test
    public void expectFailedSplitOnBackwardsCarriageReturn() {
        assertFalse(Lists.fixedSize.of(sql1, sql2).equals(this.splitter.valueOf(sql1 + "\n;\n\r" + sql2)));
    }

    @Test
    public void testComments() {
        assertEquals(Lists.fixedSize.of("sql1\n/*\nGO\n*/ continued", "sql2"),
                this.splitterGoWithSplaces.valueOf("sql1\n/*\nGO\n*/ continued\nGO\nsql2"));
    }

    @Test
    public void testScenarioWithStrings() {
        MutableList<String> sqls = this.splitterGoWithSplaces.valueOf("sql1\n" +
                "GO\n" +
                "sql2 '1', '2'\n" +
                "GO\n" +
                "sql3 3, '4'\n" +
                "GO\n");

        assertEquals(Lists.fixedSize.of("sql1", "sql2 '1', '2'", "sql3 3, '4'", ""), sqls);
    }

    @Test
    public void testSingleGoAtEnd() {
        MutableList<String> sqls = this.splitterGoWithSplaces.valueOf("mysql\ngo");

        assertEquals(Lists.fixedSize.of("mysql"), sqls);
    }
}
