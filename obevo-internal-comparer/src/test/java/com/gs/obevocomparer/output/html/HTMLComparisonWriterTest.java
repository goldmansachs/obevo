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
package com.gs.obevocomparer.output.html;

import java.io.File;
import java.io.FileReader;

import com.gs.obevocomparer.compare.CatoComparison;
import com.gs.obevocomparer.input.CatoDataSource;
import com.gs.obevocomparer.util.CatoBaseUtil;
import com.gs.obevocomparer.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;

public class HTMLComparisonWriterTest {

    @Test
    public void testWriter() throws Exception {
        CatoDataSource dataSource1 = CatoBaseUtil.createDelimitedStreamDataSource(
                "test1", new FileReader("src/test/resources/testdata1.txt"),
                TestUtil.ALL_FIELDS, ",");
        CatoDataSource dataSource2 = CatoBaseUtil.createDelimitedStreamDataSource(
                "test2", new FileReader("src/test/resources/testdata2.txt"),
                TestUtil.ALL_FIELDS, ",");

        CatoComparison comparison = CatoBaseUtil.compare("HTML Comp", dataSource1,
                dataSource2, TestUtil.KEY_FIELDS, TestUtil.EXCLUDE_FIELDS);

        HTMLComparisonWriter writer = new HTMLComparisonWriter(
                "target/comparisons/html-test-content.html",
                "target/comparisons/html-test-summary.html");
        writer.writeComparison(comparison);
        writer.close();

        Assert.assertTrue(new File("target/comparisons/html-test-content.html")
                .canRead());
        org.junit.Assert.assertTrue(new File("target/comparisons/html-test-summary.html")
                .canRead());
    }
}
