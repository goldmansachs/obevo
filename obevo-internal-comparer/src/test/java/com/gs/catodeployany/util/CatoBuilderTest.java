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
package com.gs.catodeployany.util;

import java.io.FileReader;

import com.gs.catodeployany.compare.CatoComparison;
import com.gs.catodeployany.input.CatoDataSource;
import org.junit.Assert;
import org.junit.Test;

public class CatoBuilderTest {

    @Test
    public void testBasic() throws Exception {

        CatoDataSource leftSource =
                CatoBaseUtil.createDelimitedStreamDataSource("QA Data", new FileReader("src/test/resources/testdata1.txt"),
                        TestUtil.ALL_FIELDS, ",");
        CatoDataSource rightSource =
                CatoBaseUtil.createDelimitedStreamDataSource("Prod Data",
                        new FileReader("src/test/resources/testdata2.txt"), TestUtil.ALL_FIELDS, ",");

        CatoComparison comparison =
                CatoBuilder.newInstance().withSources(leftSource, rightSource)
                        .withFields(TestUtil.KEY_FIELDS, TestUtil.EXCLUDE_FIELDS).compare("Comparison");

        Assert.assertNotNull(comparison);
    }
}
