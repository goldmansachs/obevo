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
package com.gs.obevocomparer.output.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.gs.obevocomparer.output.simple.SimpleContentMetadata;
import org.junit.Assert;
import org.junit.Test;

import static com.gs.obevocomparer.util.TestUtil.createContentRow;

public class TextContentWriterTest {

    @Test
    public void testWriter() throws IOException {
        File tempFile = File.createTempFile("Cato-test", "tmp");
        tempFile.deleteOnExit();

        TextContentWriter writer = new TextContentWriter(tempFile);

        writer.openContent(new SimpleContentMetadata("Test", 1, 0));
        writer.writeRow(createContentRow("Val1", "Text with spaces", "Val3"));
        writer.writeRow(createContentRow(4, 5.0, 6.5));
        writer.writeRow(createContentRow(null, "abc", null));
        writer.writeRow(createContentRow());
        writer.writeRow(createContentRow(new Object() {
            public String toString() {
                return "Test Obj";
            }
        }));
        writer.closeContent();

        writer.openContent(new SimpleContentMetadata("Test 2", 1, 0));
        writer.writeRow(createContentRow("Result2", "More Values"));
        writer.writeRow(createContentRow("Result2", "More Values Again"));
        writer.closeContent();

        writer.closeWriter();

        this.assertFile(tempFile,
                "Val1\tText with spaces\tVal3",
                "4\t5.0\t6.5",
                "\tabc\t",
                "",
                "Test Obj",
                "",
                "Result2\tMore Values",
                "Result2\tMore Values Again",
                "");
    }

    private void assertFile(File file, String... rows) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        int index = 0;

        while ((line = reader.readLine()) != null) {
            Assert.assertEquals(rows[index++], line);
        }

        Assert.assertEquals("File length does not match expected", index, rows.length);
    }
}
