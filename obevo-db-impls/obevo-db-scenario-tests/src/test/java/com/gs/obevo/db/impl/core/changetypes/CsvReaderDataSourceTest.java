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
package com.gs.obevo.db.impl.core.changetypes;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.gs.obevocomparer.compare.simple.SimpleCatoProperties;
import com.gs.obevocomparer.data.CatoDataObject;
import com.gs.obevocomparer.spring.CatoSimpleJavaConfiguration;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.block.factory.StringFunctions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.internal.IteratorIterate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class CsvReaderDataSourceTest {
    private final int csvVersion;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { CsvStaticDataReader.CSV_V1 },
                { CsvStaticDataReader.CSV_V2 }
        });
    }

    public CsvReaderDataSourceTest(int csvVersion) {
        this.csvVersion = csvVersion;
    }

    /**
     * Adding this flag to facilitate tests for backwards-compatibility with V1.
     */
    private boolean isV2() {
        return csvVersion == CsvStaticDataReader.CSV_V2;
    }

    @Test
    public void testCsvDataSource() throws Exception {
        verifyCsv("field1,field2,field3\n" +
                        "a1,a2,a3\n" +
                        "b1,b2,b3",
                Lists.mutable.<String>of("field1"),
                Maps.mutable.of("field1", "a1", "field2", "a2", "field3", "a3"),
                Maps.mutable.of("field1", "b1", "field2", "b2", "field3", "b3")
        );
    }

    @Test
    public void testCsvDataSourceWithSpaces() throws Exception {
        verifyCsv("field1,   field2,    field3\n" +
                        "a1,a2,a3\n" +
                        "b1,b2,b3",
                Lists.mutable.<String>of("field1"),
                Maps.mutable.of("field1", "a1", isV2() ? "field2" : "   field2", "a2", isV2() ? "field3" : "    field3", "a3"),
                Maps.mutable.of("field1", "b1", isV2() ? "field2" : "   field2", "b2", isV2() ? "field3" : "    field3", "b3")
        );
    }

    @Test
    public void testCsvDataSourceWithQuotesAndSpaces() throws Exception {
        verifyCsv("field1,   \"field2\",    \"field3   \"\n" +
                        "\"  a1 \",a2,a3\n" +
                        "b1,   2,   \"  b3 \"",
                Lists.mutable.<String>of("field1"),
                Maps.mutable.of("field1", "  a1 ", "field2", "a2", "field3   ", "a3"),
                Maps.mutable.of("field1", "b1", "field2", isV2() ? "2" : "   2", "field3   ", "  b3 ")
        );
    }

    @Test
    public void testNullTokens() throws Exception {
        verifyCsv("field1,field2,field3\n" +
                        "a1,    null  ,\"null\"\n" +
                        "null,b2,\" null \"",
                Lists.mutable.<String>of("field1"),
                Maps.mutable.of("field1", "a1", "field2", isV2() ? null : "    null  ", "field3", isV2() ? null : "null"),
                Maps.mutable.of("field1", isV2() ? null : "null", "field2", "b2", "field3", " null ")
        );
    }

    @Test
    public void testCsvDataSourceMultiLines() throws Exception {
        verifyCsv("field1,   \"field2\",    \"field3   \"\n" +
                        "\"  a1 \",\"a\n2\",a3\n" +
                        "b1,   b2,   \"  b3 \"",
                Lists.mutable.<String>of("field1"),
                Maps.mutable.of("field1", "  a1 ", "field2", "a\n2", "field3   ", "a3"),
                Maps.mutable.of("field1", "b1", "field2", isV2() ? "b2" : "   b2", "field3   ", "  b3 ")
        );
    }

    @Test
    public void testCsvDataSourceWithEscapeCharsV2() throws Exception {
        assumeTrue(csvVersion == CsvStaticDataReader.CSV_V2);
        verifyCsv("field1,field2,field3\n" +
                        "a\\\\1,a\"2,\"a\\\"3\"\n" +
                        "b\\1,\"b\\2\",\"b\\3\"\n" +
                        "\"c\\\"1\",\"c\\2\",\"c\\3\""
                ,
                Lists.mutable.<String>of("field1"),
                Maps.mutable.of("field1", "a\\1", "field2", "a\"2", "field3", "a\"3"),
                Maps.mutable.of("field1", "b\\1", "field2", "b\\2", "field3", "b\\3"),
                Maps.mutable.of("field1", "c\"1", "field2", "c\\2", "field3", "c\\3")
        );
    }

    private void verifyCsv(String input, MutableList<String> keyFields, MutableMap<String, String>... rows) throws Exception {
        CsvReaderDataSource ds = new CsvReaderDataSource(csvVersion, "test", new StringReader(input), ',', StringFunctions.toLowerCase(), "null");
        ds.setCatoConfiguration(new CatoSimpleJavaConfiguration(new SimpleCatoProperties(keyFields)));
        ds.init();
        ds.open();
        final MutableList<Map<String, Object>> dataRows = IteratorIterate.collect(ds, new Function<CatoDataObject, Map<String, Object>>() {
            @Override
            public Map<String, Object> valueOf(CatoDataObject catoDataObject) {
                Map<String, Object> data = Maps.mutable.empty();
                for (String field : catoDataObject.getFields()) {
                    data.put(field, catoDataObject.getValue(field));
                }
                data.remove(CsvReaderDataSource.ROW_NUMBER_FIELD);

                return data;
            }
        }, Lists.mutable.<Map<String, Object>>empty());
        ds.close();

        assertEquals(rows.length, dataRows.size());
        for (int i = 0; i < dataRows.size(); i++) {
            assertEquals("Error on row " + i, rows[i], dataRows.get(i));
        }
    }
}
