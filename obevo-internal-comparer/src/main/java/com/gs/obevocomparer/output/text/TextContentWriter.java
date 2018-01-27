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
package com.gs.obevocomparer.output.text;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.gs.obevocomparer.output.CatoContentMetadata;
import com.gs.obevocomparer.output.CatoContentRow;
import com.gs.obevocomparer.output.CatoContentWriter;
import com.gs.obevocomparer.util.IoUtil;

class TextContentWriter implements CatoContentWriter {

    private final Writer writer;

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public TextContentWriter(File file) throws IOException {
        this.writer = new OutputStreamWriter(IoUtil.getOutputStream(file));
    }

    public void writeRow(CatoContentRow row) throws IOException {
        boolean firstVal = true;
        for (int i = 0; i < row.getSize(); i++) {
            if (firstVal) {
                firstVal = false;
            } else {
                this.writer.write("\t");
            }

            this.writer.write(row.getValue(i) == null ? "" : row.getValue(i).toString());
        }

        this.writer.write(LINE_SEPARATOR);
    }

    @Override
    public void openContent(CatoContentMetadata contentMetadata) {
    }

    @Override
    public void closeContent() throws IOException {
        this.writer.write(LINE_SEPARATOR);
    }

    @Override
    public void closeWriter() throws IOException {
        this.writer.close();
    }
}
