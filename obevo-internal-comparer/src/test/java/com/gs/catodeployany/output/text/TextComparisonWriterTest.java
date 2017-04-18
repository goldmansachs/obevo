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
package com.gs.catodeployany.output.text;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.input.CatoDataSource;
import com.gs.catodeployany.util.CatoBaseUtil;
import com.gs.catodeployany.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class TextComparisonWriterTest {

    @Test
    public void testContentWriters() throws IOException {
        File file = File.createTempFile("Cato-test", "txt");
        file.deleteOnExit();
        TextComparisonWriter writer = new TextComparisonWriter(file);

        Assert.assertEquals(TextContentWriter.class, writer.getSummaryContentWriter().getClass());

        Assert.assertSame(writer.getSummaryContentWriter(), writer.getBreakContentWriter());
        Assert.assertSame(writer.getBreakContentWriter(), writer.getExcludedBreakContentWriter());

        Assert.assertNull(writer.getLeftDataSetContentWriter());
        Assert.assertNull(writer.getRightDataSetContentWriter());

        writer.close();
    }

    @Test
    public void testWriter() throws Exception {
        CatoDataSource dataSource1 = CatoBaseUtil.createDelimitedStreamDataSource("test1", new FileReader("src/test/resources/testdata1.txt"), TestUtil.ALL_FIELDS, ",");
        CatoDataSource dataSource2 = CatoBaseUtil.createDelimitedStreamDataSource("test2", new FileReader("src/test/resources/testdata2.txt"), TestUtil.ALL_FIELDS, ",");

        CatoComparison comparison = CatoBaseUtil.compare("Test Comp", dataSource1, dataSource2, TestUtil.KEY_FIELDS, TestUtil.EXCLUDE_FIELDS);

        TextComparisonWriter writer = new TextComparisonWriter("target/comparisons/text-test.txt");
        writer.writeComparison(comparison);
        writer.close();

        Assert.assertTrue(new File("target/comparisons/text-test.txt").canRead());
    }
}
